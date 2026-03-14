package com.ledger.integration;

import com.ledger.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Fiscal Calendar API.
 * Spec refs: 01-domain-model.md (Sections 2.1, 2.2), 03-fiscal-calendar.md, 13-api-design.md (Section 2)
 *
 * Seed data (V008) provides FY25, FY26, FY27 with 12 periods each.
 */
@AutoConfigureMockMvc
class FiscalCalendarIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * T03-1: List fiscal years returns all years.
     * Spec: 03-fiscal-calendar.md Section 3 (multi-year support), 13-api-design.md Section 2
     *
     * GIVEN seed data with FY25, FY26, FY27
     * WHEN  GET /api/v1/fiscal-years
     * THEN  response contains 3 fiscal years ordered by start_date
     */
    @Test
    void listFiscalYears_returnsAllYearsOrderedByStartDate() throws Exception {
        mockMvc.perform(get("/api/v1/fiscal-years"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].fiscalYear", is("FY25")))
                .andExpect(jsonPath("$[0].startDate", is("2024-10-01")))
                .andExpect(jsonPath("$[0].endDate", is("2025-09-30")))
                .andExpect(jsonPath("$[1].fiscalYear", is("FY26")))
                .andExpect(jsonPath("$[2].fiscalYear", is("FY27")));
    }

    /**
     * T03-2: List periods for FY26 returns 12 periods.
     * Spec: 01-domain-model.md Section 2.2, 03-fiscal-calendar.md Section 4
     *
     * GIVEN FY26 seed data
     * WHEN  GET /api/v1/fiscal-years/FY26/periods
     * THEN  response contains 12 periods sorted by sort_order 1-12
     * AND   Q1 contains Oct, Nov, Dec
     */
    @Test
    void listPeriodsForFY26_returns12PeriodsSortedBySortOrder() throws Exception {
        mockMvc.perform(get("/api/v1/fiscal-years/FY26/periods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)))
                // Verify sort_order 1-12
                .andExpect(jsonPath("$[0].sortOrder", is(1)))
                .andExpect(jsonPath("$[11].sortOrder", is(12)))
                // Q1 = Oct, Nov, Dec
                .andExpect(jsonPath("$[0].quarter", is("Q1")))
                .andExpect(jsonPath("$[0].displayName", is("October 2025")))
                .andExpect(jsonPath("$[0].calendarMonth", is("2025-10")))
                .andExpect(jsonPath("$[1].quarter", is("Q1")))
                .andExpect(jsonPath("$[1].displayName", is("November 2025")))
                .andExpect(jsonPath("$[2].quarter", is("Q1")))
                .andExpect(jsonPath("$[2].displayName", is("December 2025")))
                // Verify fiscal year is set on each period
                .andExpect(jsonPath("$[0].fiscalYear", is("FY26")))
                .andExpect(jsonPath("$[0].periodKey", is("FY26-01-OCT")))
                // Verify period ID is present
                .andExpect(jsonPath("$[0].periodId", notNullValue()));
    }

    /**
     * T03-3: Resolve posting date to fiscal period.
     * Spec: 03-fiscal-calendar.md Section 6 (posting date resolution)
     *
     * GIVEN FY26 seed data
     * WHEN  resolving posting date 2026-01-15
     * THEN  returns FY26 Q2 January period
     */
    @Test
    void resolvePeriod_januaryDate_returnsFY26Q2January() throws Exception {
        mockMvc.perform(get("/api/v1/fiscal-years/resolve-period")
                        .param("postingDate", "2026-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiscalYear", is("FY26")))
                .andExpect(jsonPath("$.quarter", is("Q2")))
                .andExpect(jsonPath("$.displayName", is("January 2026")))
                .andExpect(jsonPath("$.calendarMonth", is("2026-01")))
                .andExpect(jsonPath("$.sortOrder", is(4)));
    }

    /**
     * T03-4: Resolve October date maps to correct FY.
     * Spec: 03-fiscal-calendar.md Section 6 (edge case: October maps to next FY)
     *
     * GIVEN FY26 seed data
     * WHEN  resolving posting date 2025-10-15
     * THEN  returns FY26 Q1 October period (not FY25)
     */
    @Test
    void resolvePeriod_octoberDate_returnsFY26Q1October() throws Exception {
        mockMvc.perform(get("/api/v1/fiscal-years/resolve-period")
                        .param("postingDate", "2025-10-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiscalYear", is("FY26")))
                .andExpect(jsonPath("$.quarter", is("Q1")))
                .andExpect(jsonPath("$.displayName", is("October 2025")))
                .andExpect(jsonPath("$.calendarMonth", is("2025-10")))
                .andExpect(jsonPath("$.sortOrder", is(1)));
    }

    /**
     * T03-5: Resolve date with no matching FY returns empty.
     * Spec: 03-fiscal-calendar.md Section 6 (no matching period)
     *
     * GIVEN only FY25-FY27 exist
     * WHEN  resolving posting date 2028-01-15
     * THEN  returns 404 (no matching period)
     */
    @Test
    void resolvePeriod_noMatchingFY_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/fiscal-years/resolve-period")
                        .param("postingDate", "2028-01-15"))
                .andExpect(status().isNotFound());
    }
}
