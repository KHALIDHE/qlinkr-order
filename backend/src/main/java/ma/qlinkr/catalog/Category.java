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
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false, updatable = false)
    private long businessId;

    @Column(nullable = false)
    private int position;

    /** lang -> name, stored in CATEGORY_I18N (one row per language). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "category_i18n", joinColumns = @JoinColumn(name = "category_id"))
    @MapKeyColumn(name = "lang", length = 2)
    @Column(name = "name", nullable = false, length = 80)
    private Map<String, String> names = new HashMap<>();

    protected Category() {
        // for JPA
    }

    public Category(long businessId, int position, Map<String, String> names) {
        this.businessId = businessId;
        this.position = position;
        this.names.putAll(names);
    }

    public void update(Integer newPosition, Map<String, String> newNames) {
        if (newPosition != null) position = newPosition;
        if (newNames != null) {
            names.clear();
            names.putAll(newNames);
        }
    }

    public Long getId() { return id; }
    public long getBusinessId() { return businessId; }
    public int getPosition() { return position; }
    public Map<String, String> getNames() { return Map.copyOf(names); }
}
