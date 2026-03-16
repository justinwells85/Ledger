package com.ledger.fiscal;

import com.ledger.BaseIntegrationTest;
import com.ledger.repository.FiscalPeriodRepository;
import com.ledger.repository.FiscalYearRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for fiscal year creation via the admin API.
 * Spec: 18-admin-configuration.md Section 2, BR-85 through BR-90
 */
@AutoConfigureMockMvc
class FiscalYearCreationTest extends BaseIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    FiscalYearRepository fiscalYearRepository;

    @Autowired
    FiscalPeriodRepository fiscalPeriodRepository;

    @AfterEach
    void cleanUp() {
        // Delete FY28 if created by a test (periods first due to FK)
        var periods = fiscalPeriodRepository.findByFiscalYearFiscalYearOrderBySortOrderAsc("FY28");
        fiscalPeriodRepository.deleteAll(periods);
        fiscalYearRepository.findById("FY28").ifPresent(fiscalYearRepository::delete);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void creating_FY28_generates_fiscal_year_with_correct_dates() throws Exception {
        // Spec: 18-admin-configuration.md Section 2, BR-85, BR-86
        mockMvc.perform(post("/api/v1/fiscal-years")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"FY28\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fiscalYear").value("FY28"))
                .andExpect(jsonPath("$.startDate").value("2027-10-01"))
                .andExpect(jsonPath("$.endDate").value("2028-09-30"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void creating_FY28_generates_12_periods_with_correct_keys() throws Exception {
        // Spec: 18-admin-configuration.md Section 2, BR-87, BR-88
        mockMvc.perform(post("/api/v1/fiscal-years")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"FY28\"}"))
                .andExpect(status().isCreated());

        var periods = fiscalPeriodRepository.findByFiscalYearFiscalYearOrderBySortOrderAsc("FY28");
        assertThat(periods).hasSize(12);
        assertThat(periods.get(0).getPeriodKey()).isEqualTo("FY28-01-OCT");
        assertThat(periods.get(0).getCalendarMonth().toString()).isEqualTo("2027-10-01");
        assertThat(periods.get(0).getQuarter()).isEqualTo("Q1");
        assertThat(periods.get(11).getPeriodKey()).isEqualTo("FY28-12-SEP");
        assertThat(periods.get(11).getCalendarMonth().toString()).isEqualTo("2028-09-01");
        assertThat(periods.get(11).getQuarter()).isEqualTo("Q4");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void creating_duplicate_fiscal_year_returns_409() throws Exception {
        // Spec: 18-admin-configuration.md, BR-85 (FY27 already exists in seed data)
        mockMvc.perform(post("/api/v1/fiscal-years")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"FY27\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void invalid_label_format_returns_400() throws Exception {
        // Spec: 18-admin-configuration.md, BR-85 — must be FY## (exactly 2 digits)
        mockMvc.perform(post("/api/v1/fiscal-years")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"FY2028\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void non_sequential_fiscal_year_returns_409() throws Exception {
        // Spec: 18-admin-configuration.md, BR-90 — FY25/FY26/FY27 exist, FY29 skips FY28
        mockMvc.perform(post("/api/v1/fiscal-years")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"FY29\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void non_admin_creating_fiscal_year_returns_403() throws Exception {
        // Spec: 18-admin-configuration.md, BR-82
        // BaseIntegrationTest uses @WithMockUser(username = "system") with default USER role
        mockMvc.perform(post("/api/v1/fiscal-years")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"FY28\"}"))
                .andExpect(status().isForbidden());
    }
}
