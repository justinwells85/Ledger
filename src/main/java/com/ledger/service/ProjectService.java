package com.ledger.service;

import com.ledger.dto.ProjectCreateRequest;
import com.ledger.entity.Contract;
import com.ledger.entity.FundingSource;
import com.ledger.entity.Project;
import com.ledger.repository.ContractRepository;
import com.ledger.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    public ProjectService(ProjectRepository projectRepository, ContractRepository contractRepository) {
        this.projectRepository = projectRepository;
        this.contractRepository = contractRepository;
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
        project.setCreatedBy("system");

        return projectRepository.save(project);
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
