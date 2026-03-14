-- ============================================================================
-- V002: Contracts and Projects
-- Spec: 01-domain-model.md, Sections 2.3 and 2.4
-- ============================================================================

CREATE TABLE contract (
    contract_id     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL UNIQUE,
    vendor          VARCHAR(200) NOT NULL,
    description     TEXT,
    owner_user      VARCHAR(100) NOT NULL,
    start_date      DATE         NOT NULL,
    end_date        DATE,                           -- null = open-ended
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      VARCHAR(100) NOT NULL,

    CONSTRAINT ck_contract_status CHECK (status IN ('ACTIVE', 'CLOSED', 'TERMINATED')),
    CONSTRAINT ck_contract_dates  CHECK (end_date IS NULL OR end_date > start_date)
);

CREATE TABLE project (
    project_id      VARCHAR(20)  PRIMARY KEY,       -- business key, e.g., 'PR13752'
    contract_id     UUID         NOT NULL REFERENCES contract(contract_id),
    wbse            VARCHAR(50)  NOT NULL,
    name            VARCHAR(300) NOT NULL,
    funding_source  VARCHAR(20)  NOT NULL DEFAULT 'OPEX',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      VARCHAR(100) NOT NULL,

    CONSTRAINT ck_project_funding CHECK (funding_source IN ('OPEX', 'CAPEX', 'OTHER_TEAM')),
    CONSTRAINT ck_project_status  CHECK (status IN ('ACTIVE', 'CLOSED')),
    CONSTRAINT uq_project_wbse   UNIQUE (wbse, project_id)
);

CREATE INDEX idx_project_contract ON project(contract_id);
CREATE INDEX idx_project_wbse ON project(wbse);

COMMENT ON TABLE contract IS 'Vendor agreements / billing relationships';
COMMENT ON TABLE project IS 'Work streams within a contract, identified by WBSE + Project ID';
COMMENT ON COLUMN project.funding_source IS 'Set at project/WBSE level, applies to all milestones within';
