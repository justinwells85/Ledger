package com.ledger.integration;

import com.ledger.BaseIntegrationTest;
import com.ledger.dto.JournalLineRequest;
import com.ledger.entity.AccountType;
import com.ledger.entity.JournalEntry;
import com.ledger.entity.JournalEntryType;
import com.ledger.repository.FiscalPeriodRepository;
import com.ledger.repository.JournalEntryRepository;
import com.ledger.service.JournalService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for JournalService with DB (Testcontainers).
 * Spec: 02-journal-ledger.md, 10-business-rules.md (BR-01, BR-02)
 */
@Transactional
@Rollback
class JournalIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JournalService journalService;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private FiscalPeriodRepository fiscalPeriodRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID fiscalPeriodId;
    private UUID milestoneId;
    private UUID contractId;
    private String projectId;

    @BeforeEach
    void setUp() {
        // Use a real fiscal period from the seeded data (V008)
        var periods = fiscalPeriodRepository.findAll();
        assertThat(periods).isNotEmpty();
        fiscalPeriodId = periods.get(0).getPeriodId();

        // Create test contract
        contractId = UUID.randomUUID();
        entityManager.createNativeQuery(
                "INSERT INTO contract (contract_id, name, vendor, owner_user, start_date, status, created_at, created_by) " +
                "VALUES (:id, :name, :vendor, :owner, :startDate, 'ACTIVE', now(), 'system')")
                .setParameter("id", contractId)
                .setParameter("name", "Test Contract")
                .setParameter("vendor", "Test Vendor")
                .setParameter("owner", "system")
                .setParameter("startDate", LocalDate.of(2025, 10, 1))
                .executeUpdate();

        // Create test project
        projectId = "PR00001";
        entityManager.createNativeQuery(
                "INSERT INTO project (project_id, contract_id, wbse, name, funding_source, status, created_at, created_by) " +
                "VALUES (:pid, :cid, :wbse, :name, 'OPEX', 'ACTIVE', now(), 'system')")
                .setParameter("pid", projectId)
                .setParameter("cid", contractId)
                .setParameter("wbse", "WBSE-001")
                .setParameter("name", "Test Project")
                .executeUpdate();

        // Create test milestone
        milestoneId = UUID.randomUUID();
        entityManager.createNativeQuery(
                "INSERT INTO milestone (milestone_id, project_id, name, created_at, created_by) " +
                "VALUES (:mid, :pid, :name, now(), 'system')")
                .setParameter("mid", milestoneId)
                .setParameter("pid", projectId)
                .setParameter("name", "Test Milestone")
                .executeUpdate();

        entityManager.flush();
    }

    /**
     * I-JRN-01: Create balanced entry persists with all lines.
     * Spec: 02-journal-ledger.md Section 3, BR-01
     *
     * GIVEN a balanced entry with 2 lines (debit $25,000 = credit $25,000)
     * WHEN  createEntry is called
     * THEN  the entry is persisted and can be queried back with 2 lines
     */
    @Test
    void createBalancedEntry_persistsWithAllLines() {
        List<JournalLineRequest> lines = List.of(
                new JournalLineRequest(
                        AccountType.PLANNED, contractId, projectId, milestoneId, fiscalPeriodId,
                        new BigDecimal("25000.00"), BigDecimal.ZERO,
                        "MilestoneVersion", UUID.randomUUID()),
                new JournalLineRequest(
                        AccountType.VARIANCE_RESERVE, contractId, projectId, milestoneId, fiscalPeriodId,
                        BigDecimal.ZERO, new BigDecimal("25000.00"),
                        "MilestoneVersion", UUID.randomUUID())
        );

        JournalEntry entry = journalService.createEntry(
                JournalEntryType.PLAN_CREATE,
                LocalDate.of(2025, 11, 1),
                "New milestone: January Sustainment",
                "system",
                lines);

        assertThat(entry.getEntryId()).isNotNull();
        assertThat(entry.getEntryType()).isEqualTo(JournalEntryType.PLAN_CREATE);
        assertThat(entry.getDescription()).isEqualTo("New milestone: January Sustainment");

        // Query back from DB
        JournalEntry found = journalEntryRepository.findById(entry.getEntryId()).orElseThrow();
        assertThat(found.getLines()).hasSize(2);
        assertThat(found.getEffectiveDate()).isEqualTo(LocalDate.of(2025, 11, 1));
    }

    /**
     * I-JRN-03: Planned balance query returns correct sum.
     * Spec: 02-journal-ledger.md Section 6
     *
     * GIVEN a PLAN_CREATE entry with debit $25,000 PLANNED
     * WHEN  getPlannedBalance is called for the milestone
     * THEN  returns $25,000
     */
    @Test
    void plannedBalanceQuery_returnsCorrectSum() {
        List<JournalLineRequest> lines = List.of(
                new JournalLineRequest(
                        AccountType.PLANNED, contractId, projectId, milestoneId, fiscalPeriodId,
                        new BigDecimal("25000.00"), BigDecimal.ZERO,
                        null, null),
                new JournalLineRequest(
                        AccountType.VARIANCE_RESERVE, contractId, projectId, milestoneId, fiscalPeriodId,
                        BigDecimal.ZERO, new BigDecimal("25000.00"),
                        null, null)
        );

        journalService.createEntry(
                JournalEntryType.PLAN_CREATE,
                LocalDate.of(2025, 11, 1),
                "New milestone",
                "system",
                lines);

        BigDecimal balance = journalService.getPlannedBalance(milestoneId, LocalDate.of(2026, 12, 31));

        assertThat(balance).isEqualByComparingTo(new BigDecimal("25000.00"));
    }

    /**
     * I-JRN-04: Balance query with multiple entries aggregates.
     * Spec: 02-journal-ledger.md Section 5.2, Section 6
     *
     * GIVEN a PLAN_CREATE of $25,000 and then a PLAN_ADJUST of -$5,000
     * WHEN  getPlannedBalance is called
     * THEN  returns $20,000
     */
    @Test
    void balanceQueryWithMultipleEntries_aggregates() {
        // Create initial plan: $25,000 debit PLANNED
        journalService.createEntry(
                JournalEntryType.PLAN_CREATE,
                LocalDate.of(2025, 11, 1),
                "New milestone: $25K",
                "system",
                List.of(
                        new JournalLineRequest(
                                AccountType.PLANNED, contractId, projectId, milestoneId, fiscalPeriodId,
                                new BigDecimal("25000.00"), BigDecimal.ZERO,
                                null, null),
                        new JournalLineRequest(
                                AccountType.VARIANCE_RESERVE, contractId, projectId, milestoneId, fiscalPeriodId,
                                BigDecimal.ZERO, new BigDecimal("25000.00"),
                                null, null)
                ));

        // Adjust: reduce by $5,000 (credit PLANNED $5,000, debit VARIANCE_RESERVE $5,000)
        journalService.createEntry(
                JournalEntryType.PLAN_ADJUST,
                LocalDate.of(2026, 2, 15),
                "Budget reduction: -$5K",
                "system",
                List.of(
                        new JournalLineRequest(
                                AccountType.VARIANCE_RESERVE, contractId, projectId, milestoneId, fiscalPeriodId,
                                new BigDecimal("5000.00"), BigDecimal.ZERO,
                                null, null),
                        new JournalLineRequest(
                                AccountType.PLANNED, contractId, projectId, milestoneId, fiscalPeriodId,
                                BigDecimal.ZERO, new BigDecimal("5000.00"),
                                null, null)
                ));

        BigDecimal balance = journalService.getPlannedBalance(milestoneId, LocalDate.of(2026, 12, 31));

        assertThat(balance).isEqualByComparingTo(new BigDecimal("20000.00"));
    }

    /**
     * T06-9 / I-JRN-06: 4-line period shift entry balances (debit total = credit total).
     * Spec: 02-journal-ledger.md Section 5.3, BR-01
     *
     * GIVEN a period shift: credit PLANNED $20K from JAN, debit PLANNED $22K to FEB
     * WHEN  createEntry is called with 4 lines totaling $42K each side
     * THEN  entry persists with 4 lines and is balanced
     */
    @Test
    void fourLinePeriodShift_persistsAndBalances() {
        var janPeriodId = fiscalPeriodId; // reuse existing period as "JAN"
        // Use a different period for "FEB"
        var febPeriodId = fiscalPeriodRepository.findAll().stream()
                .filter(p -> !p.getPeriodId().equals(janPeriodId))
                .findFirst().orElseThrow().getPeriodId();

        List<JournalLineRequest> lines = List.of(
                // Remove from JAN: credit PLANNED $20K, debit VARIANCE_RESERVE $20K
                new JournalLineRequest(
                        AccountType.VARIANCE_RESERVE, contractId, projectId, milestoneId, janPeriodId,
                        new BigDecimal("20000.00"), BigDecimal.ZERO, null, null),
                new JournalLineRequest(
                        AccountType.PLANNED, contractId, projectId, milestoneId, janPeriodId,
                        BigDecimal.ZERO, new BigDecimal("20000.00"), null, null),
                // Add to FEB: debit PLANNED $22K, credit VARIANCE_RESERVE $22K
                new JournalLineRequest(
                        AccountType.PLANNED, contractId, projectId, milestoneId, febPeriodId,
                        new BigDecimal("22000.00"), BigDecimal.ZERO, null, null),
                new JournalLineRequest(
                        AccountType.VARIANCE_RESERVE, contractId, projectId, milestoneId, febPeriodId,
                        BigDecimal.ZERO, new BigDecimal("22000.00"), null, null)
        );

        JournalEntry entry = journalService.createEntry(
                JournalEntryType.PLAN_ADJUST,
                LocalDate.of(2026, 2, 1),
                "Period shift JAN → FEB",
                "system",
                lines);

        JournalEntry found = journalEntryRepository.findById(entry.getEntryId()).orElseThrow();
        assertThat(found.getLines()).hasSize(4);

        java.math.BigDecimal totalDebit = found.getLines().stream()
                .map(l -> l.getDebit()).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal totalCredit = found.getLines().stream()
                .map(l -> l.getCredit()).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        assertThat(totalDebit).isEqualByComparingTo(new BigDecimal("42000.00"));
        assertThat(totalCredit).isEqualByComparingTo(new BigDecimal("42000.00"));
    }

    /**
     * I-JRN-05: Balance query with asOfDate filters correctly.
     * Spec: 02-journal-ledger.md Section 6, BR-41
     *
     * GIVEN a PLAN_CREATE effective Nov 1 ($25,000) and a PLAN_ADJUST effective Feb 15 (-$5,000)
     * WHEN  getPlannedBalance is called with asOfDate = Jan 1
     * THEN  returns only the first entry's amount ($25,000)
     */
    @Test
    void balanceQueryWithAsOfDate_filtersCorrectly() {
        // Create initial plan effective Nov 1
        journalService.createEntry(
                JournalEntryType.PLAN_CREATE,
                LocalDate.of(2025, 11, 1),
                "New milestone",
                "system",
                List.of(
                        new JournalLineRequest(
                                AccountType.PLANNED, contractId, projectId, milestoneId, fiscalPeriodId,
                                new BigDecimal("25000.00"), BigDecimal.ZERO,
                                null, null),
                        new JournalLineRequest(
                                AccountType.VARIANCE_RESERVE, contractId, projectId, milestoneId, fiscalPeriodId,
                                BigDecimal.ZERO, new BigDecimal("25000.00"),
                                null, null)
                ));

        // Adjust effective Feb 15 (should be excluded when querying as of Jan 1)
        journalService.createEntry(
                JournalEntryType.PLAN_ADJUST,
                LocalDate.of(2026, 2, 15),
                "Budget reduction",
                "system",
                List.of(
                        new JournalLineRequest(
                                AccountType.VARIANCE_RESERVE, contractId, projectId, milestoneId, fiscalPeriodId,
                                new BigDecimal("5000.00"), BigDecimal.ZERO,
                                null, null),
                        new JournalLineRequest(
                                AccountType.PLANNED, contractId, projectId, milestoneId, fiscalPeriodId,
                                BigDecimal.ZERO, new BigDecimal("5000.00"),
                                null, null)
                ));

        // Query as of Jan 1, 2026 — should only see the Nov 1 entry
        BigDecimal balance = journalService.getPlannedBalance(milestoneId, LocalDate.of(2026, 1, 1));

        assertThat(balance).isEqualByComparingTo(new BigDecimal("25000.00"));
    }
}
