package ma.qlinkr.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Reports whether the Flyway baseline reached the database.
 * Shows up under "schema" in GET /actuator/health.
 */
@Component
public class SchemaHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbc;

    public SchemaHealthIndicator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Health health() {
        try {
            String schema = jdbc.queryForObject(
                "SELECT meta_value FROM app_meta WHERE meta_key = 'schema'", String.class);
            return Health.up().withDetail("schema", schema).build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
