package com.ledger.sap;

import com.ledger.BaseIntegrationTest;
import com.ledger.repository.ActualLineRepository;
import com.ledger.repository.SapImportRepository;
import com.ledger.service.SapImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T12 — SAP Import: Dedup + Stage
 * Spec: 05-sap-ingestion.md Steps 3-4, BR-08, BR-09
 * Tests: T12-1 through T12-6
 */
@AutoConfigureMockMvc
class SapImportDedupTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SapImportRepository sapImportRepository;

    @Autowired
    private ActualLineRepository actualLineRepository;

    @Autowired
    private SapImportService sapImportService;

    @BeforeEach
    void setUp() {
        actualLineRepository.deleteAll();
        sapImportRepository.deleteAll();
    }

    private static final String CSV_HEADER =
            "Document Number,Posting Date,Amount,Vendor Name,Cost Center,WBS Element,GL Account,Description\n";

    private String makeRow(String doc, String date, String amount, String vendor, String desc) {
        return String.format("%s,%s,%s,%s,CC001,1174905.SU.ES,500000,%s\n", doc, date, amount, vendor, desc);
    }

    private byte[] buildCsv(int count) {
        StringBuilder sb = new StringBuilder(CSV_HEADER);
        for (int i = 1; i <= count; i++) {
            sb.append(makeRow("DOC" + i, "2026-01-15", String.valueOf(1000 * i), "Globant", "Monthly #" + i));
        }
        return sb.toString().getBytes();
    }

    // TEST T12-1: First import — all lines are new
    // Spec: 05-sap-ingestion.md Step 3, BR-08
    @Test
    void firstImport_allLinesAreNew() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "first.csv", "text/csv", buildCsv(10));

        mockMvc.perform(multipart("/api/v1/imports/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newLines", is(10)))
                .andExpect(jsonPath("$.duplicateLines", is(0)));
    }

    // TEST T12-2: Re-import same file — all lines are duplicates
    // Spec: 05-sap-ingestion.md Step 3, BR-08
    @Test
    void reimportSameFile_allDuplicates() throws Exception {
        byte[] csv = buildCsv(10);
        MockMultipartFile first = new MockMultipartFile("file", "sap.csv", "text/csv", csv);
        mockMvc.perform(multipart("/api/v1/imports/upload").file(first))
                .andExpect(status().isCreated());

        MockMultipartFile second = new MockMultipartFile("file", "sap.csv", "text/csv", csv);
        mockMvc.perform(multipart("/api/v1/imports/upload").file(second))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newLines", is(0)))
                .andExpect(jsonPath("$.duplicateLines", is(10)));
    }

    // TEST T12-3: Partial overlap — 10 old + 5 new
    // Spec: 05-sap-ingestion.md Step 3, BR-08
    @Test
    void partialOverlap_correctCounts() throws Exception {
        // First upload: 10 lines
        MockMultipartFile first = new MockMultipartFile("file", "first.csv", "text/csv", buildCsv(10));
        mockMvc.perform(multipart("/api/v1/imports/upload").file(first))
                .andExpect(status().isCreated());

        // Second upload: 15 lines (same first 10 + 5 new)
        MockMultipartFile second = new MockMultipartFile("file", "second.csv", "text/csv", buildCsv(15));
        mockMvc.perform(multipart("/api/v1/imports/upload").file(second))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalLines", is(15)))
                .andExpect(jsonPath("$.newLines", is(5)))
                .andExpect(jsonPath("$.duplicateLines", is(10)));
    }

    // TEST T12-4: Hash normalization — whitespace differences are deduped
    // Spec: 05-sap-ingestion.md Section 3 (Normalization rules)
    @Test
    void hashNormalization_whitespaceDifference_detectsAsDuplicate() throws Exception {
        // Upload line with vendor "Globant S.A."
        String csv1 = CSV_HEADER +
                makeRow("DOC1", "2026-01-15", "25000.00", "Globant S.A.", "Invoice");
        MockMultipartFile first = new MockMultipartFile("file", "first.csv", "text/csv", csv1.getBytes());
        mockMvc.perform(multipart("/api/v1/imports/upload").file(first))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newLines", is(1)));

        // Upload same line with extra whitespace around vendor
        String csv2 = CSV_HEADER +
                makeRow("DOC1", "2026-01-15", "25000.00", " Globant S.A. ", "Invoice");
        MockMultipartFile second = new MockMultipartFile("file", "second.csv", "text/csv", csv2.getBytes());
        mockMvc.perform(multipart("/api/v1/imports/upload").file(second))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duplicateLines", is(1)));
    }

    // TEST T12-5: Hash normalization — case differences are deduped
    // Spec: 05-sap-ingestion.md Section 3 (Normalization rules)
    @Test
    void hashNormalization_caseDifference_detectsAsDuplicate() throws Exception {
        String csv1 = CSV_HEADER +
                makeRow("DOC1", "2026-01-15", "25000.00", "Globant", "Invoice #123");
        MockMultipartFile first = new MockMultipartFile("file", "first.csv", "text/csv", csv1.getBytes());
        mockMvc.perform(multipart("/api/v1/imports/upload").file(first))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newLines", is(1)));

        String csv2 = CSV_HEADER +
                makeRow("DOC1", "2026-01-15", "25000.00", "GLOBANT", "INVOICE #123");
        MockMultipartFile second = new MockMultipartFile("file", "second.csv", "text/csv", csv2.getBytes());
        mockMvc.perform(multipart("/api/v1/imports/upload").file(second))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duplicateLines", is(1)));
    }

    // TEST T12-6: Different amount = different hash = new line (not duplicate)
    // Spec: 05-sap-ingestion.md Section 3 dedup edge cases
    @Test
    void differentAmount_isDifferentLine_notDuplicate() throws Exception {
        String csv1 = CSV_HEADER +
                makeRow("DOC1", "2026-01-15", "25000.00", "Globant", "Invoice");
        MockMultipartFile first = new MockMultipartFile("file", "first.csv", "text/csv", csv1.getBytes());
        mockMvc.perform(multipart("/api/v1/imports/upload").file(first))
                .andExpect(status().isCreated());

        String csv2 = CSV_HEADER +
                makeRow("DOC1", "2026-01-15", "25001.00", "Globant", "Invoice");  // different amount
        MockMultipartFile second = new MockMultipartFile("file", "second.csv", "text/csv", csv2.getBytes());
        mockMvc.perform(multipart("/api/v1/imports/upload").file(second))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newLines", is(1)))
                .andExpect(jsonPath("$.duplicateLines", is(0)));
    }
}
