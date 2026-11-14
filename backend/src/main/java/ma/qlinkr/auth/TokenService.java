package ma.qlinkr.auth;

import java.time.Instant;
import ma.qlinkr.user.AppUser;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues access tokens. The claims are exactly what CurrentUser reads back. */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final JwtProperties props;

    public TokenService(JwtEncoder encoder, JwtProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    public IssuedToken issue(AppUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(props.ttl());

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
            .issuer(props.issuer())
            .subject(String.valueOf(user.getId()))
            .issuedAt(now)
            .expiresAt(expiresAt)
            .claim(CurrentUser.CLAIM_EMAIL, user.getEmail())
            .claim(CurrentUser.CLAIM_ROLE, user.getRole().name());
        if (user.getBusinessId() != null) {
            claims.claim(CurrentUser.CLAIM_BUSINESS, user.getBusinessId());
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new IssuedToken(value, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {}
}
