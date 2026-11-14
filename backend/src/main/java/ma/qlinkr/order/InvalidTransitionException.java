package ma.qlinkr.order;

/** A status change the state machine does not allow, such as READY -> NEW. */
public class InvalidTransitionException extends RuntimeException {

    public InvalidTransitionException(OrderStatus from, OrderStatus to) {
        super("An order cannot go from " + from + " to " + to + "; allowed next: " + from.next());
    }
}
