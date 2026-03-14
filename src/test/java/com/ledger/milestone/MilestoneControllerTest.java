package com.ledger.milestone;

import com.ledger.BaseIntegrationTest;
import com.ledger.entity.*;
import com.ledger.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T08 — Milestone Creation API tests (T08-4, T08-5)
 * Spec: 13-api-design.md Section 5, 04-milestone-versioning.md
 */
@AutoConfigureMockMvc
class MilestoneControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private FiscalPeriodRepository fiscalPeriodRepository;

    @Autowired
    private MilestoneVersionRepository milestoneVersionRepository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private JournalLineRepository journalLineRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    private String projectId;
    private UUID fiscalPeriodId;

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

        fiscalPeriodId = fiscalPeriodRepository.findByPeriodKey("FY26-04-JAN")
                .orElseThrow(() -> new IllegalStateException("Seed data missing FY26-04-JAN"))
                .getPeriodId();
    }

    // TEST T08-4: POST /api/v1/projects/{id}/milestones returns full response
    // Spec: 13-api-design.md Section 5
    @Test
    void createMilestone_returns201WithFullResponse() throws Exception {
        String body = """
                {
                  "name": "January Sustainment",
                  "description": "Monthly sustainment work",
                  "plannedAmount": 25250.00,
                  "fiscalPeriodId": "%s",
                  "effectiveDate": "2025-11-01",
                  "reason": "Initial budget allocation"
                }
                """.formatted(fiscalPeriodId);

        mockMvc.perform(post("/api/v1/projects/{id}/milestones", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.milestoneId").isNotEmpty())
                .andExpect(jsonPath("$.name").value("January Sustainment"))
                .andExpect(jsonPath("$.currentVersion.versionNumber").value(1))
                .andExpect(jsonPath("$.currentVersion.plannedAmount").value(25250.00))
                .andExpect(jsonPath("$.currentVersion.fiscalPeriodId").value(fiscalPeriodId.toString()));
    }

    // TEST T08-5: Negative plannedAmount returns 400
    // Spec: 04-milestone-versioning.md domain constraint
    @Test
    void createMilestone_negativeAmount_returns400() throws Exception {
        String body = """
                {
                  "name": "Bad Milestone",
                  "plannedAmount": -1000.00,
                  "fiscalPeriodId": "%s",
                  "effectiveDate": "2025-11-01",
                  "reason": "reason"
                }
                """.formatted(fiscalPeriodId);

        mockMvc.perform(post("/api/v1/projects/{id}/milestones", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
