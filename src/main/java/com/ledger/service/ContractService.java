package com.ledger.service;

import com.ledger.config.SecurityUtils;
import com.ledger.dto.ContractCreateRequest;
import com.ledger.dto.ContractUpdateRequest;
import com.ledger.entity.Contract;
import com.ledger.entity.ContractStatus;
import com.ledger.repository.ContractRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for contract CRUD operations with audit logging.
 * Spec: 01-domain-model.md Section 2.3, 11-change-management.md, 13-api-design.md Section 3
 */
@Service
@Transactional
public class ContractService {

    private final ContractRepository contractRepository;
    private final AuditService auditService;

    public ContractService(ContractRepository contractRepository, AuditService auditService) {
        this.contractRepository = contractRepository;
        this.auditService = auditService;
    }

    /**
     * Create a new contract.
     * Validates name uniqueness and date constraints.
     * Logs CREATE to audit_log.
     */
    public Contract createContract(ContractCreateRequest request) {
        // Validate name uniqueness
        if (contractRepository.existsByName(request.name())) {
            throw new DuplicateContractNameException(request.name());
        }

        // Validate date constraint: end_date must be after start_date
        if (request.endDate() != null && !request.endDate().isAfter(request.startDate())) {
            throw new IllegalArgumentException("end_date must be after start_date");
        }

        Contract contract = new Contract();
        contract.setName(request.name());
        contract.setVendor(request.vendor());
        contract.setDescription(request.description());
        contract.setOwnerUser(request.ownerUser());
        contract.setStartDate(request.startDate());
        contract.setEndDate(request.endDate());
        contract.setCreatedBy(SecurityUtils.currentUsername());

        Contract saved = contractRepository.save(contract);

        // Audit log for CREATE
        auditService.log("CONTRACT", saved.getContractId().toString(), "CREATE",
                null, null, SecurityUtils.currentUsername());

        return saved;
    }

    /**
     * Update an existing contract.
     * Requires reason field. Compares old vs new fields and builds changes JSON for audit.
     */
    public Contract updateContract(UUID contractId, ContractUpdateRequest request) {
        // Validate reason is present
        if (request.reason() == null || request.reason().isBlank()) {
            throw new IllegalArgumentException("reason is required for contract updates");
        }

        Contract existing = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));

        // Build changes map by comparing old vs new
        Map<String, Map<String, String>> changes = new LinkedHashMap<>();

        if (request.name() != null && !request.name().equals(existing.getName())) {
            changes.put("name", Map.of("before", existing.getName(), "after", request.name()));
            existing.setName(request.name());
        }
        if (request.vendor() != null && !request.vendor().equals(existing.getVendor())) {
            changes.put("vendor", Map.of("before", existing.getVendor(), "after", request.vendor()));
            existing.setVendor(request.vendor());
        }
        if (request.description() != null && !request.description().equals(nullToEmpty(existing.getDescription()))) {
            changes.put("description", Map.of(
                    "before", nullToEmpty(existing.getDescription()),
                    "after", request.description()));
            existing.setDescription(request.description());
        }
        if (request.ownerUser() != null && !request.ownerUser().equals(existing.getOwnerUser())) {
            changes.put("ownerUser", Map.of("before", existing.getOwnerUser(), "after", request.ownerUser()));
            existing.setOwnerUser(request.ownerUser());
        }
        if (request.startDate() != null && !request.startDate().equals(existing.getStartDate())) {
            changes.put("startDate", Map.of(
                    "before", existing.getStartDate().toString(),
                    "after", request.startDate().toString()));
            existing.setStartDate(request.startDate());
        }
        if (request.endDate() != null && !request.endDate().equals(existing.getEndDate())) {
            changes.put("endDate", Map.of(
                    "before", existing.getEndDate() != null ? existing.getEndDate().toString() : "",
                    "after", request.endDate().toString()));
            existing.setEndDate(request.endDate());
        }
        if (request.status() != null) {
            ContractStatus newStatus = ContractStatus.valueOf(request.status());
            if (newStatus != existing.getStatus()) {
                changes.put("status", Map.of("before", existing.getStatus().name(), "after", newStatus.name()));
                existing.setStatus(newStatus);
            }
        }

        Contract saved = contractRepository.save(existing);

        // Audit log for UPDATE (only if something changed)
        if (!changes.isEmpty()) {
            auditService.log("CONTRACT", saved.getContractId().toString(), "UPDATE",
                    changes, request.reason(), SecurityUtils.currentUsername());
        }

        return saved;
    }

    /**
     * Get a single contract by ID.
     */
    @Transactional(readOnly = true)
    public Contract getContract(UUID contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
    }

    /**
     * List contracts, optionally filtered by status.
     */
    @Transactional(readOnly = true)
    public List<Contract> listContracts(ContractStatus status) {
        if (status != null) {
            return contractRepository.findByStatus(status);
        }
        Sort sort = Sort.by(Sort.Direction.ASC, "name");
        return contractRepository.findAll(PageRequest.of(0, 200, sort)).getContent();
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
