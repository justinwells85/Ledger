package com.ledger.sap;

import com.ledger.BaseIntegrationTest;
import com.ledger.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T11 — SAP Import: File Upload + Parse
 * Spec: 05-sap-ingestion.md Steps 1-2
 * Tests: T11-1 through T11-5
 */
@AutoConfigureMockMvc
class SapImportUploadTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SapImportRepository sapImportRepository;

    @Autowired
    private ActualLineRepository actualLineRepository;

    @Autowired
    private ReconciliationRepository reconciliationRepository;

    @Autowired
    private JournalLineRepository journalLineRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @BeforeEach
    void setUp() {
        reconciliationRepository.deleteAll();
        journalLineRepository.deleteAll();
        journalEntryRepository.deleteAll();
        actualLineRepository.deleteAll();
        sapImportRepository.deleteAll();
    }

    private static final String CSV_HEADER =
            "Document Number,Posting Date,Amount,Vendor Name,Cost Center,WBS Element,GL Account,Description\n";

    private String csvRow(String docNum, String date, String amount, String vendor) {
        return String.format("%s,%s,%s,%s,CC001,1174905.SU.ES,500000,Monthly sustainment\n",
                docNum, date, amount, vendor);
    }

    // TEST T11-1: Upload CSV file with 10 lines — parses and stages all
    // Spec: 05-sap-ingestion.md Steps 1-2
    @Test
    void uploadCsv_tenLines_returns201WithTotalLines() throws Exception {
        StringBuilder csv = new StringBuilder(CSV_HEADER);
        for (int i = 1; i <= 10; i++) {
            csv.append(csvRow("DOC" + i, "2026-01-15", "1000.00", "Globant"));
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "SAP_FY26_JAN.csv", "text/csv", csv.toString().getBytes());

        mockMvc.perform(multipart("/api/v1/imports/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importId").isNotEmpty())
                .andExpect(jsonPath("$.filename").value("SAP_FY26_JAN.csv"))
                .andExpect(jsonPath("$.status").value("STAGED"))
                .andExpect(jsonPath("$.totalLines").value(10))
                .andExpect(jsonPath("$.newLines").value(10))
                .andExpect(jsonPath("$.duplicateLines").value(0))
                .andExpect(jsonPath("$.errorLines").value(0));
    }

    // TEST T11-2: Upload Excel file — parses rows correctly
    // Spec: 05-sap-ingestion.md Step 2 (XLSX support)
    @Test
    void uploadExcel_5Lines_returns201WithTotalLines() throws Exception {
        byte[] xlsxBytes = buildXlsx(5);
        MockMultipartFile file = new MockMultipartFile(
                "file", "SAP_FY26_JAN.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxBytes);

        mockMvc.perform(multipart("/api/v1/imports/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalLines").value(5))
                .andExpect(jsonPath("$.errorLines").value(0));
    }

    // TEST T11-3: Rows missing posting_date are flagged as errors
    // Spec: 05-sap-ingestion.md Step 2 (validation during parse)
    @Test
    void uploadCsv_missingPostingDate_flagsAsErrors() throws Exception {
        String csv = CSV_HEADER +
                csvRow("DOC1", "2026-01-15", "1000.00", "Globant") +
                csvRow("DOC2", "2026-01-16", "2000.00", "Globant") +
                "DOC3,,3000.00,Globant,CC001,1174905.SU.ES,500000,Monthly sustainment\n" +  // missing date
                "DOC4,,4000.00,Globant,CC001,1174905.SU.ES,500000,Monthly sustainment\n" +  // missing date
                csvRow("DOC5", "2026-01-18", "5000.00", "Globant") +
                csvRow("DOC6", "2026-01-19", "6000.00", "Globant") +
                csvRow("DOC7", "2026-01-20", "7000.00", "Globant") +
                csvRow("DOC8", "2026-01-21", "8000.00", "Globant") +
                csvRow("DOC9", "2026-01-22", "9000.00", "Globant") +
                csvRow("DOC10", "2026-01-23", "10000.00", "Globant");

        MockMultipartFile file = new MockMultipartFile(
                "file", "SAP_FY26_JAN.csv", "text/csv", csv.getBytes());

        mockMvc.perform(multipart("/api/v1/imports/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalLines").value(10))
                .andExpect(jsonPath("$.errorLines").value(2))
                .andExpect(jsonPath("$.newLines").value(8));
    }

    // TEST T11-4: Rows missing amount are flagged as errors
    // Spec: 05-sap-ingestion.md Step 2 (validation during parse)
    @Test
    void uploadCsv_missingAmount_flagsAsError() throws Exception {
        String csv = CSV_HEADER +
                csvRow("DOC1", "2026-01-15", "1000.00", "Globant") +
                "DOC2,2026-01-16,,Globant,CC001,1174905.SU.ES,500000,Monthly sustainment\n";  // missing amount

        MockMultipartFile file = new MockMultipartFile(
                "file", "SAP_FY26_JAN.csv", "text/csv", csv.getBytes());

        mockMvc.perform(multipart("/api/v1/imports/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalLines").value(2))
                .andExpect(jsonPath("$.errorLines").value(1))
                .andExpect(jsonPath("$.newLines").value(1));
    }

    // TEST T11-5: Corrupt file returns 400
    // Spec: 05-sap-ingestion.md Section 4 (Error Handling)
    @Test
    void uploadCorruptFile_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "corrupt.bin", "application/octet-stream",
                new byte[]{0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE});

        mockMvc.perform(multipart("/api/v1/imports/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    /** Build a minimal valid XLSX with N data rows for testing. */
    private byte[] buildXlsx(int rows) throws Exception {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            var sheet = workbook.createSheet("SAP");
            var header = sheet.createRow(0);
            String[] headers = {"Document Number", "Posting Date", "Amount",
                    "Vendor Name", "Cost Center", "WBS Element", "GL Account", "Description"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            for (int r = 1; r <= rows; r++) {
                var row = sheet.createRow(r);
                row.createCell(0).setCellValue("DOC" + r);
                row.createCell(1).setCellValue("2026-01-15");
                row.createCell(2).setCellValue(1000.0 * r);
                row.createCell(3).setCellValue("Globant");
                row.createCell(4).setCellValue("CC001");
                row.createCell(5).setCellValue("1174905.SU.ES");
                row.createCell(6).setCellValue("500000");
                row.createCell(7).setCellValue("Monthly sustainment");
            }
            var out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
