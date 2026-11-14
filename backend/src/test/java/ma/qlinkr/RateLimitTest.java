package ma.qlinkr;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

/** Small limits and a 2-second window, so the test can see the limit trigger and then reset. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
    "qlinkr.rate-limit.window=PT2S",
    "qlinkr.rate-limit.browse-per-ip=5",
    "qlinkr.rate-limit.orders-per-ip=3",
    "qlinkr.rate-limit.orders-per-scan-point=100",
})
class RateLimitTest extends IntegrationTest {

    private Fixtures.Shop shop;
    private long water;

    @BeforeAll
    void setUp() {
        shop = Fixtures.shop(this, "limits", "fr");
        water = Fixtures.item(this, shop, Fixtures.threeLanguages("Eau", "Water", "ماء"), 500, null);
    }

    private int order() {
        return RestClient.create("http://localhost:" + port).post().uri("/api/public/scan/" + shop.scanCode() + "/orders")
            .contentType(MediaType.APPLICATION_JSON).header("Idempotency-Key", UUID.randomUUID().toString())
            .body(Map.of("lines", List.of(Map.of("itemId", water, "quantity", 1))))
            .exchange((req, res) -> res.getStatusCode().value());
    }

    private static void waitForNextWindow() throws InterruptedException {
        long now = System.currentTimeMillis();
        Thread.sleep(2000 - now % 2000 + 50);
    }

    @Test
    void orderLimitTriggersAndResets() throws InterruptedException {
        waitForNextWindow();
        List<Integer> statuses = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            statuses.add(order());
        }
        assertThat(statuses).containsExactly(201, 201, 201, 429, 429);

        waitForNextWindow();
        assertThat(order()).isEqualTo(201);
    }

    @Test
    void browsingLimitTriggers() throws InterruptedException {
        waitForNextWindow();
        List<Integer> statuses = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            statuses.add(get("/api/public/scan/" + shop.scanCode(), null).status());
        }
        assertThat(statuses.subList(0, 5)).containsOnly(200);
        assertThat(statuses.subList(5, 7)).containsOnly(429);
    }

    @Test
    void privateEndpointsAreNotAffected() throws InterruptedException {
        waitForNextWindow();
        for (int i = 0; i < 10; i++) {
            assertThat(get("/api/business", shop.ownerToken()).status()).isEqualTo(200);
        }
    }
}
