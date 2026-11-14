package ma.qlinkr;

import java.time.Duration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Real Oracle and Redis for tests. @ServiceConnection hands the container's
 * host, port, user and password to Spring, so no test needs a URL.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    OracleContainer oracleFreeContainer() {
        // First start of Oracle Free takes 1 to 3 minutes on a laptop; the default
        // timeout is 60 s. Reuse keeps the container alive between local test runs
        // when ~/.testcontainers.properties has testcontainers.reuse.enable=true.
        // Each project gets its own user, so a reused container is never shared
        // with another project's Flyway history.
        return new OracleContainer(DockerImageName.parse("gvenzl/oracle-free:23-slim-faststart"))
            .withUsername("qlinkr")
            .withPassword("qlinkr")
            .withStartupTimeout(Duration.ofMinutes(5))
            .withReuse(true);
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
    }
}
