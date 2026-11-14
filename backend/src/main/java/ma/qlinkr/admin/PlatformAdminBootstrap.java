package ma.qlinkr.admin;

import ma.qlinkr.user.AppUser;
import ma.qlinkr.user.AppUserRepository;
import ma.qlinkr.user.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first platform admin from environment variables, once.
 * Without it there would be no way to log in on a fresh database.
 */
@Component
public class PlatformAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformAdminBootstrap.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public PlatformAdminBootstrap(AppUserRepository users, PasswordEncoder passwordEncoder,
                                  @Value("${qlinkr.bootstrap.admin-email:}") String email,
                                  @Value("${qlinkr.bootstrap.admin-password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByRole(Role.PLATFORM_ADMIN)) {
            return;
        }
        if (email.isBlank() || password.length() < 10) {
            log.warn("No platform admin exists. Set PLATFORM_ADMIN_EMAIL and PLATFORM_ADMIN_PASSWORD (10+ characters) to create one.");
            return;
        }
        users.save(AppUser.platformAdmin(email, passwordEncoder.encode(password)));
        log.info("Created platform admin {}", AppUser.normalizeEmail(email));
    }
}
