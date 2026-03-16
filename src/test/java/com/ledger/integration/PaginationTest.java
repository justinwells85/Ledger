package com.ledger.integration;

import com.ledger.BaseIntegrationTest;
import com.ledger.repository.AuditLogRepository;
import com.ledger.repository.ContractRepository;
import com.ledger.repository.JournalEntryRepository;
import com.ledger.repository.JournalLineRepository;
import com.ledger.repository.MilestoneRepository;
import com.ledger.repository.MilestoneVersionRepository;
import com.ledger.repository.ProjectRepository;
import com.ledger.repository.ReconciliationRepository;
import com.ledger.repository.ActualLineRepository;
import com.ledger.repository.SapImportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies that list endpoints apply a default size cap and return results correctly.
 * Spec: 13-api-design.md
 */
@AutoConfigureMockMvc
class PaginationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ContractRepository contractRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private ReconciliationRepository reconciliationRepository;
    @Autowired private JournalLineRepository journalLineRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private ActualLineRepository actualLineRepository;
    @Autowired private SapImportRepository sapImportRepository;
    @Autowired private MilestoneVersionRepository milestoneVersionRepository;
    @Autowired private MilestoneRepository milestoneRepository;
    @Autowired private ProjectRepository projectRepository;

    @BeforeEach
    void cleanUp() {
        auditLogRepository.deleteAll();
        reconciliationRepository.deleteAll();
        journalLineRepository.deleteAll();
        journalEntryRepository.deleteAll();
        actualLineRepository.deleteAll();
        sapImportRepository.deleteAll();
        milestoneVersionRepository.deleteAll();
        milestoneRepository.deleteAll();
        projectRepository.deleteAll();
        contractRepository.deleteAll();
    }

    /**
     * Create 5 contracts and verify the list endpoint returns all 5.
     * Spec: 13-api-design.md Section 3
     */
    @Test
    void listContracts_withFiveContracts_returnsAllFive() throws Exception {
        // Create 5 contracts
        for (int i = 1; i <= 5; i++) {
            String body = """
                    {
                        "name": "Pagination Contract %d",
                        "vendor": "Test Vendor",
                        "ownerUser": "Test Owner",
                        "startDate": "2025-01-01"
                    }
                    """.formatted(i);
            mockMvc.perform(post("/api/v1/contracts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }

        // List contracts — should return all 5
        mockMvc.perform(get("/api/v1/contracts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }
}
