package com.ledger.milestone;

import com.ledger.BaseIntegrationTest;
import com.ledger.entity.*;
import com.ledger.repository.*;
import com.ledger.service.MilestoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T09 — Milestone Version Adjustment (PLAN_ADJUST)
 * Spec: 04-milestone-versioning.md Section 4 (Adjusting), 02-journal-ledger.md 5.2–5.4
 * Tests: T09-1 through T09-8
 */
@AutoConfigureMockMvc
class MilestoneVersionAdjustmentTest extends BaseIntegrationTest {

    @Autowired
    private MilestoneService milestoneService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private FiscalPeriodRepository fiscalPeriodRepository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private MilestoneVersionRepository milestoneVersionRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private JournalLineRepository journalLineRepository;

    private String projectId;
    private UUID janPeriodId;
    private UUID febPeriodId;
    private Milestone milestone;

    @BeforeEach
    void setUp() {
        journalLineRepository.deleteAll();
        journalEntryRepository.deleteAll();
        milestoneVersionRepository.deleteAll();
        milestoneRepository.deleteAll();
        projectRepository.deleteAll();
        contractRepository.deleteAll();

        Contract contract = new Contract();
        contract.setName("Globant ADM");
        contract.setVendor("Globant");
        contract.setOwnerUser("Rob");
        contract.setStartDate(LocalDate.of(2025, 10, 1));
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setCreatedBy("system");
        contractRepository.save(contract);

        Project project = new Project();
        project.setProjectId("PR13752");
        project.setContract(contract);
        project.setWbse("1174905.SU.ES");
        project.setName("DPI Photopass");
        project.setFundingSource(FundingSource.OPEX);
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedBy("system");
        projectRepository.save(project);
        projectId = "PR13752";

        janPeriodId = fiscalPeriodRepository.findByPeriodKey("FY26-04-JAN").orElseThrow().getPeriodId();
        febPeriodId = fiscalPeriodRepository.findByPeriodKey("FY26-05-FEB").orElseThrow().getPeriodId();

        // Create base milestone at $25,250 in JAN
        milestone = milestoneService.createMilestone(
                projectId,
                "January Sustainment",
                null,
                new BigDecimal("25250.00"),
                janPeriodId,
                LocalDate.of(2025, 11, 1),
                "Initial budget allocation",
                "system"
        );
    }

    // TEST T09-1: Adjust amount down — same period
    // Spec: 04-milestone-versioning.md Section 4 (same-period decrease)
    @Test
    void createVersion_decreaseSamePeriod_createsCorrectJournal() {
        milestoneService.createVersion(
                milestone.getMilestoneId(),
                new BigDecimal("20000.00"),
                janPeriodId,
                LocalDate.of(2026, 2, 15),
                "scope cut",
                "system"
        );

        MilestoneVersion v2 = milestoneVersionRepository
                .findCurrentVersion(milestone.getMilestoneId()).orElseThrow();
        assertThat(v2.getVersionNumber()).isEqualTo(2);
        assertThat(v2.getPlannedAmount()).isEqualByComparingTo(new BigDecimal("20000.00"));

        // PLAN_ADJUST entry: decrease → debit VARIANCE_RESERVE $5,250, credit PLANNED $5,250
        List<JournalEntry> entries = journalEntryRepository.findAll();
        JournalEntry adjustEntry = entries.stream()
                .filter(e -> e.getEntryType() == JournalEntryType.PLAN_ADJUST)
                .findFirst().orElseThrow();

        List<JournalLine> lines = journalLineRepository.findAll().stream()
                .filter(l -> l.getJournalEntry().getEntryId().equals(adjustEntry.getEntryId()))
                .toList();
        assertThat(lines).hasSize(2);

        JournalLine vrLine = lines.stream()
                .filter(l -> l.getAccount() == AccountType.VARIANCE_RESERVE).findFirst().orElseThrow();
        assertThat(vrLine.getDebit()).isEqualByComparingTo(new BigDecimal("5250.00"));

        JournalLine plannedLine = lines.stream()
                .filter(l -> l.getAccount() == AccountType.PLANNED).findFirst().orElseThrow();
        assertThat(plannedLine.getCredit()).isEqualByComparingTo(new BigDecimal("5250.00"));
    }

    // TEST T09-2: Adjust amount up — same period
    // Spec: 04-milestone-versioning.md Section 4 (same-period increase)
    @Test
    void createVersion_increaseSamePeriod_createsCorrectJournal() {
        milestoneService.createVersion(
                milestone.getMilestoneId(),
                new BigDecimal("30000.00"),
                janPeriodId,
                LocalDate.of(2026, 3, 1),
                "added testing",
                "system"
        );

        MilestoneVersion v2 = milestoneVersionRepository
                .findCurrentVersion(milestone.getMilestoneId()).orElseThrow();
        assertThat(v2.getPlannedAmount()).isEqualByComparingTo(new BigDecimal("30000.00"));

        // PLAN_ADJUST: increase → debit PLANNED $4,750, credit VARIANCE_RESERVE $4,750
        List<JournalEntry> entries = journalEntryRepository.findAll();
        JournalEntry adjustEntry = entries.stream()
                .filter(e -> e.getEntryType() == JournalEntryType.PLAN_ADJUST)
                .findFirst().orElseThrow();

        List<JournalLine> lines = journalLineRepository.findAll().stream()
                .filter(l -> l.getJournalEntry().getEntryId().equals(adjustEntry.getEntryId()))
                .toList();
        assertThat(lines).hasSize(2);

        JournalLine plannedLine = lines.stream()
                .filter(l -> l.getAccount() == AccountType.PLANNED).findFirst().orElseThrow();
        assertThat(plannedLine.getDebit()).isEqualByComparingTo(new BigDecimal("4750.00"));

        JournalLine vrLine = lines.stream()
                .filter(l -> l.getAccount() == AccountType.VARIANCE_RESERVE).findFirst().orElseThrow();
        assertThat(vrLine.getCredit()).isEqualByComparingTo(new BigDecimal("4750.00"));
    }

