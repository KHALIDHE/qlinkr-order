package ma.qlinkr.catalog;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "item")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false, updatable = false)
    private long businessId;

    @Column(name = "category_id", nullable = false)
    private long categoryId;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(nullable = false)
    private boolean available;

    /** Null when stock is not tracked (a coffee); a number for limited items (the last 10 croissants). */
    private Integer stock;

    @Column(nullable = false)
    private int position;

    /**
     * Optimistic locking: every UPDATE adds "AND version = ?" and increments it.
     * Used by ordering (phase 4) so two customers cannot both take the last item.
     */
    @Version
    @Column(nullable = false)
    private long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "item_i18n", joinColumns = @JoinColumn(name = "item_id"))
    @MapKeyColumn(name = "lang", length = 2)
    private Map<String, ItemText> texts = new HashMap<>();

    protected Item() {
        // for JPA
    }

    public Item(long businessId, long categoryId, long priceCents, String photoUrl, Integer stock, int position,
                Map<String, ItemText> texts) {
        this.businessId = businessId;
        this.categoryId = categoryId;
        this.priceCents = priceCents;
        this.photoUrl = photoUrl;
        this.available = true;
        this.stock = stock;
        this.position = position;
        this.texts.putAll(texts);
    }

    public void changePrice(long cents) { this.priceCents = cents; }
    public void changePhoto(String url) { this.photoUrl = url; }
    public void setAvailable(boolean value) { this.available = value; }
    public void changeStock(Integer value) { this.stock = value; }
    public void moveTo(long category, int newPosition) { this.categoryId = category; this.position = newPosition; }

    public void replaceTexts(Map<String, ItemText> newTexts) {
        texts.clear();
        texts.putAll(newTexts);
    }

    /** Takes quantity from a tracked stock. The caller's transaction fails on a version conflict. */
    public void take(int quantity) {
        if (stock == null) {
            return;
        }
        if (stock < quantity) {
            throw new IllegalStateException("not enough stock");
        }
        stock -= quantity;
    }

    public boolean isOrderable() {
        return available && (stock == null || stock > 0);
    }

    public Long getId() { return id; }
    public long getBusinessId() { return businessId; }
    public long getCategoryId() { return categoryId; }
    public long getPriceCents() { return priceCents; }
    public String getPhotoUrl() { return photoUrl; }
    public boolean isAvailable() { return available; }
    public Integer getStock() { return stock; }
    public int getPosition() { return position; }
    public long getVersion() { return version; }
    public Map<String, ItemText> getTexts() { return Map.copyOf(texts); }
}
