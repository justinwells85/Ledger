package com.ledger.integration;

import com.ledger.BaseIntegrationTest;
import com.ledger.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Project API.
 * Spec refs: 01-domain-model.md (Section 2.4), 13-api-design.md (Section 4)
 */
@AutoConfigureMockMvc
class ProjectIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired private ProjectRepository projectRepository;
    @Autowired private ContractRepository contractRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private ReconciliationRepository reconciliationRepository;
    @Autowired private JournalLineRepository journalLineRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private ActualLineRepository actualLineRepository;
    @Autowired private SapImportRepository sapImportRepository;
    @Autowired private MilestoneVersionRepository milestoneVersionRepository;
    @Autowired private MilestoneRepository milestoneRepository;

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
     * T05-1: Create project under contract.
     * Spec: 01-domain-model.md Section 2.4, 13-api-design.md Section 4
     *
     * GIVEN an existing contract
     * WHEN  POST /api/v1/contracts/{id}/projects with valid project data
     * THEN  returns 201 with project_id, correct contract_id and funding_source
     */
    @Test
    void createProject_validRequest_returns201WithCorrectFields() throws Exception {
        String contractId = createContract("Test Contract", "Test Vendor", "Owner", "2025-10-01");

        String projectBody = """
                {
                    "projectId": "PR13752",
                    "wbse": "1174905.SU.ES",
                    "name": "DPI - Photopass - SUS Break/Fix",
                    "fundingSource": "OPEX"
                }
                """;

        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId", is("PR13752")))
                .andExpect(jsonPath("$.contractId", is(contractId)))
                .andExpect(jsonPath("$.wbse", is("1174905.SU.ES")))
                .andExpect(jsonPath("$.name", is("DPI - Photopass - SUS Break/Fix")))
                .andExpect(jsonPath("$.fundingSource", is("OPEX")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.createdBy", is("system")));
    }

    /**
     * T05-2: Project ID uniqueness.
     * Spec: 01-domain-model.md Section 2.4 (project_id unique constraint)
     *
     * GIVEN project PR13752 already exists
     * WHEN  POST with project_id = PR13752 under a different contract
     * THEN  returns 409 Conflict
     */
    @Test
    void createProject_duplicateProjectId_returns409Conflict() throws Exception {
        String contractId1 = createContract("Contract A", "Vendor A", "Owner A", "2025-10-01");
        String contractId2 = createContract("Contract B", "Vendor B", "Owner B", "2025-10-01");

        String projectBody = """
                {
                    "projectId": "PR13752",
                    "wbse": "1174905.SU.ES",
                    "name": "Project Alpha",
                    "fundingSource": "OPEX"
                }
                """;

        // Create first project
        mockMvc.perform(post("/api/v1/contracts/" + contractId1 + "/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody))
                .andExpect(status().isCreated());

        // Attempt duplicate project_id under different contract
        String duplicateBody = """
                {
                    "projectId": "PR13752",
                    "wbse": "9999999.XX.YY",
                    "name": "Project Beta",
                    "fundingSource": "CAPEX"
                }
                """;

        mockMvc.perform(post("/api/v1/contracts/" + contractId2 + "/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("PR13752")));
    }

    /**
     * T05-3: WBSE + Project ID uniqueness.
     * Spec: 01-domain-model.md Section 2.4 (WBSE + Project ID unique constraint)
     *
     * GIVEN project with wbse "1174905.SU.ES" and id "PR13752" exists
     * WHEN  POST with same wbse and project_id
     * THEN  returns 409 Conflict
     */
    @Test
    void createProject_duplicateWbseAndProjectId_returns409Conflict() throws Exception {
        String contractId = createContract("Test Contract", "Test Vendor", "Owner", "2025-10-01");

        String projectBody = """
                {
                    "projectId": "PR13752",
                    "wbse": "1174905.SU.ES",
                    "name": "Project Alpha",
                    "fundingSource": "OPEX"
                }
                """;

        // Create first project
        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody))
                .andExpect(status().isCreated());

        // Attempt duplicate with same wbse + project_id
        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("PR13752")));
    }

    /**
     * T05-4: List projects for contract.
     * Spec: 13-api-design.md Section 4 (GET /contracts/{id}/projects)
     *
     * GIVEN contract with 3 projects
     * WHEN  GET /api/v1/contracts/{id}/projects
     * THEN  returns 3 projects
     */
    @Test
    void listProjects_contractWith3Projects_returns3Projects() throws Exception {
        String contractId = createContract("Test Contract", "Test Vendor", "Owner", "2025-10-01");

        createProject(contractId, "PR00001", "1111111.AA.BB", "Project 1", "OPEX");
        createProject(contractId, "PR00002", "2222222.CC.DD", "Project 2", "CAPEX");
        createProject(contractId, "PR00003", "3333333.EE.FF", "Project 3", "OPEX");

        mockMvc.perform(get("/api/v1/contracts/" + contractId + "/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    /**
     * T05-5: Filter projects by funding source.
     * Spec: 13-api-design.md Section 4 (GET /contracts/{id}/projects?fundingSource=)
     *
     * GIVEN 2 OPEX projects and 1 CAPEX project under same contract
     * WHEN  GET /api/v1/contracts/{id}/projects?fundingSource=CAPEX
     * THEN  returns 1 project
     */
    @Test
    void listProjects_filterByCapex_returns1Project() throws Exception {
        String contractId = createContract("Test Contract", "Test Vendor", "Owner", "2025-10-01");

        createProject(contractId, "PR00001", "1111111.AA.BB", "Project 1", "OPEX");
        createProject(contractId, "PR00002", "2222222.CC.DD", "Project 2", "OPEX");
        createProject(contractId, "PR00003", "3333333.EE.FF", "Project 3", "CAPEX");

        mockMvc.perform(get("/api/v1/contracts/" + contractId + "/projects")
                        .param("fundingSource", "CAPEX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fundingSource", is("CAPEX")));
    }

    /**
     * T05-6: Funding source must be valid enum.
     * Spec: 01-domain-model.md Section 2.4, 13-api-design.md Section 4
     *
     * GIVEN a project create request with fundingSource = "INVALID"
     * WHEN  POST /api/v1/contracts/{id}/projects
     * THEN  returns 400 Bad Request
     */
    @Test
    void createProject_invalidFundingSource_returns400BadRequest() throws Exception {
        String contractId = createContract("Test Contract", "Test Vendor", "Owner", "2025-10-01");

        String projectBody = """
                {
                    "projectId": "PR99999",
                    "wbse": "9999999.XX.YY",
                    "name": "Invalid Funding Project",
                    "fundingSource": "INVALID"
                }
                """;

        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid funding source")));
    }

    // Helper: create a contract and return its ID
    private String createContract(String name, String vendor, String owner, String startDate) throws Exception {
        String body = """
                {
                    "name": "%s",
                    "vendor": "%s",
                    "ownerUser": "%s",
                    "startDate": "%s"
                }
                """.formatted(name, vendor, owner, startDate);

        MvcResult result = mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.contractId");
    }

    // Helper: create a project under a contract
    private void createProject(String contractId, String projectId, String wbse,
                                String name, String fundingSource) throws Exception {
        String body = """
                {
                    "projectId": "%s",
                    "wbse": "%s",
                    "name": "%s",
                    "fundingSource": "%s"
                }
                """.formatted(projectId, wbse, name, fundingSource);

        mockMvc.perform(post("/api/v1/contracts/" + contractId + "/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }
}
