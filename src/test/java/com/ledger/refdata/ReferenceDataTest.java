package com.ledger.refdata;

import com.ledger.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T39 — Reference data management: list, create, toggle-active.
 * Spec: 18-admin-configuration.md Section 3, BR-91 through BR-97
 */
@AutoConfigureMockMvc
class ReferenceDataTest extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void list_funding_sources_returns_seeded_data() throws Exception {
        // Spec: 18-admin-configuration.md Section 3.2
        mockMvc.perform(get("/api/v1/admin/reference-data/FUNDING_SOURCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[*].code", hasItem("OPEX")))
                .andExpect(jsonPath("$[*].code", hasItem("CAPEX")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_funding_source_returns_201() throws Exception {
        // Spec: 18-admin-configuration.md Section 3.2, BR-91
        mockMvc.perform(post("/api/v1/admin/reference-data/FUNDING_SOURCE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"GRANT\",\"displayName\":\"Grant\",\"description\":\"External grant funding\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("GRANT"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void toggle_active_on_funding_source_deactivates_it() throws Exception {
        // Spec: 18-admin-configuration.md Section 3.2, BR-92
        mockMvc.perform(post("/api/v1/admin/reference-data/FUNDING_SOURCE/OTHER_TEAM/toggle-active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cannot_deactivate_ACTIVE_contract_status() throws Exception {
        // Spec: 18-admin-configuration.md Section 3.3, BR-94
        mockMvc.perform(post("/api/v1/admin/reference-data/CONTRACT_STATUS/ACTIVE/toggle-active"))
                .andExpect(status().isConflict());
    }

    @Test
    void non_admin_cannot_create_reference_data() throws Exception {
        // Spec: 18-admin-configuration.md BR-82
        // BaseIntegrationTest uses @WithMockUser(username = "system") with USER role
        mockMvc.perform(post("/api/v1/admin/reference-data/FUNDING_SOURCE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"GRANT2\",\"displayName\":\"Grant\",\"description\":\"\"}"))
                .andExpect(status().isForbidden());
    }
}
