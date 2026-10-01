package co.com.fcv.training.citas;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/** Uses the isolated compose test database during containerized development and Testcontainers elsewhere. */
abstract class DatabaseIntegrationSupport {
    private static final boolean USE_EXTERNAL_DATABASE = System.getenv("TEST_DB_URL") != null;
    private static final MySQLContainer<?> MYSQL = USE_EXTERNAL_DATABASE ? null : new MySQLContainer<>("mysql:8.4");

    static {
        if (!USE_EXTERNAL_DATABASE) MYSQL.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        if (USE_EXTERNAL_DATABASE) {
            registry.add("spring.datasource.url", () -> System.getenv("TEST_DB_URL"));
            registry.add("spring.datasource.username", () -> System.getenv("TEST_DB_USER"));
            registry.add("spring.datasource.password", () -> System.getenv("TEST_DB_PASSWORD"));
        } else {
            registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL::getUsername);
            registry.add("spring.datasource.password", MYSQL::getPassword);
        }
        // Each test class with its own @DynamicPropertySource gets its own cached Spring context and Hikari pool.
        // With ~16 suites the default pool of 10 exceeds MySQL's default max_connections (151): keep test pools small.
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 3);
        registry.add("spring.datasource.hikari.minimum-idle", () -> 1);
    }
}
