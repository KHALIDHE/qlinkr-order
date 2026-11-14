package ma.qlinkr.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null only for PLATFORM_ADMIN; the database enforces it with a CHECK constraint. */
    @Column(name = "business_id", updatable = false)
    private Long businessId;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private Role role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AppUser() {
        // for JPA
    }

    private AppUser(Long businessId, String email, String passwordHash, Role role) {
        this.businessId = businessId;
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public static AppUser platformAdmin(String email, String passwordHash) {
        return new AppUser(null, email, passwordHash, Role.PLATFORM_ADMIN);
    }

    public static AppUser member(long businessId, String email, String passwordHash, Role role) {
        if (role == Role.PLATFORM_ADMIN) {
            throw new IllegalArgumentException("A platform admin cannot belong to a business");
        }
        return new AppUser(businessId, email, passwordHash, role);
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    public Long getId() { return id; }
    public Long getBusinessId() { return businessId; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
