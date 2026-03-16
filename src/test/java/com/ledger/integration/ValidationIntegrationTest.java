package com.ledger.integration;

import com.ledger.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for ARCH-06: Bean Validation constraints on request DTOs.
 */
@AutoConfigureMockMvc
class ValidationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void create_contract_with_missing_required_fields_returns_400() throws Exception {
        mockMvc.perform(post("/api/v1/contracts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vendor\":\"Acme\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_reconciliation_with_missing_actualId_returns_400() throws Exception {
        mockMvc.perform(post("/api/v1/reconciliation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"milestoneId\":\"00000000-0000-0000-0000-000000000001\",\"category\":\"INVOICE\"}"))
                .andExpect(status().isBadRequest());
    }
}
