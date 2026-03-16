package com.ledger.controller;

import com.ledger.dto.AuditLogResponse;
import com.ledger.dto.JournalEntryResponse;
import com.ledger.dto.MilestoneVersionResponse;
import com.ledger.dto.ReconciliationResponse;
import com.ledger.entity.AuditLog;
import com.ledger.repository.AuditLogRepository;
import com.ledger.repository.JournalEntryRepository;
import com.ledger.repository.MilestoneVersionRepository;
import com.ledger.repository.ReconciliationRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for audit log query endpoints.
 * Spec: 11-change-management.md Section 5
 */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final MilestoneVersionRepository milestoneVersionRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final JournalEntryRepository journalEntryRepository;

    public AuditController(AuditLogRepository auditLogRepository,
                           MilestoneVersionRepository milestoneVersionRepository,
                           ReconciliationRepository reconciliationRepository,
                           JournalEntryRepository journalEntryRepository) {
        this.auditLogRepository = auditLogRepository;
        this.milestoneVersionRepository = milestoneVersionRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    /**
     * GET /api/v1/audit
     * Return audit log entries with optional filters.
     * Spec: 18-admin-configuration.md Section 5
     */
    @GetMapping
    public List<AuditLogResponse> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        Specification<AuditLog> spec = buildSpec(entityType, entityId, createdBy, action, from, to);
        return auditLogRepository.findAll(spec).stream().map(AuditLogResponse::from).toList();
    }

    /**
     * GET /api/v1/audit/export.csv
     * Export filtered audit log as CSV.
     * Spec: 18-admin-configuration.md Section 5
     */
    @GetMapping(value = "/export.csv", produces = "text/csv")
    public ResponseEntity<String> exportAuditCsv(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        Specification<AuditLog> spec = buildSpec(entityType, entityId, createdBy, action, from, to);
        List<AuditLog> entries = auditLogRepository.findAll(spec);

        StringBuilder csv = new StringBuilder("timestamp,entityType,entityId,action,createdBy,reason\n");
        for (AuditLog log : entries) {
            csv.append(csvEscape(log.getCreatedAt().toString())).append(',')
               .append(csvEscape(log.getEntityType())).append(',')
               .append(csvEscape(log.getEntityId())).append(',')
               .append(csvEscape(log.getAction())).append(',')
               .append(csvEscape(log.getCreatedBy())).append(',')
               .append(csvEscape(log.getReason() != null ? log.getReason() : "")).append('\n');
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-log.csv\"");
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    private Specification<AuditLog> buildSpec(String entityType, String entityId, String createdBy,
                                               String action, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entityType != null && !entityType.isBlank())
                predicates.add(cb.equal(root.get("entityType"), entityType));
            if (entityId != null && !entityId.isBlank())
                predicates.add(cb.like(cb.lower(root.get("entityId")), "%" + entityId.toLowerCase() + "%"));
            if (createdBy != null && !createdBy.isBlank())
                predicates.add(cb.equal(root.get("createdBy"), createdBy));
            if (action != null && !action.isBlank())
                predicates.add(cb.equal(root.get("action"), action));
            if (from != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
                        from.atStartOfDay(ZoneOffset.UTC).toInstant()));
            if (to != null)
                predicates.add(cb.lessThan(root.get("createdAt"),
                        to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** Milestone audit history response. */
    public record MilestoneAuditHistory(
            UUID milestoneId,
            List<MilestoneVersionResponse> versions,
            List<ReconciliationResponse> reconciliations,
            List<JournalEntryResponse> journalEntries
    ) {}

    /**
     * GET /api/v1/audit/contract/{id}
     * Return all audit log entries for a specific contract.
     * Spec: 11-change-management.md Section 5
     */
    @GetMapping("/contract/{id}")
    public List<AuditLogResponse> getAuditLogsForContract(@PathVariable UUID id) {
        return auditLogRepository.findByEntityTypeAndEntityId("CONTRACT", id.toString())
                .stream().map(AuditLogResponse::from).toList();
    }

    /**
     * GET /api/v1/audit/user/{username}
     * Return all audit log entries created by a specific user.
     * Spec: 11-change-management.md Section 5
     */
    @GetMapping("/user/{username}")
    public List<AuditLogResponse> getAuditLogsForUser(@PathVariable String username) {
        return auditLogRepository.findByCreatedBy(username)
                .stream().map(AuditLogResponse::from).toList();
    }

    /**
     * GET /api/v1/audit/changes?from=YYYY-MM-DD&to=YYYY-MM-DD
     * Return all audit log entries within a date range (inclusive).
     * Spec: 11-change-management.md Section 5
     */
    @GetMapping("/changes")
    public List<AuditLogResponse> getAuditLogsByDateRange(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return auditLogRepository.findByCreatedAtBetween(fromInstant, toInstant)
                .stream().map(AuditLogResponse::from).toList();
    }

    /**
     * GET /api/v1/audit/milestone/{id}
     * Return full audit history for a milestone: versions, reconciliations, journal entries.
     * Spec: 11-change-management.md Section 5
     */
    @GetMapping("/milestone/{id}")
    public ResponseEntity<MilestoneAuditHistory> getMilestoneAuditHistory(@PathVariable UUID id) {
        List<MilestoneVersionResponse> versions = milestoneVersionRepository.findVersionHistory(id)
                .stream().map(MilestoneVersionResponse::from).toList();
        List<ReconciliationResponse> reconciliations = reconciliationRepository.findByMilestoneId(id)
                .stream().map(ReconciliationResponse::from).toList();
        List<JournalEntryResponse> journalEntries = journalEntryRepository.findByMilestoneId(id)
                .stream().map(JournalEntryResponse::from).toList();
        return ResponseEntity.ok(new MilestoneAuditHistory(id, versions, reconciliations, journalEntries));
    }
}
