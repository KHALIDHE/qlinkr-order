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
})
public abstract class IntegrationTest {

    protected static final String ADMIN_EMAIL = "admin@qlinkr.test";
    protected static final String ADMIN_PASSWORD = "admin-password-123";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    protected int port;

    protected Response call(HttpMethod method, String path, String token, Object body) {
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
            Object parsed = text.isBlank() ? null : JSON.readValue(text, Object.class);
            return new Response(res.getStatusCode().value(), parsed, text);
        });
    }

    protected Response get(String path, String token) {
        return call(HttpMethod.GET, path, token, null);
    }

    protected Response post(String path, String token, Object body) {
        return call(HttpMethod.POST, path, token, body);
    }

    protected String login(String email, String password) {
        Response res = post("/api/auth/login", null, Map.of("email", email, "password", password));
        if (res.status() != 200) {
            throw new AssertionError("Login failed for " + email + ": " + res.raw());
        }
        return (String) res.map().get("accessToken");
    }

    protected String adminToken() {
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
