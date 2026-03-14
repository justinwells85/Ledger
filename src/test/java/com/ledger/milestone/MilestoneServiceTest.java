package com.ledger.milestone;

import com.ledger.BaseIntegrationTest;
import com.ledger.entity.*;
import com.ledger.repository.*;
import com.ledger.service.MilestoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T08 — Milestone Creation (v1 + PLAN_CREATE)
 * Spec: 04-milestone-versioning.md Section 4, 02-journal-ledger.md Section 5.1
 * Tests: T08-1 through T08-3 (service layer)
 */
class MilestoneServiceTest extends BaseIntegrationTest {

    @Autowired
    private MilestoneService milestoneService;

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
    private UUID fiscalPeriodId;
    private UUID contractId;

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
        contractId = contract.getContractId();

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

        FiscalPeriod period = fiscalPeriodRepository.findByPeriodKey("FY26-04-JAN")
                .orElseThrow(() -> new IllegalStateException("Seed data missing FY26-04-JAN"));
        fiscalPeriodId = period.getPeriodId();
    }

    // TEST T08-1: Create milestone produces v1 and PLAN_CREATE journal entry
    // Spec: 04-milestone-versioning.md Section 4, 02-journal-ledger.md 5.1
    @Test
    void createMilestone_producesVersionAndJournalEntry() {
        Milestone milestone = milestoneService.createMilestone(
                projectId,
                "January Sustainment",
                "Monthly sustainment",
                new BigDecimal("25250.00"),
                fiscalPeriodId,
                LocalDate.of(2025, 11, 1),
                "Initial budget allocation",
                "system"
        );

        assertThat(milestone.getMilestoneId()).isNotNull();
        assertThat(milestone.getName()).isEqualTo("January Sustainment");

        List<MilestoneVersion> versions = milestoneVersionRepository.findVersionHistory(milestone.getMilestoneId());
        assertThat(versions).hasSize(1);
        MilestoneVersion v1 = versions.get(0);
        assertThat(v1.getVersionNumber()).isEqualTo(1);
        assertThat(v1.getPlannedAmount()).isEqualByComparingTo(new BigDecimal("25250.00"));

        List<JournalEntry> entries = journalEntryRepository.findAll();
        assertThat(entries).hasSize(1);
        JournalEntry entry = entries.get(0);
        assertThat(entry.getEntryType()).isEqualTo(JournalEntryType.PLAN_CREATE);

        List<JournalLine> lines = journalLineRepository.findAll();
        assertThat(lines).hasSize(2);

        JournalLine plannedLine = lines.stream()
                .filter(l -> l.getAccount() == AccountType.PLANNED)
                .findFirst().orElseThrow();
        assertThat(plannedLine.getDebit()).isEqualByComparingTo(new BigDecimal("25250.00"));
        assertThat(plannedLine.getCredit()).isEqualByComparingTo(BigDecimal.ZERO);

        JournalLine vrLine = lines.stream()
                .filter(l -> l.getAccount() == AccountType.VARIANCE_RESERVE)
                .findFirst().orElseThrow();
        assertThat(vrLine.getCredit()).isEqualByComparingTo(new BigDecimal("25250.00"));
        assertThat(vrLine.getDebit()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // TEST T08-2: Journal entry balances
    // Spec: 02-journal-ledger.md BR-01
    @Test
    void createMilestone_journalEntryBalances() {
        milestoneService.createMilestone(
                projectId, "Feb Sustainment", null,
                new BigDecimal("18500.00"),
                fiscalPeriodId,
                LocalDate.of(2025, 11, 1),
                "Initial budget",
                "system"
        );

        List<JournalLine> lines = journalLineRepository.findAll();
        BigDecimal totalDebit = lines.stream().map(JournalLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream().map(JournalLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDebit).isEqualByComparingTo(totalCredit);
    }

    // TEST T08-3: Version references the correct fiscal period
    // Spec: 04-milestone-versioning.md Section 4
    @Test
    void createMilestone_versionReferencesCorrectPeriod() {
        Milestone milestone = milestoneService.createMilestone(
                projectId, "Jan Sustainment", null,
                new BigDecimal("25250.00"),
                fiscalPeriodId,
                LocalDate.of(2025, 11, 1),
                "Initial",
                "system"
        );

        MilestoneVersion v1 = milestoneVersionRepository
                .findCurrentVersion(milestone.getMilestoneId())
                .orElseThrow();

        assertThat(v1.getFiscalPeriod().getPeriodId()).isEqualTo(fiscalPeriodId);
        assertThat(v1.getFiscalPeriod().getPeriodKey()).isEqualTo("FY26-04-JAN");
    }

    // TEST T08-5: Negative planned amount is rejected
    // Spec: 04-milestone-versioning.md, domain constraint
    @Test
    void createMilestone_negativeAmount_throwsException() {
        assertThatThrownBy(() -> milestoneService.createMilestone(
                projectId, "Bad Milestone", null,
                new BigDecimal("-1000.00"),
                fiscalPeriodId,
                LocalDate.of(2025, 11, 1),
                "reason",
                "system"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("planned amount");
    }
}
