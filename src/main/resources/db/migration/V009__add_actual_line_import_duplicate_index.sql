-- ARCH-12: Add composite index to accelerate duplicate-detection queries
-- Spec: 05-sap-ingestion.md BR-08
CREATE INDEX IF NOT EXISTS idx_actual_line_import_dup
    ON actual_line(import_id, is_duplicate);
