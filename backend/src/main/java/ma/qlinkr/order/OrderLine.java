package ma.qlinkr.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One item in an order. Name and price are copied: later catalogue edits do not change past orders. */
@Entity
@Table(name = "order_line")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false, updatable = false)
    private long itemId;

    @Column(name = "item_name", nullable = false, updatable = false, length = 100)
    private String itemName;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Column(name = "unit_price_cents", nullable = false, updatable = false)
    private long unitPriceCents;

    protected OrderLine() {
        // for JPA
    }

    OrderLine(long itemId, String itemName, int quantity, long unitPriceCents) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
    }

    public long getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public int getQuantity() { return quantity; }
    public long getUnitPriceCents() { return unitPriceCents; }
}
