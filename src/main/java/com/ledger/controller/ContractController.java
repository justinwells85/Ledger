package com.ledger.controller;

import com.ledger.dto.ContractCreateRequest;
import com.ledger.dto.ContractResponse;
import com.ledger.dto.ContractUpdateRequest;
import com.ledger.entity.ContractStatus;
import com.ledger.service.ContractService;
import com.ledger.service.DuplicateContractNameException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for contract operations.
 * Spec: 13-api-design.md Section 3
 */
@RestController
@RequestMapping("/api/v1/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    /**
     * GET /api/v1/contracts
     * List all contracts, optionally filtered by status.
     */
    @GetMapping
    public List<ContractResponse> listContracts(
            @RequestParam(required = false) ContractStatus status) {
        return contractService.listContracts(status).stream()
                .map(ContractResponse::from)
                .toList();
    }

    /**
     * GET /api/v1/contracts/{contractId}
     * Get a single contract.
     */
    @GetMapping("/{contractId}")
    public ContractResponse getContract(@PathVariable UUID contractId) {
        return ContractResponse.from(contractService.getContract(contractId));
    }

    /**
     * POST /api/v1/contracts
     * Create a new contract.
     */
    @PostMapping
    public ResponseEntity<ContractResponse> createContract(@Valid @RequestBody ContractCreateRequest request) {
        var contract = contractService.createContract(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ContractResponse.from(contract));
    }

    /**
     * PUT /api/v1/contracts/{contractId}
     * Update contract metadata. Requires reason for audit.
     */
    @RequestMapping(value = "/{contractId}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ContractResponse updateContract(
            @PathVariable UUID contractId,
            @RequestBody ContractUpdateRequest request) {
        return ContractResponse.from(contractService.updateContract(contractId, request));
    }

    @ExceptionHandler(DuplicateContractNameException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateName(DuplicateContractNameException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }
}
