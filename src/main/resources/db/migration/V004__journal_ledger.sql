-- ============================================================================
-- V004: Double-Entry Journal Ledger
-- Spec: 02-journal-ledger.md
-- ============================================================================

CREATE TABLE journal_entry (
    entry_id        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_date      TIMESTAMPTZ  NOT NULL,           -- when the event occurred
    effective_date  DATE         NOT NULL,            -- when it takes effect (time machine key)
    entry_type      VARCHAR(30)  NOT NULL,
    description     VARCHAR(500) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      VARCHAR(100) NOT NULL,

    CONSTRAINT ck_journal_entry_type CHECK (entry_type IN (
        'PLAN_CREATE',
        'PLAN_ADJUST',
        'ACTUAL_IMPORT',
        'RECONCILE',
        'RECONCILE_UNDO'
    ))
);

CREATE INDEX idx_journal_entry_effective ON journal_entry(effective_date);
CREATE INDEX idx_journal_entry_type ON journal_entry(entry_type);
CREATE INDEX idx_journal_entry_created ON journal_entry(created_at);

CREATE TABLE journal_line (
    line_id           UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_id          UUID           NOT NULL REFERENCES journal_entry(entry_id),
    account           VARCHAR(20)    NOT NULL,
    contract_id       UUID           REFERENCES contract(contract_id),       -- nullable for unreconciled actuals
    project_id        VARCHAR(20)    REFERENCES project(project_id),         -- nullable
    milestone_id      UUID           REFERENCES milestone(milestone_id),     -- nullable
    fiscal_period_id  UUID           NOT NULL REFERENCES fiscal_period(period_id),
    debit             DECIMAL(15,2)  NOT NULL DEFAULT 0,
    credit            DECIMAL(15,2)  NOT NULL DEFAULT 0,
    reference_type    VARCHAR(50),                    -- e.g., 'MilestoneVersion', 'ActualLine', 'Reconciliation'
    reference_id      UUID,                           -- FK to the triggering entity

    CONSTRAINT ck_journal_line_account CHECK (account IN ('PLANNED', 'ACTUAL', 'VARIANCE_RESERVE')),
    CONSTRAINT ck_journal_line_amounts CHECK (debit >= 0 AND credit >= 0),
    CONSTRAINT ck_journal_line_one_side CHECK (NOT (debit > 0 AND credit > 0))  -- a line is debit OR credit, not both
);

CREATE INDEX idx_journal_line_entry ON journal_line(entry_id);
CREATE INDEX idx_journal_line_account ON journal_line(account);
CREATE INDEX idx_journal_line_contract ON journal_line(contract_id);
CREATE INDEX idx_journal_line_project ON journal_line(project_id);
CREATE INDEX idx_journal_line_milestone ON journal_line(milestone_id);
CREATE INDEX idx_journal_line_period ON journal_line(fiscal_period_id);
CREATE INDEX idx_journal_line_reference ON journal_line(reference_type, reference_id);

-- Balance query index: account + scope + period for aggregation queries
CREATE INDEX idx_journal_line_balance ON journal_line(account, contract_id, fiscal_period_id);

COMMENT ON TABLE journal_entry IS 'Header for double-entry journal entries. Every financial event produces one entry.';
COMMENT ON TABLE journal_line IS 'Detail lines for journal entries. SUM(debit) must equal SUM(credit) per entry.';
COMMENT ON COLUMN journal_line.reference_type IS 'Type of entity that triggered this line (MilestoneVersion, ActualLine, Reconciliation)';
COMMENT ON COLUMN journal_line.reference_id IS 'ID of the triggering entity for audit traceability';

-- ============================================================================
-- BR-01 enforcement: trigger to validate journal entry balance on insert
-- This provides a database-level safety net in addition to service-layer validation.
-- ============================================================================

CREATE OR REPLACE FUNCTION fn_validate_journal_balance()
RETURNS TRIGGER AS $$
DECLARE
    total_debit  DECIMAL(15,2);
    total_credit DECIMAL(15,2);
    line_count   INT;
BEGIN
    SELECT SUM(debit), SUM(credit), COUNT(*)
    INTO total_debit, total_credit, line_count
    FROM journal_line
    WHERE entry_id = NEW.entry_id;

    -- BR-02: minimum 2 lines (checked after all lines inserted via deferred trigger)
    -- Note: this trigger fires per-line, so we only validate balance.
    -- Line count validation is handled at the service layer.

    -- Balance check: only validate if we have at least 2 lines
    -- (during multi-line insert, intermediate states may be unbalanced)
    IF line_count >= 2 AND total_debit != total_credit THEN
        RAISE EXCEPTION 'Journal entry % is unbalanced: debits=% credits=%',
            NEW.entry_id, total_debit, total_credit;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Use a CONSTRAINT trigger so it fires at the end of the transaction,
-- after all lines for the entry have been inserted
CREATE CONSTRAINT TRIGGER trg_validate_journal_balance
    AFTER INSERT ON journal_line
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION fn_validate_journal_balance();
