package ma.qlinkr.publicapi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import ma.qlinkr.order.OrderKind;
import ma.qlinkr.order.OrderService;
import ma.qlinkr.order.OrderViews.PublicOrderView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** What customers call from their phone after scanning. No account, no token. */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final PublicCatalogService catalog;
    private final OrderService orders;

    public PublicController(PublicCatalogService catalog, OrderService orders) {
        this.catalog = catalog;
        this.orders = orders;
    }

    @GetMapping("/scan/{code}")
    public PublicCatalogService.ScanView scan(@PathVariable String code, @RequestParam(required = false) String lang) {
        return catalog.scan(code, lang);
    }

    /**
     * Places an order. The Idempotency-Key header is generated once by the
     * phone for this cart: a double tap or a network retry sends the same key
     * and gets the same order back (200) instead of a second order (201).
     */
    @PostMapping("/scan/{code}/orders")
    public ResponseEntity<PublicOrderView> placeOrder(
            @PathVariable String code,
            @RequestHeader("Idempotency-Key") @Pattern(regexp = "^[A-Za-z0-9-]{8,64}$") String idempotencyKey,
            @Valid @RequestBody PlaceOrderRequest req) {
        OrderService.Placed placed = orders.place(code, idempotencyKey, req.kind() == null ? OrderKind.ORDER : req.kind(),
            req.lines().stream().map(l -> new OrderService.LineRequest(l.itemId(), l.quantity())).toList(), req.note());
        return ResponseEntity.status(placed.created() ? HttpStatus.CREATED : HttpStatus.OK).body(placed.view());
    }

    @GetMapping("/orders/{publicToken}")
    public PublicOrderView order(@PathVariable String publicToken) {
        return orders.publicView(publicToken);
    }

    public record LineRequest(@NotNull Long itemId, @NotNull @Min(1) @Max(50) Integer quantity) {}

    public record PlaceOrderRequest(@NotEmpty @Size(max = 30) List<@Valid LineRequest> lines,
                                    @Size(max = 200) String note, OrderKind kind) {}
}
