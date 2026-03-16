-- ============================================================================
-- V011: Reference Data Tables
-- Spec: 18-admin-configuration.md Section 3, BR-91 through BR-97
-- Replaces hardcoded Java enums with database-managed reference tables.
-- ============================================================================

-- ─────────────────────────────────────────────
-- Funding Sources
-- ─────────────────────────────────────────────
CREATE TABLE ref_funding_source (
    code            VARCHAR(50)   PRIMARY KEY,
    display_name    VARCHAR(100)  NOT NULL,
    description     VARCHAR(500),
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order      INT           NOT NULL DEFAULT 0,

    CONSTRAINT ck_ref_funding_code CHECK (code ~ '^[A-Z0-9_]+$')
);

INSERT INTO ref_funding_source (code, display_name, description, is_active, sort_order) VALUES
    ('OPEX',       'Operating Expense',  'Day-to-day operating costs',               TRUE, 1),
    ('CAPEX',      'Capital Expense',    'Long-term capital investment',              TRUE, 2),
    ('OTHER_TEAM', 'Other Team',         'Costs funded by another team or org unit',  TRUE, 3);

-- ─────────────────────────────────────────────
-- Contract Statuses
-- ─────────────────────────────────────────────
CREATE TABLE ref_contract_status (
    code            VARCHAR(50)   PRIMARY KEY,
    display_name    VARCHAR(100)  NOT NULL,
    description     VARCHAR(500),
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order      INT           NOT NULL DEFAULT 0
);

INSERT INTO ref_contract_status (code, display_name, description, is_active, sort_order) VALUES
    ('ACTIVE',      'Active',      'Contract is currently active',    TRUE, 1),
    ('CLOSED',      'Closed',      'Contract has been closed',         TRUE, 2),
    ('TERMINATED',  'Terminated',  'Contract was terminated early',    TRUE, 3);

-- ─────────────────────────────────────────────
-- Project Statuses
-- ─────────────────────────────────────────────
CREATE TABLE ref_project_status (
    code            VARCHAR(50)   PRIMARY KEY,
    display_name    VARCHAR(100)  NOT NULL,
    description     VARCHAR(500),
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order      INT           NOT NULL DEFAULT 0
);

INSERT INTO ref_project_status (code, display_name, description, is_active, sort_order) VALUES
    ('ACTIVE', 'Active', 'Project is currently active', TRUE, 1),
    ('CLOSED', 'Closed', 'Project has been closed',      TRUE, 2);

-- ─────────────────────────────────────────────
-- Reconciliation Categories
-- ─────────────────────────────────────────────
CREATE TABLE ref_reconciliation_category (
    code                        VARCHAR(50)   PRIMARY KEY,
    display_name                VARCHAR(100)  NOT NULL,
    description                 VARCHAR(500),
    affects_accrual_lifecycle   BOOLEAN       NOT NULL DEFAULT FALSE,
    is_active                   BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order                  INT           NOT NULL DEFAULT 0
);

INSERT INTO ref_reconciliation_category (code, display_name, description, affects_accrual_lifecycle, is_active, sort_order) VALUES
    ('INVOICE',           'Invoice',           'Standard vendor invoice payment',        FALSE, TRUE, 1),
    ('ACCRUAL',           'Accrual',           'Accrual entry for services not yet billed', TRUE,  TRUE, 2),
    ('ACCRUAL_REVERSAL',  'Accrual Reversal',  'Reversal of a prior accrual entry',      TRUE,  TRUE, 3),
    ('ALLOCATION',        'Allocation',        'Cost allocation between cost centers',    FALSE, TRUE, 4);
