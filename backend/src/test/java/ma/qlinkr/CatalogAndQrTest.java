package ma.qlinkr;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CatalogAndQrTest extends IntegrationTest {

    private Fixtures.Shop shop;
    private Fixtures.Shop other;
    private long tea;

    @BeforeAll
    void setUp() {
        shop = Fixtures.shop(this, "cafe", "fr");
        other = Fixtures.shop(this, "other", "en");
        tea = Fixtures.item(this, shop, Fixtures.threeLanguages("Thé à la menthe", "Mint tea", "أتاي بالنعناع"), 1500, null);
        // Only French: other languages fall back to it.
        Fixtures.item(this, shop, Map.of("fr", Map.of("name", "Msemen", "description", "Crêpe feuilletée")), 800, null);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> itemsOf(Response scan) {
        List<Map<String, Object>> categories = (List<Map<String, Object>>) scan.map().get("categories");
        return (List<Map<String, Object>>) categories.get(0).get("items");
    }

    @Test
    void scanShowsTheCatalogueInEachLanguage() {
        Response fr = get("/api/public/scan/" + shop.scanCode() + "?lang=fr", null);
        Response en = get("/api/public/scan/" + shop.scanCode() + "?lang=en", null);
        Response ar = get("/api/public/scan/" + shop.scanCode() + "?lang=ar", null);

        assertThat(fr.status()).isEqualTo(200);
        assertThat(itemsOf(fr)).extracting(i -> i.get("name")).contains("Thé à la menthe");
        assertThat(itemsOf(en)).extracting(i -> i.get("name")).contains("Mint tea");
        assertThat(itemsOf(ar)).extracting(i -> i.get("name")).contains("أتاي بالنعناع");
        assertThat(ar.map()).containsEntry("dir", "rtl");
        assertThat(fr.map()).containsEntry("dir", "ltr");
        assertThat((Map<String, Object>) ar.map().get("labels")).containsEntry("catalog", "القائمة");
    }

    @Test
    void missingTranslationFallsBackToTheBusinessLanguage() {
        Response en = get("/api/public/scan/" + shop.scanCode() + "?lang=en", null);

        assertThat(itemsOf(en)).extracting(i -> i.get("name")).contains("Msemen");
    }

    @Test
    void unknownLanguageUsesTheBusinessDefault() {
        Response scan = get("/api/public/scan/" + shop.scanCode() + "?lang=de", null);

        assertThat(scan.map()).containsEntry("lang", "fr");
    }

    @Test
    void unknownOrDisabledCodesAreNotFound() {
        assertThat(get("/api/public/scan/nosuchcode", null).status()).isEqualTo(404);

        Response p = post("/api/scan-points", other.ownerToken(), Map.of("label", "Door", "kind", "DOOR"));
        long id = ((Number) p.map().get("id")).longValue();
        call(HttpMethod.PATCH, "/api/scan-points/" + id, other.ownerToken(), Map.of("active", false));
        assertThat(get("/api/public/scan/" + p.map().get("code"), null).status()).isEqualTo(404);
    }

    @Test
    void generatedQrDecodesToTheScanUrl() throws Exception {
        byte[] png = RestClient.create("http://localhost:" + port).get()
            .uri("/api/scan-points/" + shop.scanPointId() + "/qr.png?size=400")
            .header("Authorization", "Bearer " + shop.ownerToken())
            .retrieve().body(byte[].class);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        String decoded = new MultiFormatReader().decode(
            new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)))).getText();

        assertThat(decoded).isEqualTo("http://localhost:5173/s/" + shop.scanCode());
    }

    @Test
    void svgAndPrintSheetAreServed() {
        Response svg = get("/api/scan-points/" + shop.scanPointId() + "/qr.svg", shop.ownerToken());
        Response sheet = get("/api/scan-points/print-sheet", shop.ownerToken());

        assertThat(svg.status()).isEqualTo(200);
        assertThat(sheet.raw()).contains("<svg").contains("Table 4");
    }

    @Test
    void ownersCannotTouchAnotherBusinessCatalogue() {
        assertThat(call(HttpMethod.PATCH, "/api/catalog/items/" + tea, other.ownerToken(),
            Map.of("priceCents", 1)).status()).isEqualTo(404);
        assertThat(call(HttpMethod.DELETE, "/api/catalog/items/" + tea, other.ownerToken(), null).status()).isEqualTo(404);
        assertThat(get("/api/scan-points/" + shop.scanPointId() + "/qr.svg", other.ownerToken()).status()).isEqualTo(404);
        assertThat(post("/api/catalog/items", other.ownerToken(), Map.of("categoryId", shop.categoryId(),
            "texts", Map.of("fr", Map.of("name", "x")), "priceCents", 1)).status()).isEqualTo(404);
    }

    @Test
    void staffCannotEditTheCatalogue() {
        assertThat(post("/api/catalog/categories", shop.staffToken(), Map.of("names", Map.of("fr", "x"))).status())
            .isEqualTo(403);
    }

    @Test
    void invalidCatalogueInputIsRefused() {
        assertThat(post("/api/catalog/categories", shop.ownerToken(), Map.of("names", Map.of("de", "Getränke")))
            .status()).isEqualTo(400);
        assertThat(post("/api/catalog/items", shop.ownerToken(), Map.of("categoryId", shop.categoryId(),
            "texts", Map.of("fr", Map.of("name", "x")), "priceCents", -5)).status()).isEqualTo(400);
    }
}
