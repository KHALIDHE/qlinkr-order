package ma.qlinkr.order;

import java.util.Set;

/**
 * The order lifecycle as a state machine. Each status lists where it may go
 * next; anything else is refused. Kept in the enum itself so the rules sit in
 * one place and are checked by one method.
 *
 *   NEW -> ACCEPTED -> PREPARING -> READY -> COMPLETED
 *   NEW -> REJECTED
 *   NEW / ACCEPTED / PREPARING -> CANCELLED
 */
public enum OrderStatus {
    NEW,
    ACCEPTED,
    PREPARING,
    READY,
    COMPLETED,
    CANCELLED,
    REJECTED;

    public Set<OrderStatus> next() {
        return switch (this) {
            case NEW -> Set.of(ACCEPTED, REJECTED, CANCELLED);
            case ACCEPTED -> Set.of(PREPARING, CANCELLED);
            case PREPARING -> Set.of(READY, CANCELLED);
            case READY -> Set.of(COMPLETED);
            case COMPLETED, CANCELLED, REJECTED -> Set.of();
        };
    }

    public boolean canMoveTo(OrderStatus target) {
        return next().contains(target);
    }

    public boolean isFinal() {
        return next().isEmpty();
    }

    /** Ending this way gives the reserved stock back. */
    public boolean releasesStock() {
        return this == CANCELLED || this == REJECTED;
    }
}
