/**
 * Tests for FundingReport (Funding Source Summary) page component.
 * Spec: 09-reporting.md — Funding Source Summary
 */
import { screen } from '@testing-library/react';
import { renderWithProviders } from '../../test/renderWithProviders';
import FundingReport from './FundingReport';

describe('FundingReport', () => {
  it('renders funding source rows aggregated from budget data', async () => {
    renderWithProviders(<FundingReport />);

    await screen.findByText('OPEX');

    // budgetReportFixture has one OPEX row with total $45,000 (appears in row + grand total)
    expect(screen.getAllByText('$45,000').length).toBeGreaterThan(0);
  });

  it('shows percentage of total for each funding source', async () => {
    renderWithProviders(<FundingReport />);

    await screen.findByText('OPEX');

    // Only OPEX in fixture → 100% of total
    expect(screen.getByText('100%')).toBeInTheDocument();
  });

  it('shows grand total row', async () => {
    renderWithProviders(<FundingReport />);

    await screen.findByText('OPEX');

    expect(screen.getByText('GRAND TOTAL')).toBeInTheDocument();
  });
});
