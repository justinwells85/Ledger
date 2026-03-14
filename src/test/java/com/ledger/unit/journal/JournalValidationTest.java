package com.ledger.unit.journal;

import com.ledger.dto.JournalLineRequest;
import com.ledger.entity.AccountType;
import com.ledger.service.JournalService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for journal entry validation logic (no DB, no Spring).
 * Spec: 02-journal-ledger.md, 10-business-rules.md (BR-01, BR-02)
 */
class JournalValidationTest {

    private static final UUID PERIOD_ID = UUID.randomUUID();
    private static final UUID MILESTONE_ID = UUID.randomUUID();

    /**
     * U-JRN-01: Balanced entry accepted.
     * Spec: 02-journal-ledger.md Section 3, BR-01
     *
     * GIVEN 2 lines with debit $25,000 = credit $25,000
     * WHEN  validateEntry is called
     * THEN  no exception is thrown
     */
    @Test
    void balancedEntry_noException() {
        List<JournalLineRequest> lines = List.of(
                new JournalLineRequest(
                        AccountType.PLANNED, null, null, MILESTONE_ID, PERIOD_ID,
                        new BigDecimal("25000.00"), BigDecimal.ZERO,
                        null, null),
                new JournalLineRequest(
                        AccountType.VARIANCE_RESERVE, null, null, MILESTONE_ID, PERIOD_ID,
                        BigDecimal.ZERO, new BigDecimal("25000.00"),
                        null, null)
        );

        assertThatCode(() -> JournalService.validateEntry(lines))
                .doesNotThrowAnyException();
    }

    /**
     * U-JRN-02: Unbalanced entry rejected.
     * Spec: 02-journal-ledger.md Section 3, BR-01
     *
     * GIVEN 2 lines with debit $25,000 and credit $20,000
     * WHEN  validateEntry is called
     * THEN  IllegalArgumentException is thrown mentioning BR-01
     */
    @Test
    void unbalancedEntry_throwsException() {
        List<JournalLineRequest> lines = List.of(
                new JournalLineRequest(
                        AccountType.PLANNED, null, null, MILESTONE_ID, PERIOD_ID,
                        new BigDecimal("25000.00"), BigDecimal.ZERO,
                        null, null),
                new JournalLineRequest(
                        AccountType.VARIANCE_RESERVE, null, null, MILESTONE_ID, PERIOD_ID,
                        BigDecimal.ZERO, new BigDecimal("20000.00"),
                        null, null)
        );

        assertThatThrownBy(() -> JournalService.validateEntry(lines))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BR-01");
    }

    /**
     * U-JRN-03: Entry with 0 lines rejected.
     * Spec: 02-journal-ledger.md Section 3, BR-02
     *
     * GIVEN an empty list of lines
     * WHEN  validateEntry is called
     * THEN  IllegalArgumentException is thrown mentioning BR-02
     */
    @Test
    void zeroLines_throwsException() {
        assertThatThrownBy(() -> JournalService.validateEntry(Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BR-02");
    }

    /**
     * U-JRN-04: Entry with 1 line rejected.
     * Spec: 02-journal-ledger.md Section 3, BR-02
     *
     * GIVEN a list with only 1 line
     * WHEN  validateEntry is called
     * THEN  IllegalArgumentException is thrown mentioning BR-02
     */
    @Test
    void oneLine_throwsException() {
        List<JournalLineRequest> lines = List.of(
                new JournalLineRequest(
                        AccountType.PLANNED, null, null, MILESTONE_ID, PERIOD_ID,
                        new BigDecimal("25000.00"), BigDecimal.ZERO,
                        null, null)
        );

        assertThatThrownBy(() -> JournalService.validateEntry(lines))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BR-02");
    }

    /**
     * U-JRN-05: Line with both debit > 0 and credit > 0 rejected.
     * Spec: 02-journal-ledger.md Section 3 (ck_journal_line_one_side constraint)
     *
     * GIVEN a line with debit $25,000 AND credit $25,000
     * WHEN  validateEntry is called
     * THEN  IllegalArgumentException is thrown
     */
    @Test
    void lineWithBothDebitAndCredit_throwsException() {
        List<JournalLineRequest> lines = List.of(
                new JournalLineRequest(
                        AccountType.PLANNED, null, null, MILESTONE_ID, PERIOD_ID,
                        new BigDecimal("25000.00"), new BigDecimal("25000.00"),
                        null, null),
                new JournalLineRequest(
                        AccountType.VARIANCE_RESERVE, null, null, MILESTONE_ID, PERIOD_ID,
                        BigDecimal.ZERO, BigDecimal.ZERO,
                        null, null)
        );

        assertThatThrownBy(() -> JournalService.validateEntry(lines))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("both debit");
    }
}
