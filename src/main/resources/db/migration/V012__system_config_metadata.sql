-- ============================================================================
-- V012: System Config Metadata for Data-Driven Settings UI
-- Spec: 18-admin-configuration.md Section 4, BR-98 through BR-101
-- Adds data_type, display_group, display_name, display_order to system_config
-- ============================================================================

ALTER TABLE system_config
    ADD COLUMN data_type      VARCHAR(20)   NOT NULL DEFAULT 'TEXT',
    ADD COLUMN display_group  VARCHAR(100)  NOT NULL DEFAULT 'GENERAL',
    ADD COLUMN display_name   VARCHAR(200)  NOT NULL DEFAULT '',
    ADD COLUMN display_order  INTEGER       NOT NULL DEFAULT 0;

-- Populate metadata for the existing four config keys
UPDATE system_config SET
    data_type     = 'PERCENTAGE',
    display_group = 'RECONCILIATION TOLERANCE',
    display_name  = 'Tolerance (%)',
    display_order = 1
WHERE config_key = 'tolerance_percent';

UPDATE system_config SET
    data_type     = 'DECIMAL',
    display_group = 'RECONCILIATION TOLERANCE',
    display_name  = 'Tolerance ($)',
    display_order = 2
WHERE config_key = 'tolerance_absolute';

UPDATE system_config SET
    data_type     = 'INTEGER',
    display_group = 'ACCRUAL AGING',
    display_name  = 'Warning Threshold (days)',
    display_order = 1
WHERE config_key = 'accrual_aging_warning_days';

UPDATE system_config SET
    data_type     = 'INTEGER',
    display_group = 'ACCRUAL AGING',
    display_name  = 'Critical Threshold (days)',
    display_order = 2
WHERE config_key = 'accrual_aging_critical_days';
