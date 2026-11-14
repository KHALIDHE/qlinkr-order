package ma.qlinkr.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.EnumSet;
import java.util.List;
import ma.qlinkr.auth.CurrentUser;
import ma.qlinkr.order.OrderViews.StaffOrderView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The staff screen: open orders of the caller's business, and status changes. */
@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
public class StaffOrderController {

    private static final List<OrderStatus> OPEN =
        List.of(OrderStatus.NEW, OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY);

    private final OrderService orders;

    public StaffOrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping
    public List<StaffOrderView> list(@AuthenticationPrincipal Jwt jwt,
                                     @RequestParam(required = false) List<OrderStatus> status) {
        long bid = CurrentUser.from(jwt).requireBusinessId();
        return orders.listForStaff(bid, status == null || status.isEmpty() ? OPEN : List.copyOf(EnumSet.copyOf(status)));
    }

    @PostMapping("/{id}/status")
    public StaffOrderView changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                       @Valid @RequestBody StatusChange req) {
        CurrentUser user = CurrentUser.from(jwt);
        return orders.changeStatus(user.requireBusinessId(), id, req.to(), user.userId());
    }

    public record StatusChange(@NotNull OrderStatus to) {}
}
