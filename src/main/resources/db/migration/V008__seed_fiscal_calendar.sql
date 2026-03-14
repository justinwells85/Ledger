-- ============================================================================
-- V008: Seed Data — Fiscal Calendar FY25, FY26, FY27
-- Spec: 03-fiscal-calendar.md
-- Provides 3 fiscal years for multi-year support and historical context
-- ============================================================================

-- ─────────────────────────────────────────────
-- Fiscal Years
-- ─────────────────────────────────────────────

INSERT INTO fiscal_year (fiscal_year, start_date, end_date) VALUES
    ('FY25', '2024-10-01', '2025-09-30'),
    ('FY26', '2025-10-01', '2026-09-30'),
    ('FY27', '2026-10-01', '2027-09-30');

-- ─────────────────────────────────────────────
-- FY25 Periods
-- ─────────────────────────────────────────────

INSERT INTO fiscal_period (fiscal_year, period_key, quarter, calendar_month, display_name, sort_order) VALUES
    ('FY25', 'FY25-01-OCT', 'Q1', '2024-10-01', 'October 2024',   1),
    ('FY25', 'FY25-02-NOV', 'Q1', '2024-11-01', 'November 2024',  2),
    ('FY25', 'FY25-03-DEC', 'Q1', '2024-12-01', 'December 2024',  3),
    ('FY25', 'FY25-04-JAN', 'Q2', '2025-01-01', 'January 2025',   4),
    ('FY25', 'FY25-05-FEB', 'Q2', '2025-02-01', 'February 2025',  5),
    ('FY25', 'FY25-06-MAR', 'Q2', '2025-03-01', 'March 2025',     6),
    ('FY25', 'FY25-07-APR', 'Q3', '2025-04-01', 'April 2025',     7),
    ('FY25', 'FY25-08-MAY', 'Q3', '2025-05-01', 'May 2025',       8),
    ('FY25', 'FY25-09-JUN', 'Q3', '2025-06-01', 'June 2025',      9),
    ('FY25', 'FY25-10-JUL', 'Q4', '2025-07-01', 'July 2025',      10),
    ('FY25', 'FY25-11-AUG', 'Q4', '2025-08-01', 'August 2025',    11),
    ('FY25', 'FY25-12-SEP', 'Q4', '2025-09-01', 'September 2025', 12);

-- ─────────────────────────────────────────────
-- FY26 Periods
-- ─────────────────────────────────────────────

INSERT INTO fiscal_period (fiscal_year, period_key, quarter, calendar_month, display_name, sort_order) VALUES
    ('FY26', 'FY26-01-OCT', 'Q1', '2025-10-01', 'October 2025',   1),
    ('FY26', 'FY26-02-NOV', 'Q1', '2025-11-01', 'November 2025',  2),
    ('FY26', 'FY26-03-DEC', 'Q1', '2025-12-01', 'December 2025',  3),
    ('FY26', 'FY26-04-JAN', 'Q2', '2026-01-01', 'January 2026',   4),
    ('FY26', 'FY26-05-FEB', 'Q2', '2026-02-01', 'February 2026',  5),
    ('FY26', 'FY26-06-MAR', 'Q2', '2026-03-01', 'March 2026',     6),
    ('FY26', 'FY26-07-APR', 'Q3', '2026-04-01', 'April 2026',     7),
    ('FY26', 'FY26-08-MAY', 'Q3', '2026-05-01', 'May 2026',       8),
    ('FY26', 'FY26-09-JUN', 'Q3', '2026-06-01', 'June 2026',      9),
    ('FY26', 'FY26-10-JUL', 'Q4', '2026-07-01', 'July 2026',      10),
    ('FY26', 'FY26-11-AUG', 'Q4', '2026-08-01', 'August 2026',    11),
    ('FY26', 'FY26-12-SEP', 'Q4', '2026-09-01', 'September 2026', 12);

-- ─────────────────────────────────────────────
-- FY27 Periods
-- ─────────────────────────────────────────────

INSERT INTO fiscal_period (fiscal_year, period_key, quarter, calendar_month, display_name, sort_order) VALUES
    ('FY27', 'FY27-01-OCT', 'Q1', '2026-10-01', 'October 2026',   1),
    ('FY27', 'FY27-02-NOV', 'Q1', '2026-11-01', 'November 2026',  2),
    ('FY27', 'FY27-03-DEC', 'Q1', '2026-12-01', 'December 2026',  3),
    ('FY27', 'FY27-04-JAN', 'Q2', '2027-01-01', 'January 2027',   4),
    ('FY27', 'FY27-05-FEB', 'Q2', '2027-02-01', 'February 2027',  5),
    ('FY27', 'FY27-06-MAR', 'Q2', '2027-03-01', 'March 2027',     6),
    ('FY27', 'FY27-07-APR', 'Q3', '2027-04-01', 'April 2027',     7),
    ('FY27', 'FY27-08-MAY', 'Q3', '2027-05-01', 'May 2027',       8),
    ('FY27', 'FY27-09-JUN', 'Q3', '2027-06-01', 'June 2027',      9),
    ('FY27', 'FY27-10-JUL', 'Q4', '2027-07-01', 'July 2027',      10),
    ('FY27', 'FY27-11-AUG', 'Q4', '2027-08-01', 'August 2027',    11),
    ('FY27', 'FY27-12-SEP', 'Q4', '2027-09-01', 'September 2027', 12);
