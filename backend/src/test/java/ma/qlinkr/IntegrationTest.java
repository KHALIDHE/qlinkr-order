package ma.qlinkr;

import java.util.List;
import java.util.Map;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Base class for tests that call the running API over HTTP. All subclasses
 * share one Spring context and one Oracle container, because their
 * configuration is identical and Spring caches the context.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "qlinkr.jwt.secret=test-only-secret-0123456789-abcdefghijklmnopqrstuvwxyz",
    "qlinkr.bootstrap.admin-email=" + IntegrationTest.ADMIN_EMAIL,
    "qlinkr.bootstrap.admin-password=" + IntegrationTest.ADMIN_PASSWORD,
    // Everything comes from 127.0.0.1 in tests: keep the public limits out of the way (RateLimitTest sets its own).
    "qlinkr.rate-limit.browse-per-ip=1000000",
    "qlinkr.rate-limit.orders-per-ip=1000000",
    "qlinkr.rate-limit.orders-per-scan-point=1000000",
})
public abstract class IntegrationTest {

    protected static final String ADMIN_EMAIL = "admin@qlinkr.test";
    protected static final String ADMIN_PASSWORD = "admin-password-123";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    protected int port;

    Response call(HttpMethod method, String path, String token, Object body) {
        RestClient client = RestClient.create("http://localhost:" + port);
        RestClient.RequestBodySpec spec = client.method(method).uri(path).contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            spec.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            spec.body(body);
        }
        return spec.exchange((req, res) -> {
            String text = new String(res.getBody().readAllBytes());
            String t = text.stripLeading();
            Object parsed = t.startsWith("{") || t.startsWith("[") ? JSON.readValue(text, Object.class) : null;
            return new Response(res.getStatusCode().value(), parsed, text);
        });
    }

    Response get(String path, String token) {
        return call(HttpMethod.GET, path, token, null);
    }

    Response post(String path, String token, Object body) {
        return call(HttpMethod.POST, path, token, body);
    }

    String login(String email, String password) {
        Response res = post("/api/auth/login", null, Map.of("email", email, "password", password));
        if (res.status() != 200) {
            throw new AssertionError("Login failed for " + email + ": " + res.raw());
        }
        return (String) res.map().get("accessToken");
    }

    String adminToken() {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    public record Response(int status, Object body, String raw) {

        @SuppressWarnings("unchecked")
        public Map<String, Object> map() {
            return (Map<String, Object>) body;
        }

        @SuppressWarnings("unchecked")
        public List<Map<String, Object>> list() {
            return (List<Map<String, Object>>) body;
        }
    }
}
