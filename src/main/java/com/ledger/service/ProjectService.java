package com.ledger.service;

import com.ledger.config.SecurityUtils;
import com.ledger.dto.ProjectCreateRequest;
import com.ledger.dto.ProjectUpdateRequest;
import com.ledger.entity.Contract;
import com.ledger.entity.FundingSource;
import com.ledger.entity.Project;
import com.ledger.entity.ProjectStatus;
import com.ledger.repository.ContractRepository;
import com.ledger.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for project CRUD operations.
 * Spec: 01-domain-model.md Section 2.4, 13-api-design.md Section 4
 */
@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ContractRepository contractRepository;
    private final AuditService auditService;

    public ProjectService(ProjectRepository projectRepository, ContractRepository contractRepository,
                          AuditService auditService) {
        this.projectRepository = projectRepository;
        this.contractRepository = contractRepository;
        this.auditService = auditService;
    }

    /**
     * Create a new project under a contract.
     * Validates project_id uniqueness and funding source.
     */
    public Project createProject(UUID contractId, ProjectCreateRequest request) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));

        // Validate funding source
        FundingSource fundingSource;
        try {
            fundingSource = FundingSource.valueOf(request.fundingSource());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid funding source: " + request.fundingSource());
        }

        // Validate project_id uniqueness
        if (projectRepository.existsByProjectId(request.projectId())) {
            throw new DuplicateProjectIdException(request.projectId());
        }

        Project project = new Project();
        project.setProjectId(request.projectId());
        project.setContract(contract);
        project.setWbse(request.wbse());
        project.setName(request.name());
        project.setFundingSource(fundingSource);
        project.setCreatedBy(SecurityUtils.currentUsername());

        return projectRepository.save(project);
    }

    /**
     * Update an existing project.
     * Requires reason field. Logs UPDATE to audit_log.
     * Spec: 11-change-management.md Section 2.2
     */
    public Project updateProject(String projectId, ProjectUpdateRequest request) {
        if (request.reason() == null || request.reason().isBlank()) {
            throw new IllegalArgumentException("reason is required for project updates");
        }

        Project existing = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        Map<String, Map<String, String>> changes = new LinkedHashMap<>();

        if (request.name() != null && !request.name().equals(existing.getName())) {
            changes.put("name", Map.of("before", existing.getName(), "after", request.name()));
            existing.setName(request.name());
        }
        if (request.wbse() != null && !request.wbse().equals(existing.getWbse())) {
            changes.put("wbse", Map.of("before", existing.getWbse(), "after", request.wbse()));
            existing.setWbse(request.wbse());
        }
        if (request.status() != null) {
            ProjectStatus newStatus = ProjectStatus.valueOf(request.status());
            if (newStatus != existing.getStatus()) {
                changes.put("status", Map.of("before", existing.getStatus().name(), "after", newStatus.name()));
                existing.setStatus(newStatus);
            }
        }

        Project saved = projectRepository.save(existing);

        if (!changes.isEmpty()) {
            auditService.log("PROJECT", projectId, "UPDATE", changes, request.reason(), SecurityUtils.currentUsername());
        }

        return saved;
    }

    /**
     * List projects for a contract, optionally filtered by funding source.
     */
    @Transactional(readOnly = true)
    public List<Project> listProjects(UUID contractId, FundingSource fundingSource) {
        if (fundingSource != null) {
            return projectRepository.findByContractContractIdAndFundingSource(contractId, fundingSource);
        }
        return projectRepository.findByContractContractId(contractId);
    }

    /**
     * Get a single project by ID.
     */
    @Transactional(readOnly = true)
    public Project getProject(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
    }
}
