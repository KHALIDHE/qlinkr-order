package ma.qlinkr.auth;

import java.util.Optional;
import java.util.UUID;
import ma.qlinkr.user.AppUser;
import ma.qlinkr.user.AppUserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    /**
     * Hash of a random value, made once at startup. When the email is unknown
     * we still run one BCrypt comparison against it, so a wrong email and a
     * wrong password take the same time and nobody can probe which emails exist.
     */
    private final String dummyHash;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public TokenService.IssuedToken login(String email, String password) {
        Optional<AppUser> found = users.findByEmail(AppUser.normalizeEmail(email));
        String hash = found.map(AppUser::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(password, hash);

        if (found.isEmpty() || !passwordMatches || !found.get().isActive()) {
            // One message for every case: unknown email, wrong password, disabled account.
            throw new BadCredentialsException("Invalid email or password");
        }
        return tokens.issue(found.get());
    }
}
