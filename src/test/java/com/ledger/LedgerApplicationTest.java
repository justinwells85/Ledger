package com.ledger;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T01-1: Application context loads.
 * T01-2: Testcontainers PostgreSQL starts and is accessible.
 *
 * Spec ref: CLAUDE.md — project skeleton, test infrastructure.
 */
class LedgerApplicationTest extends BaseIntegrationTest {

    @Autowired
    private DataSource dataSource;

    /**
     * T01-1: Application context loads.
     * GIVEN a Spring Boot application with all config
     * WHEN  the application starts
     * THEN  the context loads without errors
     */
    @Test
    void contextLoads() {
        // If we reach this point, the Spring application context loaded successfully.
        // The @SpringBootTest annotation on BaseIntegrationTest ensures the full context starts.
    }

    /**
     * T01-2: Testcontainers PostgreSQL starts and is accessible.
     * GIVEN a test extending the base test class
     * WHEN  the test runs
     * THEN  a PostgreSQL container is running and accessible
     */
    @Test
    void testcontainersPostgresqlIsAccessible() throws Exception {
        assertThat(postgres.isRunning()).isTrue();

        try (Connection conn = dataSource.getConnection()) {
            assertThat(conn.isValid(5)).isTrue();

            // Verify we can execute a query against the PostgreSQL database
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT 1")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(1);
            }
        }
    }
}
