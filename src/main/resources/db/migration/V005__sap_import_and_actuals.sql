-- ============================================================================
-- V005: SAP Import and Actual Lines
-- Spec: 05-sap-ingestion.md
-- ============================================================================

CREATE TABLE sap_import (
    import_id       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    filename        VARCHAR(500) NOT NULL,
    imported_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    imported_by     VARCHAR(100) NOT NULL,
    fiscal_year     VARCHAR(10),                     -- optional, informational
    status          VARCHAR(20)  NOT NULL DEFAULT 'STAGED',
    total_lines     INT          NOT NULL DEFAULT 0,
    new_lines       INT          NOT NULL DEFAULT 0,
    duplicate_lines INT          NOT NULL DEFAULT 0,
    error_lines     INT          NOT NULL DEFAULT 0,

    CONSTRAINT ck_sap_import_status CHECK (status IN ('STAGED', 'COMMITTED', 'REJECTED'))
);

CREATE INDEX idx_sap_import_status ON sap_import(status);
CREATE INDEX idx_sap_import_date ON sap_import(imported_at);

CREATE TABLE actual_line (
    actual_id             UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    import_id             UUID           NOT NULL REFERENCES sap_import(import_id),
    sap_document_number   VARCHAR(50),                -- sparse
    posting_date          DATE           NOT NULL,
    fiscal_period_id      UUID           REFERENCES fiscal_period(period_id),  -- derived from posting_date; nullable if period unresolved
    amount                DECIMAL(15,2)  NOT NULL,
    vendor_name           VARCHAR(300),               -- sparse
    cost_center           VARCHAR(50),                -- sparse
    wbse                  VARCHAR(50),                -- sparse
    gl_account            VARCHAR(50),                -- sparse
    description           TEXT,                       -- free text from SAP
    line_hash             VARCHAR(64)    NOT NULL,    -- SHA-256 for dedup
    is_duplicate          BOOLEAN        NOT NULL DEFAULT false,
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT uq_actual_line_hash_non_dup UNIQUE (line_hash) DEFERRABLE INITIALLY DEFERRED
);

-- Note on the unique constraint: The hash uniqueness constraint prevents two non-duplicate
-- lines from having the same hash. When is_duplicate = true, the line is kept for audit
-- but doesn't participate in financial calculations. The DEFERRABLE allows the import
-- pipeline to stage duplicates before marking them.
--
-- Actually, we need to allow duplicate hashes when is_duplicate = true.
-- Replace the simple UNIQUE with a partial unique index:
ALTER TABLE actual_line DROP CONSTRAINT uq_actual_line_hash_non_dup;

CREATE UNIQUE INDEX uq_actual_line_hash_active
    ON actual_line(line_hash)
    WHERE is_duplicate = false;

CREATE INDEX idx_actual_line_import ON actual_line(import_id);
CREATE INDEX idx_actual_line_posting ON actual_line(posting_date);
CREATE INDEX idx_actual_line_period ON actual_line(fiscal_period_id);
CREATE INDEX idx_actual_line_hash ON actual_line(line_hash);
CREATE INDEX idx_actual_line_wbse ON actual_line(wbse);
CREATE INDEX idx_actual_line_vendor ON actual_line(vendor_name);
CREATE INDEX idx_actual_line_created ON actual_line(created_at);
CREATE INDEX idx_actual_line_not_dup ON actual_line(is_duplicate) WHERE is_duplicate = false;

COMMENT ON TABLE sap_import IS 'Record of each SAP file upload. Tracks staging → commit/reject workflow.';
COMMENT ON TABLE actual_line IS 'Individual line items from SAP exports. Deduplicated by line_hash.';
COMMENT ON COLUMN actual_line.line_hash IS 'SHA-256 of all normalized SAP fields. Used for dedup across imports.';
COMMENT ON COLUMN actual_line.is_duplicate IS 'True if this line matched an existing active line on import. Not used in financial calculations.';
