package ma.qlinkr;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Flyway ran on a real Oracle, health is public, everything else needs a login. */
class HealthEndpointTest extends IntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayAppliedEveryMigration() {
        String schema = jdbc.queryForObject(
            "SELECT meta_value FROM app_meta WHERE meta_key = 'schema'", String.class);
        assertThat(schema).isEqualTo("qlinkr-order");

        // Flyway creates its table with quoted lower-case names on Oracle.
        Integer applied = jdbc.queryForObject(
            "SELECT COUNT(*) FROM \"flyway_schema_history\" WHERE \"success\" = 1", Integer.class);
        assertThat(applied).isEqualTo(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void healthIsPublicAndUp() {
        Response res = get("/actuator/health", null);

        assertThat(res.map()).containsEntry("status", "UP");
        Map<String, Object> components = (Map<String, Object>) res.map().get("components");
        assertThat(components).containsKeys("db", "redis", "schema");
    }

    @Test
    void everythingElseNeedsALogin() {
        assertThat(get("/api/anything", null).status()).isEqualTo(401);
    }
}
