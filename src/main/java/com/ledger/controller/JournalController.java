package com.ledger.controller;

import com.ledger.dto.JournalEntryResponse;
import com.ledger.dto.JournalLineResponse;
import com.ledger.repository.JournalEntryRepository;
import com.ledger.repository.JournalLineRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for journal viewer endpoints.
 * Spec: 02-journal-ledger.md, 14-ui-views.md Section 9
 */
@RestController
@RequestMapping("/api/v1/journal")
public class JournalController {

    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;

    public JournalController(JournalEntryRepository journalEntryRepository,
                              JournalLineRepository journalLineRepository) {
        this.journalEntryRepository = journalEntryRepository;
        this.journalLineRepository = journalLineRepository;
    }

    /**
     * GET /api/v1/journal
     * List all journal entries sorted by effective_date descending.
     */
    @GetMapping
    public List<JournalEntryResponse> listEntries() {
        Sort sort = Sort.by(Sort.Direction.DESC, "effectiveDate", "createdAt");
        return journalEntryRepository.findAll(PageRequest.of(0, 200, sort))
                .getContent().stream().map(JournalEntryResponse::from).toList();
    }

    /**
     * GET /api/v1/journal/{id}/lines
     * Get the debit/credit lines for a journal entry.
     */
    @GetMapping("/{id}/lines")
    public ResponseEntity<List<JournalLineResponse>> getLines(@PathVariable UUID id) {
        if (!journalEntryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(journalLineRepository.findByJournalEntryEntryId(id)
                .stream().map(JournalLineResponse::from).toList());
    }
}
