package com.ledger.integration;

import com.ledger.BaseIntegrationTest;
import com.ledger.repository.AuditLogRepository;
import com.ledger.repository.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Contract API.
 * Spec refs: 01-domain-model.md (Section 2.3), 11-change-management.md, 13-api-design.md (Section 3)
 */
@AutoConfigureMockMvc
class ContractIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void cleanUp() {
        auditLogRepository.deleteAll();
        contractRepository.deleteAll();
    }

    /**
     * T04-1: Create contract.
     * Spec: 01-domain-model.md Section 2.3, 13-api-design.md Section 3
     *
     * GIVEN a valid contract request
     * WHEN  POST /api/v1/contracts
     * THEN  returns 201 with contract_id, status = ACTIVE
     */
    @Test
    void createContract_validRequest_returns201WithActiveStatus() throws Exception {
        String requestBody = """
                {
                    "name": "Globant ADM",
                    "vendor": "Globant",
                    "description": "ADM services contract",
                    "ownerUser": "Rob Moore",
                    "startDate": "2025-10-01",
                    "endDate": "2026-09-30"
                }
                """;

        mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contractId", notNullValue()))
                .andExpect(jsonPath("$.name", is("Globant ADM")))
                .andExpect(jsonPath("$.vendor", is("Globant")))
                .andExpect(jsonPath("$.description", is("ADM services contract")))
                .andExpect(jsonPath("$.ownerUser", is("Rob Moore")))
                .andExpect(jsonPath("$.startDate", is("2025-10-01")))
                .andExpect(jsonPath("$.endDate", is("2026-09-30")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.createdBy", is("system")));
    }

    /**
     * T04-2: Create contract - name uniqueness.
     * Spec: 01-domain-model.md Section 2.3 (name UNIQUE constraint), 13-api-design.md Section 3
     *
     * GIVEN a contract named "Globant ADM" already exists
     * WHEN  POST /api/v1/contracts with name "Globant ADM"
     * THEN  returns 409 Conflict
     */
    @Test
    void createContract_duplicateName_returns409Conflict() throws Exception {
        String requestBody = """
                {
                    "name": "Globant ADM",
                    "vendor": "Globant",
                    "description": "ADM services contract",
                    "ownerUser": "Rob Moore",
                    "startDate": "2025-10-01",
                    "endDate": "2026-09-30"
                }
                """;

        // Create first contract
        mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        // Attempt to create duplicate
        mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Globant ADM")));
    }

    /**
     * T04-3: Update contract requires reason.
     * Spec: 11-change-management.md Section 4 (required reasons), 13-api-design.md Section 3
     *
     * GIVEN an existing contract
     * WHEN  PUT /api/v1/contracts/{id} without reason field
     * THEN  returns 400 Bad Request
     */
    @Test
    void updateContract_missingReason_returns400BadRequest() throws Exception {
        // Create a contract first
        String createBody = """
                {
                    "name": "Test Contract",
                    "vendor": "Test Vendor",
                    "ownerUser": "Rob",
                    "startDate": "2025-10-01"
                }
                """;
        MvcResult createResult = mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();

        String contractId = com.jayway.jsonpath.JsonPath.read(
                createResult.getResponse().getContentAsString(), "$.contractId");

        // Attempt update without reason
        String updateBody = """
                {
                    "name": "Test Contract",
                    "vendor": "Test Vendor",
                    "ownerUser": "Brad",
                    "startDate": "2025-10-01"
                }
                """;

        mockMvc.perform(put("/api/v1/contracts/" + contractId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isBadRequest());
    }

    /**
     * T04-4: Update contract creates audit log entry.
     * Spec: 11-change-management.md Sections 2.2 and 3 (audit log entity)
     *
     * GIVEN an existing contract with owner "Rob"
     * WHEN  PUT /api/v1/contracts/{id} changing owner to "Brad" with reason
     * THEN  audit_log has entry with entity_type=CONTRACT, action=UPDATE
     * AND   changes JSON shows {"ownerUser": {"old": "Rob", "new": "Brad"}}
     */
    @Test
    void updateContract_changesOwner_createsAuditLogEntry() throws Exception {
        // Create a contract
        String createBody = """
                {
                    "name": "Audit Test Contract",
                    "vendor": "Test Vendor",
                    "ownerUser": "Rob",
                    "startDate": "2025-10-01"
                }
                """;
        MvcResult createResult = mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();

        String contractId = com.jayway.jsonpath.JsonPath.read(
                createResult.getResponse().getContentAsString(), "$.contractId");

        // Update with reason
        String updateBody = """
                {
                    "name": "Audit Test Contract",
                    "vendor": "Test Vendor",
                    "ownerUser": "Brad",
                    "startDate": "2025-10-01",
                    "status": "ACTIVE",
                    "reason": "Ownership transfer"
                }
                """;

        mockMvc.perform(put("/api/v1/contracts/" + contractId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk());

        // Verify audit log
        var auditLogs = auditLogRepository.findByEntityTypeAndEntityId("CONTRACT", contractId);
        // Should have CREATE + UPDATE entries
        var updateLogs = auditLogs.stream()
                .filter(a -> "UPDATE".equals(a.getAction()))
                .toList();
        assertThat(updateLogs).hasSize(1);

        var updateLog = updateLogs.get(0);
        assertThat(updateLog.getEntityType()).isEqualTo("CONTRACT");
        assertThat(updateLog.getAction()).isEqualTo("UPDATE");
        assertThat(updateLog.getReason()).isEqualTo("Ownership transfer");

        var changes = updateLog.getChanges();
        assertThat(changes).containsKey("ownerUser");
        assertThat(changes.get("ownerUser").get("old")).isEqualTo("Rob");
        assertThat(changes.get("ownerUser").get("new")).isEqualTo("Brad");
    }

    /**
     * T04-5: List contracts filters by status.
     * Spec: 13-api-design.md Section 3 (GET /contracts with status filter)
     *
     * GIVEN 2 ACTIVE and 1 CLOSED contract
     * WHEN  GET /api/v1/contracts?status=ACTIVE
     * THEN  returns 2 contracts
     */
    @Test
    void listContracts_filterByActiveStatus_returns2Contracts() throws Exception {
        // Create 3 contracts
        createContract("Contract A", "Vendor A", "Owner A", "2025-10-01", null);
        createContract("Contract B", "Vendor B", "Owner B", "2025-10-01", null);
        String closedId = createContract("Contract C", "Vendor C", "Owner C", "2025-10-01", null);

        // Close Contract C via update
        String closeBody = """
                {
                    "name": "Contract C",
                    "vendor": "Vendor C",
                    "ownerUser": "Owner C",
                    "startDate": "2025-10-01",
                    "status": "CLOSED",
                    "reason": "Contract completed"
                }
                """;
        mockMvc.perform(put("/api/v1/contracts/" + closedId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(closeBody))
                .andExpect(status().isOk());

        // Filter by ACTIVE
        mockMvc.perform(get("/api/v1/contracts")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    /**
     * T04-6: Contract end_date must be after start_date.
     * Spec: 01-domain-model.md Section 2.3 (end_date after start_date constraint)
     *
     * GIVEN a contract request with end_date before start_date
     * WHEN  POST /api/v1/contracts
     * THEN  returns 400 Bad Request
     */
    @Test
    void createContract_endDateBeforeStartDate_returns400BadRequest() throws Exception {
        String requestBody = """
                {
                    "name": "Bad Date Contract",
                    "vendor": "Test Vendor",
                    "ownerUser": "Rob",
                    "startDate": "2026-09-30",
                    "endDate": "2025-10-01"
                }
                """;

        mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // Helper method to create a contract and return its ID
    private String createContract(String name, String vendor, String owner,
                                   String startDate, String endDate) throws Exception {
        String endDateField = endDate != null ? ", \"endDate\": \"" + endDate + "\"" : "";
        String body = """
                {
                    "name": "%s",
                    "vendor": "%s",
                    "ownerUser": "%s",
                    "startDate": "%s"%s
                }
                """.formatted(name, vendor, owner, startDate, endDateField);

        MvcResult result = mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.contractId");
    }
}
