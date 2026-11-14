package ma.qlinkr;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CatalogCacheTest extends IntegrationTest {

    @Autowired
    StringRedisTemplate redis;

    private Fixtures.Shop shop;
    private long juice;

    @BeforeAll
    void setUp() {
        shop = Fixtures.shop(this, "cache", "fr");
        juice = Fixtures.item(this, shop, Fixtures.threeLanguages("Jus d'orange", "Orange juice", "عصير البرتقال"), 1800, null);
    }

    @SuppressWarnings("unchecked")
    private long priceSeenByCustomers() {
        Map<String, Object> scan = get("/api/public/scan/" + shop.scanCode() + "?lang=en", null).map();
        List<Map<String, Object>> cats = (List<Map<String, Object>>) scan.get("categories");
        List<Map<String, Object>> items = (List<Map<String, Object>>) cats.get(0).get("items");
        return ((Number) items.get(0).get("priceCents")).longValue();
    }

    @Test
    void scanFillsTheCacheAndAnEditIsVisibleImmediately() {
        assertThat(priceSeenByCustomers()).isEqualTo(1800);
        assertThat(redis.hasKey("catalog:" + shop.businessId() + ":en")).isTrue();

        call(HttpMethod.PATCH, "/api/catalog/items/" + juice, shop.ownerToken(), Map.of("priceCents", 2000));

        assertThat(redis.hasKey("catalog:" + shop.businessId() + ":en")).as("edit evicts").isFalse();
        assertThat(priceSeenByCustomers()).isEqualTo(2000);
    }

    /**
     * Measures the scan endpoint with and without the cache on this machine.
     * The numbers are written to target/cache-measurement.txt; they depend on
     * the machine and are only reported as measured there.
     */
    @Test
    void measureScanLatencyWithAndWithoutCache() throws IOException {
        for (int i = 0; i < 20; i++) {
            priceSeenByCustomers(); // warm-up
        }
        long[] cached = new long[200];
        for (int i = 0; i < cached.length; i++) {
            long t = System.nanoTime();
            priceSeenByCustomers();
            cached[i] = System.nanoTime() - t;
        }
        long[] uncached = new long[200];
        for (int i = 0; i < uncached.length; i++) {
            redis.delete("catalog:" + shop.businessId() + ":en");
            long t = System.nanoTime();
            priceSeenByCustomers();
            uncached[i] = System.nanoTime() - t;
        }
        String report = "scan endpoint, 200 requests each, in-process HTTP client, Testcontainers Oracle + Redis\n"
            + "with cache:    p50 " + ms(cached, 50) + " ms, p95 " + ms(cached, 95) + " ms\n"
            + "without cache: p50 " + ms(uncached, 50) + " ms, p95 " + ms(uncached, 95) + " ms\n";
        Files.writeString(Path.of("target", "cache-measurement.txt"), report);
        assertThat(report).contains("with cache");
    }

    private static String ms(long[] samples, int percentile) {
        long[] sorted = samples.clone();
        Arrays.sort(sorted);
        int index = (int) Math.ceil(percentile / 100.0 * sorted.length) - 1;
        return String.format("%.2f", sorted[Math.max(0, index)] / 1_000_000.0);
    }
}
