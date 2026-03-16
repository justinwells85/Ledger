package com.ledger.config;

import com.ledger.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T40 — Settings page: data-driven from system_config metadata.
 * Spec: 18-admin-configuration.md Section 4, BR-98 through BR-101
 */
@AutoConfigureMockMvc
class SystemConfigMetadataTest extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void config_response_includes_display_group_and_display_name() throws Exception {
        // Spec: 18-admin-configuration.md Section 4.3
        mockMvc.perform(get("/api/v1/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.configKey == 'tolerance_percent')].displayGroup",
                        hasItem("RECONCILIATION TOLERANCE")))
                .andExpect(jsonPath("$[?(@.configKey == 'tolerance_percent')].displayName",
                        hasItem("Tolerance (%)")))
                .andExpect(jsonPath("$[?(@.configKey == 'tolerance_percent')].dataType",
                        hasItem("PERCENTAGE")));
    }

    @Test
    void config_response_includes_accrual_aging_group() throws Exception {
        // Spec: 18-admin-configuration.md Section 4.3
        mockMvc.perform(get("/api/v1/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.configKey == 'accrual_aging_warning_days')].displayGroup",
                        hasItem("ACCRUAL AGING")))
                .andExpect(jsonPath("$[?(@.configKey == 'accrual_aging_warning_days')].displayOrder",
                        hasItem(1)));
    }
}
