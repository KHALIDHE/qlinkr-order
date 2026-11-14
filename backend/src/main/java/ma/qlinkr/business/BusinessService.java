package ma.qlinkr.business;

import java.util.List;
import ma.qlinkr.common.ConflictException;
import ma.qlinkr.common.NotFoundException;
import ma.qlinkr.user.AppUser;
import ma.qlinkr.user.AppUserRepository;
import ma.qlinkr.user.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business and staff management. Every method that touches one business
 * receives its id from the caller's token (see CurrentUser), and every
 * member lookup filters on it.
 */
@Service
public class BusinessService {

    private final BusinessRepository businesses;
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public BusinessService(BusinessRepository businesses, AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.businesses = businesses;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    /** Platform admin only: one transaction creates the business and its owner, or neither. */
    @Transactional
    public Business createWithOwner(String name, String slug, BusinessType type, String defaultLang,
                                    String ownerEmail, String ownerPassword) {
        if (businesses.existsBySlug(slug)) {
            throw new ConflictException("The slug '" + slug + "' is already used");
        }
        ensureEmailFree(ownerEmail);
        Business business = businesses.save(new Business(name, slug, type, defaultLang));
        users.save(AppUser.member(business.getId(), ownerEmail, passwordEncoder.encode(ownerPassword), Role.OWNER));
        return business;
    }

    @Transactional(readOnly = true)
    public List<Business> listAll() {
        return businesses.findAll();
    }

    @Transactional(readOnly = true)
    public Business get(long businessId) {
        return businesses.findById(businessId)
            .orElseThrow(() -> new NotFoundException("Business not found"));
    }

    @Transactional
    public Business update(long businessId, String name, String defaultLang, String theme) {
        Business business = get(businessId);
        if (name != null) business.rename(name);
        if (defaultLang != null) business.changeDefaultLang(defaultLang);
        if (theme != null) business.changeTheme(theme);
        return business; // dirty checking: JPA writes the changes at commit
    }

    @Transactional(readOnly = true)
    public List<AppUser> listMembers(long businessId) {
        return users.findByBusinessIdOrderByIdAsc(businessId);
    }

    @Transactional
    public AppUser addStaff(long businessId, String email, String password) {
        ensureEmailFree(email);
        return users.save(AppUser.member(businessId, email, passwordEncoder.encode(password), Role.STAFF));
    }

    @Transactional
    public void removeStaff(long businessId, long userId) {
        AppUser member = users.findByIdAndBusinessId(userId, businessId)
            .filter(u -> u.getRole() == Role.STAFF)
            .orElseThrow(() -> new NotFoundException("Staff member not found"));
        users.delete(member);
    }

    private void ensureEmailFree(String email) {
        if (users.existsByEmail(AppUser.normalizeEmail(email))) {
            throw new ConflictException("This email is already used");
        }
    }
}
