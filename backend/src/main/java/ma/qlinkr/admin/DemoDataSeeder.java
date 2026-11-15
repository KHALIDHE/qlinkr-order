package ma.qlinkr.admin;

import java.util.Map;
import ma.qlinkr.business.Business;
import ma.qlinkr.business.BusinessRepository;
import ma.qlinkr.business.BusinessService;
import ma.qlinkr.business.BusinessType;
import ma.qlinkr.catalog.Category;
import ma.qlinkr.catalog.CategoryRepository;
import ma.qlinkr.catalog.Item;
import ma.qlinkr.catalog.ItemRepository;
import ma.qlinkr.catalog.ItemText;
import ma.qlinkr.scan.ScanPoint;
import ma.qlinkr.scan.ScanPointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates one demo café (owner, staff, catalogue, a fixed QR code) so a live demo has something to scan.
 * Only runs when qlinkr.demo.enabled=true and a demo password is set. Does nothing if it already exists.
 */
@Component
@ConditionalOnProperty(name = "qlinkr.demo.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    public static final String SCAN_CODE = "demo2026cafe";
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final BusinessService service;
    private final BusinessRepository businesses;
    private final CategoryRepository categories;
    private final ItemRepository items;
    private final ScanPointRepository scanPoints;
    private final String password;

    public DemoDataSeeder(BusinessService service, BusinessRepository businesses, CategoryRepository categories,
                          ItemRepository items, ScanPointRepository scanPoints,
                          @Value("${qlinkr.demo.password:}") String password) {
        this.service = service;
        this.businesses = businesses;
        this.categories = categories;
        this.items = items;
        this.scanPoints = scanPoints;
        this.password = password;
    }

    private static Map<String, ItemText> texts(String fr, String en, String ar) {
        return Map.of("fr", new ItemText(fr, null), "en", new ItemText(en, null), "ar", new ItemText(ar, null));
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password.length() < 10 || businesses.existsBySlug("demo-cafe")) {
            return;
        }
        Business b = service.createWithOwner("Café Demo", "demo-cafe", BusinessType.RESTAURANT, "fr",
            "owner@demo.qlinkr", password);
        service.addStaff(b.getId(), "staff@demo.qlinkr", password);

        Category drinks = categories.save(new Category(b.getId(), 1,
            Map.of("fr", "Boissons", "en", "Drinks", "ar", "مشروبات")));
        Category bakery = categories.save(new Category(b.getId(), 2,
            Map.of("fr", "Viennoiseries", "en", "Bakery", "ar", "معجنات")));

        items.save(new Item(b.getId(), drinks.getId(), 1500, null, null, 1, texts("Thé à la menthe", "Mint tea", "أتاي بالنعناع")));
        items.save(new Item(b.getId(), drinks.getId(), 1200, null, null, 2, texts("Café", "Coffee", "قهوة")));
        items.save(new Item(b.getId(), drinks.getId(), 1800, null, null, 3, texts("Jus d'orange", "Orange juice", "عصير البرتقال")));
        // Limited stock on purpose: a good item to show the "last item" protection.
        items.save(new Item(b.getId(), bakery.getId(), 500, null, 10, 1, texts("Croissant", "Croissant", "كرواسون")));

        scanPoints.save(new ScanPoint(b.getId(), SCAN_CODE, "Table 4", ScanPoint.Kind.TABLE));
        log.info("Demo café created. Scan code: {}", SCAN_CODE);
    }
}
