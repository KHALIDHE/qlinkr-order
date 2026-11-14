package ma.qlinkr;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderingTest extends IntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    private Fixtures.Shop shop;
    private Fixtures.Shop other;
    private long coffee;

    @BeforeAll
    void setUp() {
        shop = Fixtures.shop(this, "bakery", "fr");
        other = Fixtures.shop(this, "rival", "fr");
        coffee = Fixtures.item(this, shop, Fixtures.threeLanguages("Café", "Coffee", "قهوة"), 1200, null);
    }

    private Response order(String code, String key, long itemId, int quantity) {
        return orderWith(code, key, List.of(Map.of("itemId", itemId, "quantity", quantity)));
    }

    private Response orderWith(String code, String key, List<Map<String, Object>> lines) {
        return RestClient.create("http://localhost:" + port).post().uri("/api/public/scan/" + code + "/orders")
            .contentType(MediaType.APPLICATION_JSON).header("Idempotency-Key", key)
            .body(Map.of("lines", lines))
            .exchange((req, res) -> {
                String text = new String(res.getBody().readAllBytes());
                Object parsed = text.isBlank() ? null
                    : tools.jackson.databind.json.JsonMapper.builder().build().readValue(text, Object.class);
                return new Response(res.getStatusCode().value(), parsed, text);
            });
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }

    private int stockOf(long itemId) {
        return jdbc.queryForObject("SELECT stock FROM item WHERE id = ?", Integer.class, itemId);
    }

    @Test
    void scanToOrderToStaffScreen() {
        Response placed = order(shop.scanCode(), key(), coffee, 2);

        assertThat(placed.status()).as(placed.raw()).isEqualTo(201);
        assertThat(placed.map()).containsEntry("status", "NEW").containsEntry("totalCents", 2400);
        String token = (String) placed.map().get("token");

        List<Map<String, Object>> open = get("/api/orders", shop.staffToken()).list();
        assertThat(open).anySatisfy(o -> assertThat(o).containsEntry("scanPointLabel", "Table 4"));
        assertThat(get("/api/public/orders/" + token, null).map()).containsEntry("status", "NEW");
    }

    @Test
    void lastItemRace_hundredCustomersTenInStock_exactlyTenWin() throws Exception {
        long croissant = Fixtures.item(this, shop, Fixtures.threeLanguages("Croissant", "Croissant", "كرواسون"), 500, 10);
        int customers = 100;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(32);
        List<Future<Integer>> results = new ArrayList<>();
        try {
            for (int i = 0; i < customers; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return order(shop.scanCode(), key(), croissant, 1).status();
                }));
            }
            start.countDown();
            int created = 0;
            int soldOut = 0;
            for (Future<Integer> f : results) {
                int status = f.get();
                if (status == 201) created++;
                else if (status == 409) soldOut++;
            }

            assertThat(created).isEqualTo(10);
            assertThat(soldOut).isEqualTo(90);
            assertThat(stockOf(croissant)).isZero();
            assertThat(jdbc.queryForObject("SELECT COALESCE(SUM(quantity), 0) FROM order_line WHERE item_id = ?",
                Integer.class, croissant)).isEqualTo(10);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void doubleTap_sameKeyFiveTimes_oneOrder() throws Exception {
        String sameKey = key();
        List<Integer> statuses = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            statuses.add(order(shop.scanCode(), sameKey, coffee, 1).status());
        }

        assertThat(statuses).containsExactly(201, 200, 200, 200, 200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM customer_order WHERE idempotency_key = ?",
            Integer.class, sameKey)).isEqualTo(1);
    }

    @Test
    void doubleTap_sameKeyInParallel_oneOrder() throws Exception {
        String sameKey = key();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(5);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return order(shop.scanCode(), sameKey, coffee, 1).status();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> f : futures) {
                statuses.add(f.get());
            }
            assertThat(statuses).containsOnly(200, 201).contains(201);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM customer_order WHERE idempotency_key = ?",
            Integer.class, sameKey)).isEqualTo(1);
    }

    @Test
    void lifecycleFollowsTheStateMachine() {
        long id = orderId(order(shop.scanCode(), key(), coffee, 1));

        assertThat(status(id, "READY", shop.staffToken())).as("NEW cannot jump to READY").isEqualTo(409);
        assertThat(status(id, "ACCEPTED", shop.staffToken())).isEqualTo(200);
        assertThat(status(id, "PREPARING", shop.staffToken())).isEqualTo(200);
        assertThat(status(id, "READY", shop.staffToken())).isEqualTo(200);
        assertThat(status(id, "CANCELLED", shop.staffToken())).as("too late to cancel").isEqualTo(409);
        assertThat(status(id, "COMPLETED", shop.staffToken())).isEqualTo(200);
        assertThat(status(id, "ACCEPTED", shop.staffToken())).as("COMPLETED is final").isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_event WHERE order_id = ?", Integer.class, id))
            .isEqualTo(5);
    }

    @Test
    void cancellingGivesTheStockBack() {
        long cake = Fixtures.item(this, shop, Fixtures.threeLanguages("Gâteau", "Cake", "كعكة"), 2500, 3);
        long id = orderId(order(shop.scanCode(), key(), cake, 2));
        assertThat(stockOf(cake)).isEqualTo(1);

        assertThat(status(id, "CANCELLED", shop.staffToken())).isEqualTo(200);

        assertThat(stockOf(cake)).isEqualTo(3);
    }

    @Test
    void orderingMoreThanTheStockIsRefusedWithTheItem() {
        long tart = Fixtures.item(this, shop, Fixtures.threeLanguages("Tarte", "Tart", "تارت"), 3000, 1);

        Response r = order(shop.scanCode(), key(), tart, 2);

        assertThat(r.status()).isEqualTo(409);
        assertThat(r.raw()).contains("unavailableItemIds").contains(String.valueOf(tart));
        assertThat(stockOf(tart)).isEqualTo(1);
    }

    @Test
    void anotherBusinessCannotSeeOrMoveTheOrder() {
        long id = orderId(order(shop.scanCode(), key(), coffee, 1));

        assertThat(status(id, "ACCEPTED", other.staffToken())).isEqualTo(404);
        assertThat(get("/api/orders", other.staffToken()).list()).noneMatch(o -> ((Number) o.get("id")).longValue() == id);
        // An item of another business cannot be ordered through this QR code.
        long foreign = Fixtures.item(this, other, Fixtures.threeLanguages("X", "X", "X"), 100, null);
        assertThat(order(shop.scanCode(), key(), foreign, 1).status()).isEqualTo(409);
    }

    @Test
    void requestsWithoutAValidIdempotencyKeyAreRefused() {
        assertThat(order(shop.scanCode(), "short", coffee, 1).status()).isEqualTo(400);
        assertThat(order(shop.scanCode(), key(), coffee, 0).status()).isEqualTo(400);
    }

    private long orderId(Response placed) {
        assertThat(placed.status()).as(placed.raw()).isEqualTo(201);
        String token = (String) placed.map().get("token");
        return jdbc.queryForObject("SELECT id FROM customer_order WHERE public_token = ?", Long.class, token);
    }

    private int status(long orderId, String to, String token) {
        return call(HttpMethod.POST, "/api/orders/" + orderId + "/status", token, Map.of("to", to)).status();
    }
}
