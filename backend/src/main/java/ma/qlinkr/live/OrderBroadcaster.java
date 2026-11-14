package ma.qlinkr.live;

import ma.qlinkr.order.OrderViews.OrderChanged;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Pushes every order change to the staff screen of its business and to the
 * customer's phone. OrderService publishes OrderChanged only after its
 * transaction has committed, so screens never show an order that was rolled back.
 */
@Component
public class OrderBroadcaster {

    private final SimpMessagingTemplate broker;

    public OrderBroadcaster(SimpMessagingTemplate broker) {
        this.broker = broker;
    }

    @EventListener
    public void on(OrderChanged change) {
        broker.convertAndSend("/topic/business." + change.businessId() + ".orders", change.staffView());
        broker.convertAndSend("/topic/orders." + change.publicToken(), change.publicView());
    }
}
