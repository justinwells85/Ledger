-- ============================================================================
-- V007: Entity Audit Log and System Configuration
-- Spec: 11-change-management.md, 10-business-rules.md (BR-30 through BR-32)
-- ============================================================================

-- ─────────────────────────────────────────────
-- Audit Log (non-financial entity changes)
-- ─────────────────────────────────────────────

CREATE TABLE audit_log (
    audit_id        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type     VARCHAR(30)  NOT NULL,           -- CONTRACT, PROJECT, CONFIGURATION
    entity_id       VARCHAR(100) NOT NULL,            -- ID of the changed entity (varchar to handle both UUID and string PKs)
    action          VARCHAR(30)  NOT NULL,            -- CREATE, UPDATE, STATUS_CHANGE
    changes         JSONB,                            -- {"field": {"old": "value", "new": "value"}, ...}
    reason          TEXT,                             -- required for updates and status changes
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      VARCHAR(100) NOT NULL,

    CONSTRAINT ck_audit_log_action CHECK (action IN ('CREATE', 'UPDATE', 'STATUS_CHANGE'))
);

CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_date ON audit_log(created_at);
CREATE INDEX idx_audit_log_user ON audit_log(created_by);

-- ─────────────────────────────────────────────
-- System Configuration
-- ─────────────────────────────────────────────

CREATE TABLE system_config (
    config_key      VARCHAR(100) PRIMARY KEY,
    config_value    VARCHAR(500) NOT NULL,
    description     VARCHAR(500),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by      VARCHAR(100) NOT NULL
);

-- Default configuration values (BR-31, BR-32)
INSERT INTO system_config (config_key, config_value, description, updated_by) VALUES
    ('tolerance_percent', '0.02', 'Reconciliation tolerance as decimal (0.02 = 2%)', 'SYSTEM'),
    ('tolerance_absolute', '50.00', 'Reconciliation tolerance as absolute dollar amount', 'SYSTEM'),
    ('accrual_aging_warning_days', '60', 'Days after which an open accrual triggers a warning', 'SYSTEM'),
    ('accrual_aging_critical_days', '90', 'Days after which an open accrual triggers a critical alert', 'SYSTEM');

COMMENT ON TABLE audit_log IS 'Non-financial entity change log. Financial changes tracked in journal_entry/journal_line.';
COMMENT ON TABLE system_config IS 'System-level configuration values (tolerance thresholds, aging periods, etc.)';
