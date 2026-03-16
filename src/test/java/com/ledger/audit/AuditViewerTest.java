package com.ledger.audit;

import com.ledger.BaseIntegrationTest;
import com.ledger.entity.AuditLog;
import com.ledger.repository.AuditLogRepository;
import com.ledger.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T41 — Audit log viewer: filtered query and CSV export.
 * Spec: 18-admin-configuration.md Section 5
 */
@AutoConfigureMockMvc
class AuditViewerTest extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired AuditService auditService;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();

        auditService.log("CONTRACT", "c-001", "CREATE", null, "Contract created", "alice");
        auditService.log("CONTRACT", "c-001", "UPDATE",
                Map.of("name", Map.of("before", "Old Name", "after", "New Name")),
                "Name changed", "alice");
        auditService.log("PROJECT", "PR13752", "UPDATE",
                Map.of("status", Map.of("before", "ACTIVE", "after", "CLOSED")),
                "Project closed", "bob");
        auditService.log("CONFIGURATION", "tolerance_percent", "UPDATE",
                Map.of("value", Map.of("before", "0.02", "after", "0.03")),
                "Tolerance increased", "bob");
    }

    @Test
    void get_audit_no_filters_returns_all_entries() throws Exception {
        // Spec: 18-admin-configuration.md Section 5
        mockMvc.perform(get("/api/v1/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void get_audit_filtered_by_entityType_returns_matching_entries() throws Exception {
        // Spec: 18-admin-configuration.md Section 5
        mockMvc.perform(get("/api/v1/audit").param("entityType", "CONTRACT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].entityType", everyItem(equalTo("CONTRACT"))));
    }

    @Test
    void get_audit_filtered_by_createdBy_returns_matching_entries() throws Exception {
        // Spec: 18-admin-configuration.md Section 5
        mockMvc.perform(get("/api/v1/audit").param("createdBy", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].createdBy", everyItem(equalTo("bob"))));
    }

    @Test
    void get_audit_export_csv_returns_csv_content_type() throws Exception {
        // Spec: 18-admin-configuration.md Section 5 — Export
        mockMvc.perform(get("/api/v1/audit/export.csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    void get_audit_export_csv_contains_header_and_rows() throws Exception {
        // Spec: 18-admin-configuration.md Section 5 — Export
        mockMvc.perform(get("/api/v1/audit/export.csv"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("timestamp,entityType,entityId,action,createdBy,reason")))
                .andExpect(content().string(containsString("alice")))
                .andExpect(content().string(containsString("CONTRACT")));
    }
}
