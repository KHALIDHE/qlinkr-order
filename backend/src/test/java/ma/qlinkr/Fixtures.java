package ma.qlinkr;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

/** Builds a business with an owner, a staff member, a category, items and a scan point through the real API. */
final class Fixtures {

    private Fixtures() {
    }

    record Shop(long businessId, String slug, String ownerToken, String staffToken, long categoryId, String scanCode,
                long scanPointId) {}

    static Shop shop(IntegrationTest t, String prefix, String defaultLang) {
        String admin = t.adminToken();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String slug = prefix + "-" + suffix;
        String password = "password-" + suffix;
        IntegrationTest.Response created = t.post("/api/admin/businesses", admin, Map.of(
            "name", "Shop " + slug, "slug", slug, "type", "RESTAURANT", "defaultLang", defaultLang,
            "ownerEmail", "owner-" + slug + "@qlinkr.test", "ownerPassword", password));
        assertThat(created.status()).as(created.raw()).isEqualTo(201);
        long businessId = ((Number) created.map().get("id")).longValue();
        String owner = t.login("owner-" + slug + "@qlinkr.test", password);
        t.post("/api/business/staff", owner, Map.of("email", "staff-" + slug + "@qlinkr.test", "password", password));
        String staff = t.login("staff-" + slug + "@qlinkr.test", password);

        IntegrationTest.Response category = t.post("/api/catalog/categories", owner, Map.of(
            "names", Map.of("fr", "Boissons", "en", "Drinks", "ar", "مشروبات"), "position", 1));
        assertThat(category.status()).as(category.raw()).isEqualTo(201);

        IntegrationTest.Response point = t.post("/api/scan-points", owner, Map.of("label", "Table 4", "kind", "TABLE"));
        assertThat(point.status()).as(point.raw()).isEqualTo(201);

        return new Shop(businessId, slug, owner, staff, ((Number) category.map().get("id")).longValue(),
            (String) point.map().get("code"), ((Number) point.map().get("id")).longValue());
    }

    static long item(IntegrationTest t, Shop shop, Map<String, Object> texts, long priceCents, Integer stock) {
        Map<String, Object> body = new java.util.HashMap<>(Map.of(
            "categoryId", shop.categoryId(), "texts", texts, "priceCents", priceCents));
        if (stock != null) {
            body.put("stock", stock);
        }
        IntegrationTest.Response r = t.post("/api/catalog/items", shop.ownerToken(), body);
        assertThat(r.status()).as(r.raw()).isEqualTo(201);
        return ((Number) r.map().get("id")).longValue();
    }

    static Map<String, Object> threeLanguages(String fr, String en, String ar) {
        return Map.of("fr", Map.of("name", fr), "en", Map.of("name", en), "ar", Map.of("name", ar));
    }
}
