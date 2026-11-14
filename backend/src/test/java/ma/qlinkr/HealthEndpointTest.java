package ma.qlinkr;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

/**
 * Starts the whole application against a real Oracle, then checks that
 * Flyway ran and that the health endpoint is open without a login.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                properties = "spring.docker.compose.enabled=false")
class HealthEndpointTest {

    @Autowired
    JdbcTemplate jdbc;

    @org.springframework.beans.factory.annotation.Value("${local.server.port}")
    int port;

    @Test
    void flywayBaselineCreatedTheMetaTable() {
        String schema = jdbc.queryForObject(
            "SELECT meta_value FROM app_meta WHERE meta_key = 'schema'", String.class);
        assertThat(schema).isEqualTo("qlinkr-order");

        Integer applied = jdbc.queryForObject(
            // Flyway creates its table with quoted lower-case names on Oracle.
            "SELECT COUNT(*) FROM \"flyway_schema_history\" WHERE \"success\" = 1", Integer.class);
        assertThat(applied).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void healthIsPublicAndUp() {
        RestClient client = RestClient.create("http://localhost:" + port);

        Map<String, Object> body = client.get().uri("/actuator/health")
            .retrieve().body(Map.class);

        assertThat(body).containsEntry("status", "UP");
        Map<String, Object> components = (Map<String, Object>) body.get("components");
        assertThat(components).containsKeys("db", "redis", "schema");
    }

    @Test
    void everythingElseNeedsALogin() {
        RestClient client = RestClient.create("http://localhost:" + port);

        var response = client.get().uri("/api/anything")
            .exchange((req, res) -> res.getStatusCode().value());

        assertThat(response).isEqualTo(401);
    }
}
