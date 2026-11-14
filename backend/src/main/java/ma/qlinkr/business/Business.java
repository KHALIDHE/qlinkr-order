package ma.qlinkr.business;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A tenant of the platform. Every business-owned row points to one of these. */
@Entity
@Table(name = "business")
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 80, unique = true, updatable = false)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, length = 20, updatable = false)
    private BusinessType type;

    @Column(name = "default_lang", nullable = false, length = 2)
    private String defaultLang;

    @Column(nullable = false, length = 20)
    private String theme;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Business() {
        // for JPA
    }

    public Business(String name, String slug, BusinessType type, String defaultLang) {
        this.name = name;
        this.slug = slug;
        this.type = type;
        this.defaultLang = defaultLang;
        this.theme = "simple";
        this.createdAt = Instant.now();
    }

    public void rename(String newName) {
        this.name = newName;
    }

    public void changeDefaultLang(String lang) {
        this.defaultLang = lang;
    }

    public void changeTheme(String newTheme) {
        this.theme = newTheme;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public BusinessType getType() { return type; }
    public String getDefaultLang() { return defaultLang; }
    public String getTheme() { return theme; }
    public Instant getCreatedAt() { return createdAt; }
}
