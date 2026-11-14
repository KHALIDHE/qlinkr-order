package ma.qlinkr.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import ma.qlinkr.auth.CurrentUser;
import ma.qlinkr.catalog.CategoryRepository;
import ma.qlinkr.catalog.ItemRepository;
import ma.qlinkr.common.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The owner edits the catalogue. Texts are maps of language -> text, at least
 * one language required. Every change publishes CatalogChanged, which clears
 * the public catalogue cache for that business.
 */
@RestController
@RequestMapping("/api/catalog")
@PreAuthorize("hasRole('OWNER')")
public class CatalogController {

    private final CategoryRepository categories;
    private final ItemRepository items;
    private final ApplicationEventPublisher events;

    public CatalogController(CategoryRepository categories, ItemRepository items, ApplicationEventPublisher events) {
        this.categories = categories;
        this.items = items;
        this.events = events;
    }

    public record CatalogChanged(long businessId) {}

    // --- categories ---------------------------------------------------------

    @GetMapping("/categories")
    public List<CategoryView> listCategories(@AuthenticationPrincipal Jwt jwt) {
        return categories.findByBusinessIdOrderByPositionAscIdAsc(bid(jwt)).stream().map(CategoryView::of).toList();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CategoryView createCategory(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CategoryRequest req) {
        long bid = bid(jwt);
        Category c = categories.save(new Category(bid, req.position() == null ? 0 : req.position(), names(req.names())));
        events.publishEvent(new CatalogChanged(bid));
        return CategoryView.of(c);
    }

    @PatchMapping("/categories/{id}")
    @Transactional
    public CategoryView updateCategory(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                       @Valid @RequestBody CategoryPatch req) {
        long bid = bid(jwt);
        Category c = categories.findByIdAndBusinessId(id, bid).orElseThrow(() -> new NotFoundException("Category not found"));
        c.update(req.position(), req.names() == null ? null : names(req.names()));
        events.publishEvent(new CatalogChanged(bid));
        return CategoryView.of(c);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteCategory(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        long bid = bid(jwt);
        categories.delete(categories.findByIdAndBusinessId(id, bid).orElseThrow(() -> new NotFoundException("Category not found")));
        events.publishEvent(new CatalogChanged(bid));
    }

    // --- items --------------------------------------------------------------

    @GetMapping("/items")
    public List<ItemView> listItems(@AuthenticationPrincipal Jwt jwt) {
        return items.findByBusinessIdOrderByCategoryIdAscPositionAscIdAsc(bid(jwt)).stream().map(ItemView::of).toList();
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ItemView createItem(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ItemRequest req) {
        long bid = bid(jwt);
        categories.findByIdAndBusinessId(req.categoryId(), bid)
            .orElseThrow(() -> new NotFoundException("Category not found"));
        Item item = items.save(new Item(bid, req.categoryId(), req.priceCents(), req.photoUrl(), req.stock(),
            req.position() == null ? 0 : req.position(), texts(req.texts())));
        events.publishEvent(new CatalogChanged(bid));
        return ItemView.of(item);
    }

    @PatchMapping("/items/{id}")
    @Transactional
    public ItemView updateItem(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @Valid @RequestBody ItemPatch req) {
        long bid = bid(jwt);
        Item item = items.findByIdAndBusinessId(id, bid).orElseThrow(() -> new NotFoundException("Item not found"));
        if (req.priceCents() != null) item.changePrice(req.priceCents());
        if (req.photoUrl() != null) item.changePhoto(req.photoUrl());
        if (req.available() != null) item.setAvailable(req.available());
        if (req.clearStock() != null && req.clearStock()) item.changeStock(null);
        else if (req.stock() != null) item.changeStock(req.stock());
        if (req.texts() != null) item.replaceTexts(texts(req.texts()));
        if (req.categoryId() != null) {
            categories.findByIdAndBusinessId(req.categoryId(), bid).orElseThrow(() -> new NotFoundException("Category not found"));
            item.moveTo(req.categoryId(), req.position() == null ? item.getPosition() : req.position());
        }
        events.publishEvent(new CatalogChanged(bid));
        return ItemView.of(item);
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteItem(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        long bid = bid(jwt);
        items.delete(items.findByIdAndBusinessId(id, bid).orElseThrow(() -> new NotFoundException("Item not found")));
        events.publishEvent(new CatalogChanged(bid));
    }

    // --- helpers and DTOs -----------------------------------------------------

    private static long bid(Jwt jwt) {
        return CurrentUser.from(jwt).requireBusinessId();
    }

    private static Map<String, String> names(Map<String, @Size(min = 1, max = 80) String> in) {
        in.keySet().forEach(CatalogController::requireLang);
        return in;
    }

    private static Map<String, ItemText> texts(Map<String, ItemTextRequest> in) {
        in.keySet().forEach(CatalogController::requireLang);
        Map<String, ItemText> out = new java.util.HashMap<>();
        in.forEach((lang, t) -> out.put(lang, new ItemText(t.name(), t.description())));
        return out;
    }

    private static void requireLang(String lang) {
        if (!Lang.isSupported(lang)) {
            throw new IllegalArgumentException("Unsupported language '" + lang + "', use ar, fr or en");
        }
    }

    public record CategoryRequest(@NotEmpty Map<@Pattern(regexp = "ar|fr|en") String, @Size(min = 1, max = 80) String> names,
                                  @Min(0) @Max(9999) Integer position) {}

    public record CategoryPatch(Map<@Pattern(regexp = "ar|fr|en") String, @Size(min = 1, max = 80) String> names,
                                @Min(0) @Max(9999) Integer position) {}

    public record ItemTextRequest(@NotNull @Size(min = 1, max = 100) String name, @Size(max = 500) String description) {}

    public record ItemRequest(@NotNull Long categoryId,
                              @NotEmpty Map<@Pattern(regexp = "ar|fr|en") String, @Valid ItemTextRequest> texts,
                              @NotNull @PositiveOrZero @Max(100_000_000) Long priceCents,
                              @Size(max = 500) @Pattern(regexp = "^https?://.*", message = "must be an http(s) URL") String photoUrl,
                              @PositiveOrZero Integer stock,
                              @Min(0) @Max(9999) Integer position) {}

    public record ItemPatch(Long categoryId,
                            Map<@Pattern(regexp = "ar|fr|en") String, @Valid ItemTextRequest> texts,
                            @PositiveOrZero @Max(100_000_000) Long priceCents,
                            @Size(max = 500) @Pattern(regexp = "^https?://.*", message = "must be an http(s) URL") String photoUrl,
                            Boolean available,
                            @PositiveOrZero Integer stock,
                            Boolean clearStock,
                            @Min(0) @Max(9999) Integer position) {}

    public record CategoryView(long id, int position, Map<String, String> names) {
        static CategoryView of(Category c) {
            return new CategoryView(c.getId(), c.getPosition(), c.getNames());
        }
    }

    public record ItemView(long id, long categoryId, long priceCents, String photoUrl, boolean available,
                           Integer stock, int position, Map<String, ItemText> texts) {
        static ItemView of(Item i) {
            return new ItemView(i.getId(), i.getCategoryId(), i.getPriceCents(), i.getPhotoUrl(), i.isAvailable(),
                i.getStock(), i.getPosition(), i.getTexts());
        }
    }
}
