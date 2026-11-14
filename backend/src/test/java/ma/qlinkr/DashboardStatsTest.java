package ma.qlinkr;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

/**
 * Seeds orders with known dates, hours, items and preparation times, then
 * checks every dashboard number against values worked out by hand.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DashboardStatsTest extends IntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    private Fixtures.Shop shop;
    private long coffee;
    private long cake;

    @BeforeAll
    void seed() {
        shop = Fixtures.shop(this, "stats", "fr");
        coffee = Fixtures.item(this, shop, Fixtures.threeLanguages("Café", "Coffee", "قهوة"), 1000, null);
        cake = Fixtures.item(this, shop, Fixtures.threeLanguages("Gâteau", "Cake", "كعكة"), 3000, null);

        // day          hour  lines                status      prep (s)
        // 2026-01-10   08    coffee x2            COMPLETED   300
        // 2026-01-10   08    coffee x1, cake x1   COMPLETED   600
        // 2026-01-10   13    cake x1              CANCELLED   (never counts)
        // 2026-01-11   13    coffee x3            READY       900
        place("2026-01-10T08:05:00Z", "COMPLETED", 300, Map.of(coffee, 2));
        place("2026-01-10T08:40:00Z", "COMPLETED", 600, Map.of(coffee, 1, cake, 1));
        place("2026-01-10T13:10:00Z", "CANCELLED", null, Map.of(cake, 1));
        place("2026-01-11T13:30:00Z", "READY", 900, Map.of(coffee, 3));
    }

    private void place(String createdAt, String status, Integer prepSeconds, Map<Long, Integer> lines) {
        List<Map<String, Object>> body = new ArrayList<>();
        lines.forEach((item, qty) -> body.add(Map.of("itemId", item, "quantity", qty)));
        Map<?, ?> placed = RestClient.create("http://localhost:" + port).post()
            .uri("/api/public/scan/" + shop.scanCode() + "/orders")
            .contentType(MediaType.APPLICATION_JSON).header("Idempotency-Key", UUID.randomUUID().toString())
            .body(Map.of("lines", body)).retrieve().body(Map.class);
        OffsetDateTime created = OffsetDateTime.parse(createdAt);
        OffsetDateTime accepted = created.plusMinutes(1);
        jdbc.update("""
            UPDATE customer_order SET created_at = ?, status = ?, accepted_at = ?, ready_at = ?
            WHERE public_token = ?""",
            created, status, prepSeconds == null ? null : accepted,
            prepSeconds == null ? null : accepted.plusSeconds(prepSeconds), placed.get("token"));
    }

    private List<Map<String, Object>> stat(String name, String extra) {
        return get("/api/owner/stats/" + name + "?from=2026-01-10&to=2026-01-11" + extra, shop.ownerToken()).list();
    }

    private static long n(Object o) {
        return ((Number) o).longValue();
    }

    @Test
    void salesPerDayIgnoresCancelledOrders() {
        List<Map<String, Object>> days = stat("sales-per-day", "");

        assertThat(days).hasSize(2);
        assertThat(n(days.get(0).get("ORDERS"))).isEqualTo(2);
        assertThat(n(days.get(0).get("REVENUE_CENTS"))).isEqualTo(2000 + 1000 + 3000);
        assertThat(n(days.get(1).get("ORDERS"))).isEqualTo(1);
        assertThat(n(days.get(1).get("REVENUE_CENTS"))).isEqualTo(3000);
        assertThat(n(days.get(1).get("REVENUE_7D_AVG_CENTS"))).isEqualTo((6000 + 3000) / 2);
    }

    @Test
    void bestItemsAreRankedByQuantity() {
        List<Map<String, Object>> items = stat("best-items", "");

        assertThat(items.get(0)).containsEntry("ITEM_NAME", "Café");
        assertThat(n(items.get(0).get("QUANTITY"))).isEqualTo(6);
        assertThat(n(items.get(0).get("RANK_BY_QUANTITY"))).isEqualTo(1);
        assertThat(n(items.get(1).get("QUANTITY"))).as("the cancelled cake does not count").isEqualTo(1);
    }

    @Test
    void busyHoursShowShares() {
        List<Map<String, Object>> hours = stat("busy-hours", "");

        assertThat(hours).extracting(h -> n(h.get("HOUR"))).containsExactly(8L, 13L);
        assertThat(n(hours.get(0).get("ORDERS"))).isEqualTo(2);
        assertThat(((Number) hours.get(0).get("SHARE_PERCENT")).doubleValue()).isEqualTo(66.7);
    }

    @Test
    void preparationTimeAverageMedianAndP90() {
        Map<String, Object> prep = get("/api/owner/stats/preparation-time?from=2026-01-10&to=2026-01-11",
            shop.ownerToken()).map();

        assertThat(n(prep.get("ORDERS"))).isEqualTo(3);
        assertThat(n(prep.get("AVG_SECONDS"))).isEqualTo(600);
        assertThat(n(prep.get("MEDIAN_SECONDS"))).isEqualTo(600);
        assertThat(n(prep.get("P90_SECONDS"))).isEqualTo(840); // PERCENTILE_CONT: 600 + 0.8 * (900 - 600)
    }

    @Test
    void staffCannotSeeTheDashboard() {
        assertThat(get("/api/owner/stats/sales-per-day?from=2026-01-10&to=2026-01-11", shop.staffToken()).status())
            .isEqualTo(403);
    }

    /**
     * Writes the execution plan of the sales query with the dashboard index
     * hidden and then visible (Oracle invisible indexes), for docs/sql-notes.md.
     */
    @Test
    void recordsQueryPlansBeforeAndAfterTheIndex() throws IOException {
        String sql = "SELECT COUNT(*), SUM(total_cents) FROM customer_order WHERE business_id = :b "
            + "AND created_at >= TIMESTAMP '2026-01-10 00:00:00 UTC' AND created_at < TIMESTAMP '2026-01-12 00:00:00 UTC' "
            + "AND status NOT IN ('CANCELLED', 'REJECTED')";
        StringBuilder out = new StringBuilder();
        jdbc.execute("ALTER INDEX ix_order_business_created INVISIBLE");
        try {
            out.append("-- index INVISIBLE\n").append(plan(sql));
        } finally {
            jdbc.execute("ALTER INDEX ix_order_business_created VISIBLE");
        }
        String after = plan(sql);
        out.append("\n-- index VISIBLE\n").append(after);

        Path file = Path.of("target", "sql-plans.txt");
        Files.writeString(file, out.toString());
        assertThat(after).contains("IX_ORDER_BUSINESS_CREATED");
    }

    private String plan(String sql) {
        jdbc.execute("EXPLAIN PLAN FOR " + sql.replace(":b", String.valueOf(shop.businessId())));
        return String.join("\n", jdbc.queryForList("SELECT plan_table_output FROM TABLE(DBMS_XPLAN.DISPLAY())", String.class));
    }
}