    // TEST T09-3: Period shift with amount change — 4-line journal
    // Spec: 04-milestone-versioning.md Section 4 (period shift)
    @Test
    void createVersion_periodShift_creates4LineJournal() {
        milestoneService.createVersion(
                milestone.getMilestoneId(),
                new BigDecimal("22000.00"),
                febPeriodId,
                LocalDate.of(2026, 3, 1),
                "shifted to Feb",
                "system"
        );

        MilestoneVersion v2 = milestoneVersionRepository
                .findCurrentVersion(milestone.getMilestoneId()).orElseThrow();
        assertThat(v2.getPlannedAmount()).isEqualByComparingTo(new BigDecimal("22000.00"));
        assertThat(v2.getFiscalPeriod().getPeriodId()).isEqualTo(febPeriodId);

        JournalEntry adjustEntry = journalEntryRepository.findAll().stream()
                .filter(e -> e.getEntryType() == JournalEntryType.PLAN_ADJUST)
                .findFirst().orElseThrow();

        List<JournalLine> lines = journalLineRepository.findAll().stream()
                .filter(l -> l.getJournalEntry().getEntryId().equals(adjustEntry.getEntryId()))
                .toList();
        assertThat(lines).hasSize(4);

        BigDecimal totalDebit = lines.stream().map(JournalLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream().map(JournalLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        // 4-line: $25,250 (remove old) + $22,000 (add new) = $47,250 each side
        assertThat(totalDebit).isEqualByComparingTo(totalCredit);
        assertThat(totalDebit).isEqualByComparingTo(new BigDecimal("47250.00"));
    }

    // TEST T09-4: Version number auto-increments
    // Spec: 04-milestone-versioning.md V-01
    @Test
    void createVersion_versionNumberAutoIncrements() {
        milestoneService.createVersion(milestone.getMilestoneId(), new BigDecimal("20000.00"),
                janPeriodId, LocalDate.of(2026, 2, 1), "cut", "system");
        milestoneService.createVersion(milestone.getMilestoneId(), new BigDecimal("22000.00"),
                janPeriodId, LocalDate.of(2026, 3, 1), "restore", "system");

        List<MilestoneVersion> history = milestoneVersionRepository
                .findVersionHistory(milestone.getMilestoneId());
        assertThat(history).hasSize(3);
        assertThat(history.get(0).getVersionNumber()).isEqualTo(1);
        assertThat(history.get(1).getVersionNumber()).isEqualTo(2);
        assertThat(history.get(2).getVersionNumber()).isEqualTo(3);
    }

    // TEST T09-5: Effective date must be >= prior version's effective_date — BR-05
    // Spec: 04-milestone-versioning.md V-03
    @Test
    void createVersion_effectiveDateBeforePrior_throwsException() {
        milestoneService.createVersion(milestone.getMilestoneId(), new BigDecimal("20000.00"),
                janPeriodId, LocalDate.of(2026, 2, 15), "cut", "system");

        assertThatThrownBy(() -> milestoneService.createVersion(
                milestone.getMilestoneId(),
                new BigDecimal("22000.00"),
                janPeriodId,
                LocalDate.of(2026, 2, 1), // before 2026-02-15
                "rollback",
                "system"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("effective_date");
    }

    // TEST T09-6: Reason is required — BR-42
    // Spec: 04-milestone-versioning.md V-04
    @Test
    void createVersion_emptyReason_throwsException() {
        assertThatThrownBy(() -> milestoneService.createVersion(
                milestone.getMilestoneId(),
                new BigDecimal("20000.00"),
                janPeriodId,
                LocalDate.of(2026, 2, 1),
                "",
                "system"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("reason");
    }

    // TEST T09-7: Net planned balance after multiple adjustments
    // Spec: 02-journal-ledger.md Section 6
    @Test
    void createVersion_multipleSamePeriodAdjustments_correctBalance() {
        milestoneService.createVersion(milestone.getMilestoneId(), new BigDecimal("20000.00"),
                janPeriodId, LocalDate.of(2026, 2, 1), "cut", "system");
        milestoneService.createVersion(milestone.getMilestoneId(), new BigDecimal("22000.00"),
                janPeriodId, LocalDate.of(2026, 3, 1), "restore", "system");

        BigDecimal balance = milestoneService.getPlannedBalance(
                milestone.getMilestoneId(), LocalDate.now());
        assertThat(balance).isEqualByComparingTo(new BigDecimal("22000.00"));
    }

    // TEST T09-8: GET /api/v1/milestones/{id}/versions returns all ordered
    // Spec: 13-api-design.md Section 5
    @Test
    void getVersionHistory_returnsAllVersionsOrdered() throws Exception {
        milestoneService.createVersion(milestone.getMilestoneId(), new BigDecimal("20000.00"),
                janPeriodId, LocalDate.of(2026, 2, 1), "cut", "system");
        milestoneService.createVersion(milestone.getMilestoneId(), new BigDecimal("22000.00"),
                janPeriodId, LocalDate.of(2026, 3, 1), "restore", "system");

        mockMvc.perform(get("/api/v1/milestones/{id}/versions", milestone.getMilestoneId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].versionNumber").value(1))
                .andExpect(jsonPath("$[1].versionNumber").value(2))
                .andExpect(jsonPath("$[2].versionNumber").value(3));
    }
}
