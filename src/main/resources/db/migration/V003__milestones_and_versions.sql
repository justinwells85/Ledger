-- ============================================================================
-- V003: Milestones and Milestone Versions
-- Spec: 01-domain-model.md Sections 2.5/2.6, 04-milestone-versioning.md
-- ============================================================================

CREATE TABLE milestone (
    milestone_id    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      VARCHAR(20)  NOT NULL REFERENCES project(project_id),
    name            VARCHAR(300) NOT NULL,
    description     TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      VARCHAR(100) NOT NULL
);

CREATE INDEX idx_milestone_project ON milestone(project_id);

CREATE TABLE milestone_version (
    version_id      UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    milestone_id    UUID           NOT NULL REFERENCES milestone(milestone_id),
    version_number  INT            NOT NULL,
    planned_amount  DECIMAL(15,2)  NOT NULL,          -- can be 0 for cancellations
    fiscal_period_id UUID          NOT NULL REFERENCES fiscal_period(period_id),
    effective_date  DATE           NOT NULL,
    reason          VARCHAR(500)   NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by      VARCHAR(100)   NOT NULL,

    CONSTRAINT uq_milestone_version UNIQUE (milestone_id, version_number),
    CONSTRAINT ck_milestone_version_num CHECK (version_number >= 1),
    CONSTRAINT ck_milestone_version_amount CHECK (planned_amount >= 0)
);

CREATE INDEX idx_milestone_version_milestone ON milestone_version(milestone_id);
CREATE INDEX idx_milestone_version_effective ON milestone_version(effective_date);
CREATE INDEX idx_milestone_version_period ON milestone_version(fiscal_period_id);

-- Composite index for the common "latest version as of date" query pattern
-- SELECT ... WHERE milestone_id = ? AND effective_date <= ? ORDER BY version_number DESC LIMIT 1
CREATE INDEX idx_milestone_version_lookup ON milestone_version(milestone_id, effective_date, version_number DESC);

COMMENT ON TABLE milestone IS 'Deliverables/payment obligations within a project';
COMMENT ON TABLE milestone_version IS 'Point-in-time snapshots of planned amount and target period. Independently versioned per milestone.';
COMMENT ON COLUMN milestone_version.effective_date IS 'When this version became active for reporting. Used by time machine.';
COMMENT ON COLUMN milestone_version.reason IS 'Required explanation for the change. V1 defaults to initial budget rationale.';
