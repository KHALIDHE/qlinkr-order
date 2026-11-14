package ma.qlinkr.order;

import java.time.Instant;
import java.util.List;

/** What staff and customers see of an order. Customers never see internal ids. */
public final class OrderViews {

    private OrderViews() {
    }

    public record LineView(long itemId, String name, int quantity, long unitPriceCents) {
        static LineView of(OrderLine l) {
            return new LineView(l.getItemId(), l.getItemName(), l.getQuantity(), l.getUnitPriceCents());
        }
    }

    public record PublicOrderView(String token, OrderKind kind, OrderStatus status, long totalCents,
                                  List<LineView> lines, Instant createdAt) {
        public static PublicOrderView of(CustomerOrder o) {
            return new PublicOrderView(o.getPublicToken(), o.getKind(), o.getStatus(), o.getTotalCents(),
                o.getLines().stream().map(LineView::of).toList(), o.getCreatedAt());
        }
    }

    public record StaffOrderView(long id, long scanPointId, String scanPointLabel, OrderKind kind, OrderStatus status,
                                 long totalCents, String note, List<LineView> lines, Instant createdAt,
                                 List<OrderStatus> allowedNext) {
        public static StaffOrderView of(CustomerOrder o, String scanPointLabel) {
            return new StaffOrderView(o.getId(), o.getScanPointId(), scanPointLabel, o.getKind(), o.getStatus(),
                o.getTotalCents(), o.getCustomerNote(), o.getLines().stream().map(LineView::of).toList(),
                o.getCreatedAt(), o.getStatus().next().stream().sorted().toList());
        }
    }

    /** Published after commit, for the live screens (phase 5). */
    public record OrderChanged(long businessId, String publicToken, StaffOrderView staffView, PublicOrderView publicView) {}
}
