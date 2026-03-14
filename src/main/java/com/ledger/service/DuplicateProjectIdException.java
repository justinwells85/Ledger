package com.ledger.service;

/**
 * Thrown when attempting to create a project with an ID that already exists.
 * Spec: 01-domain-model.md Section 2.4 (project_id unique constraint)
 */
public class DuplicateProjectIdException extends RuntimeException {

    public DuplicateProjectIdException(String projectId) {
        super("Project with ID '" + projectId + "' already exists");
    }
}
