package ma.qlinkr.publicapi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ma.qlinkr.business.Business;
import ma.qlinkr.cache.CatalogCache;
import ma.qlinkr.business.BusinessRepository;
import ma.qlinkr.business.BusinessType;
import ma.qlinkr.catalog.CategoryRepository;
import ma.qlinkr.catalog.ItemRepository;
import ma.qlinkr.catalog.Category;
import ma.qlinkr.catalog.Item;
import ma.qlinkr.catalog.ItemText;
import ma.qlinkr.catalog.Lang;
import ma.qlinkr.common.NotFoundException;
import ma.qlinkr.scan.ScanPoint;
import ma.qlinkr.scan.ScanPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds what a customer sees after scanning: business, labels, catalogue, in one language. */
@Service
public class PublicCatalogService {

    /** Display words per business type and language. The code itself only knows neutral names. */
    static final Map<BusinessType, Map<String, Labels>> LABELS = Map.of(
        BusinessType.RESTAURANT, Map.of(
            "fr", new Labels("Menu", "Table", "Commander"),
            "en", new Labels("Menu", "Table", "Order"),
            "ar", new Labels("القائمة", "الطاولة", "اطلب")),
        BusinessType.SHOP, Map.of(
            "fr", new Labels("Catalogue", "Comptoir", "Réserver"),
            "en", new Labels("Catalogue", "Counter", "Reserve"),
            "ar", new Labels("الكتالوج", "الكونتوار", "احجز")),
        BusinessType.SERVICE, Map.of(
            "fr", new Labels("Services", "Accueil", "Demander"),
            "en", new Labels("Services", "Reception", "Request"),
            "ar", new Labels("الخدمات", "الاستقبال", "اطلب")));

    private final ScanPointRepository points;
    private final BusinessRepository businesses;
    private final CategoryRepository categories;
    private final ItemRepository items;
    private final CatalogCache cache;

    public PublicCatalogService(ScanPointRepository points, BusinessRepository businesses,
                                CategoryRepository categories, ItemRepository items, CatalogCache cache) {
        this.points = points;
        this.businesses = businesses;
        this.categories = categories;
        this.items = items;
        this.cache = cache;
    }

    @Transactional(readOnly = true)
    public ScanView scan(String code, String requestedLang) {
        ScanPoint point = points.findByCodeAndActiveTrue(code).orElseThrow(() -> new NotFoundException("Unknown QR code"));
        Business business = businesses.findById(point.getBusinessId()).orElseThrow();
        String lang = Lang.isSupported(requestedLang) ? requestedLang : business.getDefaultLang();
        return new ScanView(
            new BusinessView(business.getName(), business.getType(), business.getTheme()),
            new ScanPointView(point.getLabel(), point.getKind()),
            lang, Lang.direction(lang), LABELS.get(business.getType()).get(lang),
            cachedCatalog(business, lang));
    }

    /** Cache first (Redis); on a miss, build from Oracle and store it. */
    List<CategoryView> cachedCatalog(Business business, String lang) {
        return cache.get(business.getId(), lang).orElseGet(() -> {
            List<CategoryView> built = catalog(business, lang);
            cache.put(business.getId(), lang, built);
            return built;
        });
    }

    /** The catalogue part depends only on business and language, so it is what gets cached. */
    @Transactional(readOnly = true)
    public List<CategoryView> catalog(Business business, String lang) {
        List<CategoryView> out = new ArrayList<>();
        List<Item> all = items.findByBusinessIdOrderByCategoryIdAscPositionAscIdAsc(business.getId());
        for (Category c : categories.findByBusinessIdOrderByPositionAscIdAsc(business.getId())) {
            List<ItemView> its = all.stream().filter(i -> i.getCategoryId() == c.getId()).map(i -> {
                ItemText t = Lang.pick(i.getTexts(), lang, business.getDefaultLang()).orElse(new ItemText("?", null));
                return new ItemView(i.getId(), t.name(), t.description(), i.getPriceCents(), i.getPhotoUrl(),
                    i.isOrderable(), i.getStock() != null && i.getStock() > 0 && i.getStock() <= 3 ? i.getStock() : null);
            }).toList();
            out.add(new CategoryView(c.getId(), Lang.pick(c.getNames(), lang, business.getDefaultLang()).orElse("?"), its));
        }
        return out;
    }

    public record Labels(String catalog, String scanPoint, String action) {}

    public record BusinessView(String name, BusinessType type, String theme) {}

    public record ScanPointView(String label, ScanPoint.Kind kind) {}

    /** fewLeft: shown only when 1 to 3 remain, so customers know to hurry without seeing exact stock. */
    public record ItemView(long id, String name, String description, long priceCents, String photoUrl,
                           boolean orderable, Integer fewLeft) {}

    public record CategoryView(long id, String name, List<ItemView> items) {}

    public record ScanView(BusinessView business, ScanPointView scanPoint, String lang, String dir, Labels labels,
                           List<CategoryView> categories) {}
}
