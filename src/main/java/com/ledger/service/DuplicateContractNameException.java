package com.ledger.service;

/**
 * Thrown when attempting to create a contract with a name that already exists.
 * Spec: 01-domain-model.md Section 2.3 (name UNIQUE constraint)
 */
public class DuplicateContractNameException extends RuntimeException {

    public DuplicateContractNameException(String name) {
        super("Contract with name '" + name + "' already exists");
    }
}
