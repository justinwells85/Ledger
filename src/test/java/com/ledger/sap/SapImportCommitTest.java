package com.ledger.sap;

import com.ledger.BaseIntegrationTest;
import com.ledger.entity.*;
import com.ledger.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T13 — SAP Import: Commit (ACTUAL_IMPORT journal entries)
 * Spec: 05-sap-ingestion.md Steps 5-6, 02-journal-ledger.md 5.5, BR-70-73
 * Tests: T13-1 through T13-5
 */
@AutoConfigureMockMvc
class SapImportCommitTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SapImportRepository sapImportRepository;

    @Autowired
    private ActualLineRepository actualLineRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private JournalLineRepository journalLineRepository;

    @Autowired
    private FiscalPeriodRepository fiscalPeriodRepository;

    @BeforeEach
    void setUp() {
        journalLineRepository.deleteAll();
        journalEntryRepository.deleteAll();
        actualLineRepository.deleteAll();
        sapImportRepository.deleteAll();
    }

    private static final String CSV_HEADER =
            "Document Number,Posting Date,Amount,Vendor Name,Cost Center,WBS Element,GL Account,Description\n";

    private MockMultipartFile makeCsvFile(int count, String filename) {
        StringBuilder sb = new StringBuilder(CSV_HEADER);
        for (int i = 1; i <= count; i++) {
            sb.append(String.format("DOC%d,2026-01-15,%s,Globant,CC001,1174905.SU.ES,500000,Line %d\n",
                    i, 1000 * i, i));
        }
        return new MockMultipartFile("file", filename, "text/csv", sb.toString().getBytes());
    }

    private String uploadAndGetImportId(int lines) throws Exception {
        var result = mockMvc.perform(multipart("/api/v1/imports/upload").file(makeCsvFile(lines, "sap.csv")))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        // Extract importId from JSON
        return body.replaceAll(".*\"importId\":\"([^\"]+)\".*", "$1");
    }

    // TEST T13-1: Commit creates actual lines and sets status to COMMITTED
    // Spec: 05-sap-ingestion.md Step 6
    @Test
    void commit_createsActualLinesAndSetsCommitted() throws Exception {
        String importId = uploadAndGetImportId(5);

        mockMvc.perform(post("/api/v1/imports/{id}/commit", importId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMMITTED")));

        SapImport committed = sapImportRepository.findById(
                java.util.UUID.fromString(importId)).orElseThrow();
        assertThat(committed.getStatus()).isEqualTo(SapImportStatus.COMMITTED);
        assertThat(committed.getNewLines()).isEqualTo(5);
    }

    // TEST T13-2: Each non-duplicate actual line gets an ACTUAL_IMPORT journal entry
    // Spec: 05-sap-ingestion.md Step 6, 02-journal-ledger.md 5.5
    @Test
    void commit_createsJournalEntriesPerNewLine() throws Exception {
        String importId = uploadAndGetImportId(5);
        mockMvc.perform(post("/api/v1/imports/{id}/commit", importId))
                .andExpect(status().isOk());

        List<JournalEntry> entries = journalEntryRepository.findAll();
        long actualImportEntries = entries.stream()
                .filter(e -> e.getEntryType() == JournalEntryType.ACTUAL_IMPORT)
                .count();
        assertThat(actualImportEntries).isEqualTo(5);

        // Each entry has 2 lines: debit ACTUAL, credit VARIANCE_RESERVE
        List<JournalLine> allLines = journalLineRepository.findAll();
        assertThat(allLines).hasSize(10); // 5 entries × 2 lines

        long debitActuals = allLines.stream()
                .filter(l -> l.getAccount() == AccountType.ACTUAL && l.getDebit().compareTo(java.math.BigDecimal.ZERO) > 0)
                .count();
        assertThat(debitActuals).isEqualTo(5);

        long creditVR = allLines.stream()
                .filter(l -> l.getAccount() == AccountType.VARIANCE_RESERVE && l.getCredit().compareTo(java.math.BigDecimal.ZERO) > 0)
                .count();
        assertThat(creditVR).isEqualTo(5);
    }

    // TEST T13-3: Posting date resolved to fiscal period
    // Spec: 05-sap-ingestion.md Step 6 (fiscal_period resolution), BR-73
    @Test
    void commit_resolvesPostingDateToFiscalPeriod() throws Exception {
        String importId = uploadAndGetImportId(1);
        mockMvc.perform(post("/api/v1/imports/{id}/commit", importId))
                .andExpect(status().isOk());

        ActualLine line = actualLineRepository.findAll().get(0);
        assertThat(line.getFiscalPeriodId()).isNotNull();

        // January 2026 → FY26-04-JAN
        var period = fiscalPeriodRepository.findById(line.getFiscalPeriodId()).orElseThrow();
        assertThat(period.getPeriodKey()).isEqualTo("FY26-04-JAN");
    }

    // TEST T13-4: Negative amount (accrual reversal) creates valid balanced journal
    // Spec: 05-sap-ingestion.md Section 2 Step 6 (negative amounts)
    @Test
    void commit_negativeAmount_createsBalancedJournal() throws Exception {
        String csv = CSV_HEADER +
                "DOC1,2026-01-15,-25000.00,Globant,CC001,1174905.SU.ES,500000,Reversal\n";
        MockMultipartFile file = new MockMultipartFile("file", "reversal.csv", "text/csv", csv.getBytes());
        var result = mockMvc.perform(multipart("/api/v1/imports/upload").file(file))
                .andExpect(status().isCreated())
                .andReturn();
        String importId = result.getResponse().getContentAsString()
                .replaceAll(".*\"importId\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/v1/imports/{id}/commit", importId))
                .andExpect(status().isOk());

        // Journal entry must balance: negative debit = negative credit of same magnitude
        List<JournalEntry> entries = journalEntryRepository.findAll();
        JournalEntry entry = entries.stream()
                .filter(e -> e.getEntryType() == JournalEntryType.ACTUAL_IMPORT)
                .findFirst().orElseThrow();

        List<JournalLine> lines = journalLineRepository.findAll().stream()
                .filter(l -> l.getJournalEntry().getEntryId().equals(entry.getEntryId()))
                .toList();
        assertThat(lines).hasSize(2);

        java.math.BigDecimal totalDebit = lines.stream()
                .map(JournalLine::getDebit).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal totalCredit = lines.stream()
                .map(JournalLine::getCredit).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        assertThat(totalDebit).isEqualByComparingTo(totalCredit);
    }

    // TEST T13-5: Reject import sets status to REJECTED (no actual lines created)
    // Spec: 05-sap-ingestion.md Section 2 Step 5
    @Test
    void rejectImport_setsRejectedStatus() throws Exception {
        // Stage an import but DON'T commit
        String importId = uploadAndGetImportId(3);

        mockMvc.perform(post("/api/v1/imports/{id}/reject", importId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")));

        SapImport rejected = sapImportRepository.findById(
                java.util.UUID.fromString(importId)).orElseThrow();
        assertThat(rejected.getStatus()).isEqualTo(SapImportStatus.REJECTED);

        // Lines still exist in the staging table but no journal entries
        assertThat(journalEntryRepository.findAll()).isEmpty();
    }
}
