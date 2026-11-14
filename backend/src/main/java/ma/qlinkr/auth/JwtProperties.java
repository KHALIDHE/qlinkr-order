package ma.qlinkr.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * qlinkr.jwt.* settings. The secret comes from the JWT_SECRET environment
 * variable and is never committed. HS256 needs at least 256 bits, so a
 * shorter secret stops the application at startup instead of producing
 * weak tokens.
 */
@ConfigurationProperties(prefix = "qlinkr.jwt")
public record JwtProperties(String secret, Duration ttl, String issuer) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                "qlinkr.jwt.secret (JWT_SECRET) must be set and at least 32 bytes long");
        }
        if (ttl == null) {
            ttl = Duration.ofHours(8);
        }
        if (issuer == null) {
            issuer = "qlinkr";
        }
    }
}
