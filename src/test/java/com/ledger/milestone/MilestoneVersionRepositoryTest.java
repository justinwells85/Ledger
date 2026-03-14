package com.ledger.milestone;

import com.ledger.BaseIntegrationTest;
import com.ledger.entity.*;
import com.ledger.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")

/**
 * T07 — Milestone + Version Entity
 * Spec: 01-domain-model.md Sections 2.5/2.6, 04-milestone-versioning.md
 * Tests: T07-1 through T07-5
 */
class MilestoneVersionRepositoryTest extends BaseIntegrationTest {

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
    private JournalLineRepository journalLineRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    private Milestone milestone;
    private FiscalPeriod period;

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

        milestone = new Milestone();
        milestone.setProject(project);
        milestone.setName("January Sustainment");
        milestone.setCreatedBy("system");
        milestoneRepository.save(milestone);

        // Use any seed fiscal period (FY26-04-JAN is January 2026)
        period = fiscalPeriodRepository.findByPeriodKey("FY26-04-JAN")
                .orElseThrow(() -> new IllegalStateException("Seed data missing FY26-04-JAN period"));
    }

    private MilestoneVersion makeVersion(int number, LocalDate effective, BigDecimal amount) {
        MilestoneVersion v = new MilestoneVersion();
        v.setMilestone(milestone);
        v.setVersionNumber(number);
        v.setPlannedAmount(amount);
        v.setFiscalPeriod(period);
        v.setEffectiveDate(effective);
        v.setReason("Version " + number);
        v.setCreatedBy("system");
        return v;
    }

    // TEST T07-1: MilestoneVersion unique constraint on (milestone_id, version_number)
    // Spec: 01-domain-model.md Section 2.6, V003 migration UNIQUE constraint
    @Test
    void uniqueConstraint_onMilestoneIdAndVersionNumber() {
        milestoneVersionRepository.save(makeVersion(1, LocalDate.of(2025, 11, 1), new BigDecimal("25000.00")));
        milestoneVersionRepository.flush();

        MilestoneVersion duplicate = makeVersion(1, LocalDate.of(2025, 12, 1), new BigDecimal("30000.00"));
        assertThatThrownBy(() -> {
            milestoneVersionRepository.saveAndFlush(duplicate);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    // TEST T07-2: Find current version (highest version_number) — BR-40
    // Spec: 04-milestone-versioning.md, BR-40
    @Test
    void findCurrentVersion_returnsHighestVersionNumber() {
        milestoneVersionRepository.save(makeVersion(1, LocalDate.of(2025, 11, 1), new BigDecimal("25000.00")));
        milestoneVersionRepository.save(makeVersion(2, LocalDate.of(2025, 12, 1), new BigDecimal("27000.00")));
        milestoneVersionRepository.save(makeVersion(3, LocalDate.of(2026, 1, 1), new BigDecimal("30000.00")));
        milestoneVersionRepository.flush();

        Optional<MilestoneVersion> current = milestoneVersionRepository.findCurrentVersion(milestone.getMilestoneId());

        assertThat(current).isPresent();
        assertThat(current.get().getVersionNumber()).isEqualTo(3);
        assertThat(current.get().getPlannedAmount()).isEqualByComparingTo(new BigDecimal("30000.00"));
    }

    // TEST T07-3: Find version as of date — returns latest where effective_date <= asOfDate
    // Spec: 04-milestone-versioning.md, BR-41
    @Test
    void findVersionAsOfDate_returnsCorrectVersion() {
        milestoneVersionRepository.save(makeVersion(1, LocalDate.of(2025, 11, 1), new BigDecimal("25000.00")));
        milestoneVersionRepository.save(makeVersion(2, LocalDate.of(2026, 2, 15), new BigDecimal("27000.00")));
        milestoneVersionRepository.flush();

        Optional<MilestoneVersion> result = milestoneVersionRepository
                .findVersionAsOfDate(milestone.getMilestoneId(), LocalDate.of(2026, 1, 1));

        assertThat(result).isPresent();
        assertThat(result.get().getVersionNumber()).isEqualTo(1);
        assertThat(result.get().getPlannedAmount()).isEqualByComparingTo(new BigDecimal("25000.00"));
    }

    // TEST T07-4: Find version as of date — exact date match returns that version
    // Spec: 04-milestone-versioning.md, BR-41
    @Test
    void findVersionAsOfDate_exactDateMatch() {
        milestoneVersionRepository.save(makeVersion(1, LocalDate.of(2025, 11, 1), new BigDecimal("25000.00")));
        milestoneVersionRepository.save(makeVersion(2, LocalDate.of(2026, 2, 15), new BigDecimal("27000.00")));
        milestoneVersionRepository.flush();

        Optional<MilestoneVersion> result = milestoneVersionRepository
                .findVersionAsOfDate(milestone.getMilestoneId(), LocalDate.of(2026, 2, 15));

        assertThat(result).isPresent();
        assertThat(result.get().getVersionNumber()).isEqualTo(2);
    }

    // TEST T07-5: Find version as of date — before any version returns empty
    // Spec: 04-milestone-versioning.md, BR-41
    @Test
    void findVersionAsOfDate_beforeFirstVersion_returnsEmpty() {
        milestoneVersionRepository.save(makeVersion(1, LocalDate.of(2025, 11, 1), new BigDecimal("25000.00")));
        milestoneVersionRepository.flush();

        Optional<MilestoneVersion> result = milestoneVersionRepository
                .findVersionAsOfDate(milestone.getMilestoneId(), LocalDate.of(2025, 10, 1));

        assertThat(result).isEmpty();
    }
}
