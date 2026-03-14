-- ============================================================================
-- V006: Reconciliation
-- Spec: 06-reconciliation.md, 07-accrual-lifecycle.md
-- ============================================================================

CREATE TABLE reconciliation (
    reconciliation_id  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    actual_id          UUID         NOT NULL REFERENCES actual_line(actual_id),
    milestone_id       UUID         NOT NULL REFERENCES milestone(milestone_id),
    category           VARCHAR(30)  NOT NULL,
    match_notes        TEXT,                         -- optional user explanation
    reconciled_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    reconciled_by      VARCHAR(100) NOT NULL,

    -- BR-06: an actual can be reconciled to at most one milestone
    CONSTRAINT uq_reconciliation_actual UNIQUE (actual_id),

    -- BR-07: category must be one of the four types
    CONSTRAINT ck_reconciliation_category CHECK (category IN (
        'INVOICE',
        'ACCRUAL',
        'ACCRUAL_REVERSAL',
        'ALLOCATION'
    ))
);

CREATE INDEX idx_reconciliation_actual ON reconciliation(actual_id);
CREATE INDEX idx_reconciliation_milestone ON reconciliation(milestone_id);
CREATE INDEX idx_reconciliation_category ON reconciliation(category);
CREATE INDEX idx_reconciliation_date ON reconciliation(reconciled_at);

-- Index for accrual lifecycle queries: find open accruals per milestone
CREATE INDEX idx_reconciliation_accruals ON reconciliation(milestone_id, category)
    WHERE category IN ('ACCRUAL', 'ACCRUAL_REVERSAL');

COMMENT ON TABLE reconciliation IS 'Links SAP actual lines to planned milestones. One actual maps to at most one milestone.';
COMMENT ON COLUMN reconciliation.category IS 'Classification: INVOICE (final), ACCRUAL (provisional), ACCRUAL_REVERSAL (offsets accrual), ALLOCATION (indirect cost)';
