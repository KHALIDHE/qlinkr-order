package ma.qlinkr.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_order")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false, updatable = false)
    private long businessId;

    @Column(name = "scan_point_id", nullable = false, updatable = false)
    private long scanPointId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12, updatable = false)
    private OrderKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private OrderStatus status;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "public_token", nullable = false, updatable = false, length = 32)
    private String publicToken;

    @Column(name = "total_cents", nullable = false, updatable = false)
    private long totalCents;

    @Column(name = "customer_note", length = 200, updatable = false)
    private String customerNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /** Two staff members changing the same order at once: the second save fails instead of overwriting. */
    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    @OrderBy("id")
    private List<OrderLine> lines = new ArrayList<>();

    protected CustomerOrder() {
        // for JPA
    }

    CustomerOrder(long businessId, long scanPointId, OrderKind kind, String idempotencyKey, String publicToken,
                  List<OrderLine> lines, String note, Instant now) {
        this.businessId = businessId;
        this.scanPointId = scanPointId;
        this.kind = kind;
        this.status = OrderStatus.NEW;
        this.idempotencyKey = idempotencyKey;
        this.publicToken = publicToken;
        this.lines.addAll(lines);
        this.totalCents = lines.stream().mapToLong(l -> l.getUnitPriceCents() * l.getQuantity()).sum();
        this.customerNote = note;
        this.createdAt = now;
    }

    /** Moves along the state machine or throws. Records when the main milestones happen, for the dashboard. */
    OrderStatus moveTo(OrderStatus target, Instant now) {
        if (!status.canMoveTo(target)) {
            throw new InvalidTransitionException(status, target);
        }
        OrderStatus from = status;
        status = target;
        switch (target) {
            case ACCEPTED -> acceptedAt = now;
            case READY -> readyAt = now;
            case COMPLETED -> completedAt = now;
            default -> { }
        }
        return from;
    }

    public Long getId() { return id; }
    public long getBusinessId() { return businessId; }
    public long getScanPointId() { return scanPointId; }
    public OrderKind getKind() { return kind; }
    public OrderStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getPublicToken() { return publicToken; }
    public long getTotalCents() { return totalCents; }
    public String getCustomerNote() { return customerNote; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getReadyAt() { return readyAt; }
    public Instant getCompletedAt() { return completedAt; }
    public List<OrderLine> getLines() { return List.copyOf(lines); }
}
