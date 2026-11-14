package ma.qlinkr.scan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A place where a QR code is put: a table, a counter, a shelf, a door. */
@Entity
@Table(name = "scan_point")
public class ScanPoint {

    public enum Kind { TABLE, COUNTER, SHELF, DOOR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false, updatable = false)
    private long businessId;

    /** Random, unguessable, printed in the QR code. Never reused. */
    @Column(nullable = false, updatable = false, length = 16)
    private String code;

    @Column(nullable = false, length = 60)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Kind kind;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ScanPoint() {
        // for JPA
    }

    public ScanPoint(long businessId, String code, String label, Kind kind) {
        this.businessId = businessId;
        this.code = code;
        this.label = label;
        this.kind = kind;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public void rename(String newLabel) { this.label = newLabel; }
    public void setActive(boolean value) { this.active = value; }

    public Long getId() { return id; }
    public long getBusinessId() { return businessId; }
    public String getCode() { return code; }
    public String getLabel() { return label; }
    public Kind getKind() { return kind; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
