package ma.qlinkr.order;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import ma.qlinkr.catalog.CatalogController.CatalogChanged;
import ma.qlinkr.catalog.ItemRepository;
import ma.qlinkr.catalog.Item;
import ma.qlinkr.catalog.ItemText;
import ma.qlinkr.catalog.Lang;
import ma.qlinkr.business.Business;
import ma.qlinkr.business.BusinessRepository;
import ma.qlinkr.common.NotFoundException;
import ma.qlinkr.order.OrderViews.OrderChanged;
import ma.qlinkr.order.OrderViews.PublicOrderView;
import ma.qlinkr.order.OrderViews.StaffOrderView;
import ma.qlinkr.scan.ScanPoint;
import ma.qlinkr.scan.ScanPointRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Placing orders and moving them through their lifecycle.
 *
 * Stock uses optimistic locking: an item row carries a version, and the
 * UPDATE that takes stock only succeeds if nobody changed the row since it
 * was read. When two customers race for the last item, one commit wins and
 * the other fails with a version conflict; that attempt is retried from a
 * fresh read, sees stock 0, and answers "no longer available".
 */
@Service
public class OrderService {

    private static final int MAX_ATTEMPTS = 40;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orders;
    private final ItemRepository items;
    private final ScanPointRepository scanPoints;
    private final BusinessRepository businesses;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public OrderService(OrderRepository orders, ItemRepository items, ScanPointRepository scanPoints,
                        BusinessRepository businesses, JdbcTemplate jdbc, PlatformTransactionManager txManager,
                        ApplicationEventPublisher events, Clock clock) {
        this.orders = orders;
        this.items = items;
        this.scanPoints = scanPoints;
        this.businesses = businesses;
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(txManager);
        this.events = events;
        this.clock = clock;
    }

    public record LineRequest(long itemId, int quantity) {}

    public record Placed(PublicOrderView view, boolean created) {}

    public Placed place(String scanCode, String idempotencyKey, OrderKind kind, List<LineRequest> lines, String note) {
        ScanPoint point = scanPoints.findByCodeAndActiveTrue(scanCode)
            .orElseThrow(() -> new NotFoundException("Unknown QR code"));
        long bid = point.getBusinessId();

        var existing = orders.findByBusinessIdAndIdempotencyKey(bid, idempotencyKey);
        if (existing.isPresent()) {
            return new Placed(PublicOrderView.of(existing.get()), false);
        }

        Business business = businesses.findById(bid).orElseThrow();
        Map<Long, Integer> wanted = new HashMap<>();
        lines.forEach(l -> wanted.merge(l.itemId(), l.quantity(), Integer::sum));

        for (int attempt = 1; ; attempt++) {
            try {
                CustomerOrder saved = tx.execute(s -> attemptPlace(point, business, idempotencyKey, kind, wanted, note));
                publish(saved, point.getLabel());
                events.publishEvent(new CatalogChanged(bid)); // stock changed: refresh the public catalogue
                return new Placed(PublicOrderView.of(saved), true);
            } catch (OptimisticLockingFailureException e) {
                // Someone else changed one of these items between our read and our write. Read again.
                if (attempt >= MAX_ATTEMPTS) {
                    throw new IllegalStateException("Too much contention on these items, please try again");
                }
                backoff(attempt);
            } catch (DataIntegrityViolationException e) {
                // The same idempotency key was committed by a parallel request: return that order.
                var winner = orders.findByBusinessIdAndIdempotencyKey(bid, idempotencyKey);
                if (winner.isPresent()) {
                    return new Placed(PublicOrderView.of(winner.get()), false);
                }
                throw e;
            }
        }
    }

    private CustomerOrder attemptPlace(ScanPoint point, Business business, String key, OrderKind kind,
                                       Map<Long, Integer> wanted, String note) {
        List<OrderLine> lines = new ArrayList<>();
        List<Long> unavailable = new ArrayList<>();
        for (var e : wanted.entrySet()) {
            Item item = items.findByIdAndBusinessId(e.getKey(), business.getId()).orElse(null);
            if (item == null || !item.isAvailable() || (item.getStock() != null && item.getStock() < e.getValue())) {
                unavailable.add(e.getKey());
                continue;
            }
            item.take(e.getValue()); // version is checked and incremented when the transaction flushes
            String name = Lang.pick(item.getTexts(), business.getDefaultLang(), business.getDefaultLang())
                .map(ItemText::name).orElse("item " + item.getId());
            lines.add(new OrderLine(item.getId(), name, e.getValue(), item.getPriceCents()));
        }
        if (!unavailable.isEmpty()) {
            throw new NoLongerAvailableException(unavailable);
        }
        CustomerOrder order = orders.saveAndFlush(new CustomerOrder(business.getId(), point.getId(), kind, key,
            newPublicToken(), lines, note, clock.instant()));
        recordEvent(order.getId(), null, OrderStatus.NEW, null);
        return order;
    }

    public PublicOrderView publicView(String token) {
        return orders.findByPublicToken(token).map(PublicOrderView::of)
            .orElseThrow(() -> new NotFoundException("Order not found"));
    }

    public List<StaffOrderView> listForStaff(long businessId, List<OrderStatus> statuses) {
        Map<Long, String> labels = new HashMap<>();
        scanPoints.findByBusinessIdOrderByIdAsc(businessId).forEach(p -> labels.put(p.getId(), p.getLabel()));
        return orders.findTop200ByBusinessIdAndStatusInOrderByCreatedAtAsc(businessId, statuses).stream()
            .map(o -> StaffOrderView.of(o, labels.get(o.getScanPointId()))).toList();
    }

    /** Staff move an order along the state machine. Cancelling or rejecting gives tracked stock back. */
    public StaffOrderView changeStatus(long businessId, long orderId, OrderStatus target, long actorUserId) {
        CustomerOrder updated = tx.execute(s -> {
            CustomerOrder o = orders.findByIdAndBusinessId(orderId, businessId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
            OrderStatus from = o.moveTo(target, clock.instant());
            if (target.releasesStock()) {
                for (OrderLine l : o.getLines()) {
                    // A plain conditional UPDATE: no read-modify-write race, and untracked items are left alone.
                    jdbc.update("UPDATE item SET stock = stock + ?, version = version + 1 WHERE id = ? AND stock IS NOT NULL",
                        l.getQuantity(), l.getItemId());
                }
            }
            recordEvent(o.getId(), from, target, actorUserId);
            return orders.saveAndFlush(o);
        });
        String label = scanPoints.findById(updated.getScanPointId()).map(ScanPoint::getLabel).orElse(null);
        publish(updated, label);
        if (target.releasesStock()) {
            events.publishEvent(new CatalogChanged(businessId));
        }
        return StaffOrderView.of(updated, label);
    }

    private void recordEvent(long orderId, OrderStatus from, OrderStatus to, Long actor) {
        jdbc.update("INSERT INTO order_event (order_id, from_status, to_status, actor_user_id, at) VALUES (?, ?, ?, ?, SYSTIMESTAMP)",
            orderId, from == null ? null : from.name(), to.name(), actor);
    }

    private void publish(CustomerOrder o, String scanPointLabel) {
        events.publishEvent(new OrderChanged(o.getBusinessId(), o.getPublicToken(),
            StaffOrderView.of(o, scanPointLabel), PublicOrderView.of(o)));
    }

    /** 128 random bits, URL-safe: the only thing that lets a phone follow its own order. */
    private static String newPublicToken() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static void backoff(int attempt) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(2, 6L + Math.min(attempt, 10) * 4L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
