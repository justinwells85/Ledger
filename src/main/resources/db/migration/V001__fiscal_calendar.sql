-- ============================================================================
-- V001: Fiscal Calendar
-- Spec: 03-fiscal-calendar.md
-- ============================================================================

CREATE TABLE fiscal_year (
    fiscal_year     VARCHAR(10)  PRIMARY KEY,  -- e.g., 'FY26'
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_fiscal_year_dates CHECK (end_date > start_date)
);

CREATE TABLE fiscal_period (
    period_id       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    fiscal_year     VARCHAR(10)  NOT NULL REFERENCES fiscal_year(fiscal_year),
    period_key      VARCHAR(20)  NOT NULL UNIQUE,   -- e.g., 'FY26-01-OCT'
    quarter         VARCHAR(2)   NOT NULL,           -- Q1, Q2, Q3, Q4
    calendar_month  DATE         NOT NULL,           -- first day of calendar month, e.g., 2025-10-01
    display_name    VARCHAR(50)  NOT NULL,           -- e.g., 'October 2025'
    sort_order      INT          NOT NULL,           -- 1-12 within fiscal year

    CONSTRAINT ck_fiscal_period_quarter CHECK (quarter IN ('Q1', 'Q2', 'Q3', 'Q4')),
    CONSTRAINT ck_fiscal_period_sort    CHECK (sort_order BETWEEN 1 AND 12),
    CONSTRAINT uq_fiscal_period_year_sort UNIQUE (fiscal_year, sort_order)
);

CREATE INDEX idx_fiscal_period_year ON fiscal_period(fiscal_year);
CREATE INDEX idx_fiscal_period_calendar_month ON fiscal_period(calendar_month);

COMMENT ON TABLE fiscal_year IS 'Disney fiscal years (October through September)';
COMMENT ON TABLE fiscal_period IS 'Monthly periods within a fiscal year';
