package com.ledger.integration;

import com.ledger.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T02: Database + Flyway
 * Verifies that all 8 Flyway migrations run correctly, all tables/indexes/constraints/triggers
 * are created, and seed data is present.
 *
 * Spec refs: 12-database-schema.md, V001-V008
 */
class DatabaseMigrationTest extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * T02-1: All migrations run successfully.
     * GIVEN an empty PostgreSQL database
     * WHEN  the application starts
     * THEN  Flyway reports 10 migrations applied (V001-V010)
     * AND   all 14 tables exist
     *
     * Spec: 12-database-schema.md | Migration Inventory
     */
    @Test
    void allMigrationsRunSuccessfully() {
        // Verify Flyway applied exactly 12 migrations (V001-V012)
        Integer migrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
                Integer.class
        );
        assertThat(migrationCount).isEqualTo(12);

        // Verify all tables exist (V001 through V011 create tables, V008-V009 are seed/index)
        List<String> expectedTables = List.of(
                "fiscal_year",                // V001
                "fiscal_period",              // V001
                "contract",                   // V002
                "project",                    // V002
                "milestone",                  // V003
                "milestone_version",          // V003
                "journal_entry",              // V004
                "journal_line",               // V004
                "sap_import",                 // V005
                "actual_line",                // V005
                "reconciliation",             // V006
                "audit_log",                  // V007
                "system_config",              // V007
                "ref_funding_source",         // V011
                "ref_contract_status",        // V011
                "ref_project_status",         // V011
                "ref_reconciliation_category" // V011
        );

        for (String table : expectedTables) {
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                    Integer.class,
                    table
            );
            assertThat(exists)
                    .as("Table '%s' should exist", table)
                    .isEqualTo(1);
        }
    }

    /**
     * T02-2: Fiscal calendar seed data present.
     * GIVEN migrations have run
     * WHEN  querying fiscal_period
     * THEN  36 periods exist (12 per FY x 3 fiscal years)
     * AND   FY26 Q1 contains Oct, Nov, Dec
     * AND   FY26 Q2 contains Jan, Feb, Mar
     *
     * Spec: 12-database-schema.md | V008 seed data, 03-fiscal-calendar.md
     */
    @Test
    void fiscalCalendarSeedDataPresent() {
        // Verify 36 total periods (12 per FY x 3 fiscal years)
        Integer periodCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fiscal_period",
                Integer.class
        );
        assertThat(periodCount).isEqualTo(36);

        // Verify 3 fiscal years exist
        Integer yearCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fiscal_year",
                Integer.class
        );
        assertThat(yearCount).isEqualTo(3);

        // Verify 12 periods per fiscal year
        List<Map<String, Object>> periodsByYear = jdbcTemplate.queryForList(
                "SELECT fiscal_year, COUNT(*) as cnt FROM fiscal_period GROUP BY fiscal_year ORDER BY fiscal_year"
        );
        assertThat(periodsByYear).hasSize(3);
        for (Map<String, Object> row : periodsByYear) {
            assertThat(((Number) row.get("cnt")).intValue())
                    .as("Fiscal year %s should have 12 periods", row.get("fiscal_year"))
                    .isEqualTo(12);
        }

        // FY26 Q1 contains Oct, Nov, Dec (sort_order 1, 2, 3)
        List<String> fy26Q1Months = jdbcTemplate.queryForList(
                "SELECT period_key FROM fiscal_period WHERE fiscal_year = 'FY26' AND quarter = 'Q1' ORDER BY sort_order",
                String.class
        );
        assertThat(fy26Q1Months).containsExactly("FY26-01-OCT", "FY26-02-NOV", "FY26-03-DEC");

        // FY26 Q2 contains Jan, Feb, Mar (sort_order 4, 5, 6)
        List<String> fy26Q2Months = jdbcTemplate.queryForList(
                "SELECT period_key FROM fiscal_period WHERE fiscal_year = 'FY26' AND quarter = 'Q2' ORDER BY sort_order",
                String.class
        );
        assertThat(fy26Q2Months).containsExactly("FY26-04-JAN", "FY26-05-FEB", "FY26-06-MAR");
    }

    /**
     * T02-3: System config defaults present.
     * GIVEN migrations have run
     * WHEN  querying system_config
     * THEN  tolerance_percent = 0.02
     * AND   tolerance_absolute = 50.00
     * AND   accrual_aging_warning_days = 60
     * AND   accrual_aging_critical_days = 90
     *
     * Spec: 12-database-schema.md | V007, 10-business-rules.md | BR-31, BR-32
     */
    @Test
    void systemConfigDefaultsPresent() {
        String tolerancePercent = jdbcTemplate.queryForObject(
                "SELECT config_value FROM system_config WHERE config_key = 'tolerance_percent'",
                String.class
        );
        assertThat(new BigDecimal(tolerancePercent)).isEqualByComparingTo(new BigDecimal("0.02"));

        String toleranceAbsolute = jdbcTemplate.queryForObject(
                "SELECT config_value FROM system_config WHERE config_key = 'tolerance_absolute'",
                String.class
        );
        assertThat(new BigDecimal(toleranceAbsolute)).isEqualByComparingTo(new BigDecimal("50.00"));

        String warningDays = jdbcTemplate.queryForObject(
                "SELECT config_value FROM system_config WHERE config_key = 'accrual_aging_warning_days'",
                String.class
        );
        assertThat(Integer.parseInt(warningDays)).isEqualTo(60);

        String criticalDays = jdbcTemplate.queryForObject(
                "SELECT config_value FROM system_config WHERE config_key = 'accrual_aging_critical_days'",
                String.class
        );
        assertThat(Integer.parseInt(criticalDays)).isEqualTo(90);
    }

    /**
     * T02-4: Journal balance trigger exists and rejects unbalanced entries.
     * GIVEN migrations have run
     * WHEN  inserting an unbalanced journal entry (debit $100, credit $50) and committing
     * THEN  transaction is rejected with balance error
     *
     * Spec: 12-database-schema.md | BR-01, 02-journal-ledger.md | BR-01
     */
    @Test
    void journalBalanceTriggerRejectsUnbalancedEntries() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        // Get a fiscal period ID for journal_line FK
        UUID periodId = jdbcTemplate.queryForObject(
                "SELECT period_id FROM fiscal_period LIMIT 1",
                UUID.class
        );

        assertThatThrownBy(() -> txTemplate.executeWithoutResult(status -> {
            // Insert a journal entry
            UUID entryId = UUID.randomUUID();
            jdbcTemplate.update(
                    "INSERT INTO journal_entry (entry_id, entry_date, effective_date, entry_type, description, created_by) " +
                            "VALUES (?, now(), ?, 'PLAN_CREATE', 'Unbalanced test entry', 'test')",
                    entryId, LocalDate.now()
            );

            // Insert debit line: $100
            jdbcTemplate.update(
                    "INSERT INTO journal_line (line_id, entry_id, account, fiscal_period_id, debit, credit) " +
                            "VALUES (?, ?, 'PLANNED', ?, 100.00, 0.00)",
                    UUID.randomUUID(), entryId, periodId
            );

            // Insert credit line: $50 (unbalanced!)
            jdbcTemplate.update(
                    "INSERT INTO journal_line (line_id, entry_id, account, fiscal_period_id, debit, credit) " +
                            "VALUES (?, ?, 'VARIANCE_RESERVE', ?, 0.00, 50.00)",
                    UUID.randomUUID(), entryId, periodId
            );

            // The deferred constraint trigger fires on commit — this should throw
        })).rootCause().hasMessageContaining("unbalanced");
    }
}
