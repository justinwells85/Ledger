package com.ledger.controller;

import com.ledger.entity.*;
import com.ledger.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for reference data management.
 * Spec: 18-admin-configuration.md Section 3, BR-91 through BR-97
 */
@RestController
@RequestMapping("/api/v1/admin/reference-data")
public class ReferenceDataController {

    /** Shared DTO for reference data entries. */
    public record RefEntry(String code, String displayName, String description, boolean active,
                           int sortOrder, Boolean affectsAccrualLifecycle) {
        public static RefEntry from(RefFundingSource e) {
            return new RefEntry(e.getCode(), e.getDisplayName(), e.getDescription(), e.isActive(), e.getSortOrder(), null);
        }
        public static RefEntry from(RefContractStatus e) {
            return new RefEntry(e.getCode(), e.getDisplayName(), e.getDescription(), e.isActive(), e.getSortOrder(), null);
        }
        public static RefEntry from(RefProjectStatus e) {
            return new RefEntry(e.getCode(), e.getDisplayName(), e.getDescription(), e.isActive(), e.getSortOrder(), null);
        }
        public static RefEntry from(RefReconciliationCategory e) {
            return new RefEntry(e.getCode(), e.getDisplayName(), e.getDescription(), e.isActive(), e.getSortOrder(),
                    e.isAffectsAccrualLifecycle());
        }
    }

    private final RefFundingSourceRepository fundingSourceRepo;
    private final RefContractStatusRepository contractStatusRepo;
    private final RefProjectStatusRepository projectStatusRepo;
    private final RefReconciliationCategoryRepository reconciliationCategoryRepo;

    public ReferenceDataController(RefFundingSourceRepository fundingSourceRepo,
                                   RefContractStatusRepository contractStatusRepo,
                                   RefProjectStatusRepository projectStatusRepo,
                                   RefReconciliationCategoryRepository reconciliationCategoryRepo) {
        this.fundingSourceRepo = fundingSourceRepo;
        this.contractStatusRepo = contractStatusRepo;
        this.projectStatusRepo = projectStatusRepo;
        this.reconciliationCategoryRepo = reconciliationCategoryRepo;
    }

    /**
     * GET /api/v1/admin/reference-data/{type}
     * List all entries for a reference data type.
     */
    @GetMapping("/{type}")
    public List<RefEntry> listByType(@PathVariable String type) {
        return switch (type.toUpperCase()) {
            case "FUNDING_SOURCE" -> fundingSourceRepo.findAllByOrderBySortOrderAsc().stream()
                    .map(RefEntry::from).toList();
            case "CONTRACT_STATUS" -> contractStatusRepo.findAllByOrderBySortOrderAsc().stream()
                    .map(RefEntry::from).toList();
            case "PROJECT_STATUS" -> projectStatusRepo.findAllByOrderBySortOrderAsc().stream()
                    .map(RefEntry::from).toList();
            case "RECONCILIATION_CATEGORY" -> reconciliationCategoryRepo.findAllByOrderBySortOrderAsc().stream()
                    .map(RefEntry::from).toList();
            default -> throw new IllegalArgumentException("Unknown reference data type: " + type);
        };
    }

    /**
     * POST /api/v1/admin/reference-data/{type}
     * Create a new reference data entry. ADMIN only.
     */
    @PostMapping("/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefEntry> create(@PathVariable String type,
                                           @RequestBody Map<String, String> body) {
        String code = body.get("code");
        String displayName = body.getOrDefault("displayName", "");
        String description = body.getOrDefault("description", "");

        if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
        if (!code.matches("[A-Z0-9_]+")) throw new IllegalArgumentException("Code must be uppercase alphanumeric with underscores");

        RefEntry created = switch (type.toUpperCase()) {
            case "FUNDING_SOURCE" -> {
                RefFundingSource e = new RefFundingSource();
                e.setCode(code);
                e.setDisplayName(displayName);
                e.setDescription(description);
                yield RefEntry.from(fundingSourceRepo.save(e));
            }
            case "CONTRACT_STATUS" -> {
                RefContractStatus e = new RefContractStatus();
                e.setCode(code);
                e.setDisplayName(displayName);
                e.setDescription(description);
                yield RefEntry.from(contractStatusRepo.save(e));
            }
            case "PROJECT_STATUS" -> {
                RefProjectStatus e = new RefProjectStatus();
                e.setCode(code);
                e.setDisplayName(displayName);
                e.setDescription(description);
                yield RefEntry.from(projectStatusRepo.save(e));
            }
            case "RECONCILIATION_CATEGORY" -> {
                RefReconciliationCategory e = new RefReconciliationCategory();
                e.setCode(code);
                e.setDisplayName(displayName);
                e.setDescription(description);
                yield RefEntry.from(reconciliationCategoryRepo.save(e));
            }
            default -> throw new IllegalArgumentException("Unknown reference data type: " + type);
        };

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * POST /api/v1/admin/reference-data/{type}/{code}/toggle-active
     * Toggle the active/inactive state of a reference data entry. ADMIN only.
     * BR-94: ACTIVE contract status cannot be deactivated.
     * BR-96: ACCRUAL and ACCRUAL_REVERSAL reconciliation categories cannot be deactivated.
     */
    @PostMapping("/{type}/{code}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public RefEntry toggleActive(@PathVariable String type, @PathVariable String code) {
        return switch (type.toUpperCase()) {
            case "FUNDING_SOURCE" -> {
                RefFundingSource e = fundingSourceRepo.findById(code)
                        .orElseThrow(() -> new IllegalArgumentException("Not found: " + code));
                e.setActive(!e.isActive());
                yield RefEntry.from(fundingSourceRepo.save(e));
            }
            case "CONTRACT_STATUS" -> {
                if ("ACTIVE".equals(code)) {
                    // BR-94: the ACTIVE status is required and cannot be deactivated
                    throw new IllegalStateException("The ACTIVE contract status is required and cannot be deactivated");
                }
                RefContractStatus e = contractStatusRepo.findById(code)
                        .orElseThrow(() -> new IllegalArgumentException("Not found: " + code));
                e.setActive(!e.isActive());
                yield RefEntry.from(contractStatusRepo.save(e));
            }
            case "PROJECT_STATUS" -> {
                RefProjectStatus e = projectStatusRepo.findById(code)
                        .orElseThrow(() -> new IllegalArgumentException("Not found: " + code));
                e.setActive(!e.isActive());
                yield RefEntry.from(projectStatusRepo.save(e));
            }
            case "RECONCILIATION_CATEGORY" -> {
                if ("ACCRUAL".equals(code) || "ACCRUAL_REVERSAL".equals(code)) {
                    // BR-96: system-reserved categories cannot be deactivated
                    throw new IllegalStateException("Category " + code + " is system-reserved and cannot be deactivated");
                }
                RefReconciliationCategory e = reconciliationCategoryRepo.findById(code)
                        .orElseThrow(() -> new IllegalArgumentException("Not found: " + code));
                e.setActive(!e.isActive());
                yield RefEntry.from(reconciliationCategoryRepo.save(e));
            }
            default -> throw new IllegalArgumentException("Unknown reference data type: " + type);
        };
    }
}
