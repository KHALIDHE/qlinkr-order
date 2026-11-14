package ma.qlinkr;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class AuthIntegrationTest extends IntegrationTest {

    @Autowired
    JwtEncoder realEncoder;

    @Test
    void adminLogsInAndSeesItsIdentity() {
        String token = adminToken();

        Response me = get("/api/auth/me", token);

        assertThat(me.status()).isEqualTo(200);
        assertThat(me.map()).containsEntry("email", ADMIN_EMAIL).containsEntry("role", "PLATFORM_ADMIN");
        assertThat(me.map().get("businessId")).isNull();
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameAnswer() {
        Response wrongPassword = post("/api/auth/login", null,
            Map.of("email", ADMIN_EMAIL, "password", "not-the-password"));
        Response unknownEmail = post("/api/auth/login", null,
            Map.of("email", "nobody-" + UUID.randomUUID() + "@qlinkr.test", "password", "whatever-123"));

        assertThat(wrongPassword.status()).isEqualTo(401);
        assertThat(unknownEmail.status()).isEqualTo(401);
        assertThat(wrongPassword.map().get("detail")).isEqualTo(unknownEmail.map().get("detail"));
    }

    @Test
    void emailIsCaseInsensitive() {
        assertThat(login(ADMIN_EMAIL.toUpperCase(), ADMIN_PASSWORD)).isNotBlank();
    }

    @Test
    void invalidLoginBodyIs400WithFieldErrors() {
        Response res = post("/api/auth/login", null, Map.of("email", "not-an-email", "password", ""));

        assertThat(res.status()).isEqualTo(400);
        @SuppressWarnings("unchecked")
        Map<String, Object> errors = (Map<String, Object>) res.map().get("errors");
        assertThat(errors).containsKeys("email", "password");
    }

    @Test
    void missingTokenIs401() {
        assertThat(get("/api/auth/me", null).status()).isEqualTo(401);
        assertThat(get("/api/business", null).status()).isEqualTo(401);
    }

    @Test
    void expiredTokenIs401() {
        Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
        String expired = sign(realEncoder, JwtClaimsSet.builder()
            .issuer("qlinkr").subject("1").issuedAt(past).expiresAt(past.plus(10, ChronoUnit.MINUTES))
            .claim("role", "PLATFORM_ADMIN").claim("email", ADMIN_EMAIL).build());

        assertThat(get("/api/auth/me", expired).status()).isEqualTo(401);
    }

    @Test
    void tokenSignedWithAnotherKeyIs401() {
        var otherKey = new SecretKeySpec(
            "a-completely-different-secret-key-of-48-bytes!!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder attackerEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(otherKey));
        String forged = sign(attackerEncoder, JwtClaimsSet.builder()
            .issuer("qlinkr").subject("1").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600))
            .claim("role", "PLATFORM_ADMIN").claim("email", ADMIN_EMAIL).build());

        assertThat(get("/api/auth/me", forged).status()).isEqualTo(401);
    }

    @Test
    void editingThePayloadBreaksTheSignature() {
        String token = adminToken();
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String edited = payload.replace("PLATFORM_ADMIN", "OWNER");
        String tampered = parts[0] + "." + Base64.getUrlEncoder().withoutPadding()
            .encodeToString(edited.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];

        assertThat(get("/api/auth/me", tampered).status()).isEqualTo(401);
    }

    @Test
    void tokenFromAnotherIssuerIs401() {
        String other = sign(realEncoder, JwtClaimsSet.builder()
            .issuer("someone-else").subject("1").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600))
            .claim("role", "PLATFORM_ADMIN").claim("email", ADMIN_EMAIL).build());

        assertThat(get("/api/auth/me", other).status()).isEqualTo(401);
    }

    private static String sign(JwtEncoder encoder, JwtClaimsSet claims) {
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    }
}
