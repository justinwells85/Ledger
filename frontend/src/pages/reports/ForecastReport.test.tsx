/**
 * Tests for ForecastReport page component.
 * Spec: 09-reporting.md — Forecast Report
 */
import { screen } from '@testing-library/react';
import { renderWithProviders } from '../../test/renderWithProviders';
import ForecastReport from './ForecastReport';

describe('ForecastReport', () => {
  it('renders project rows with planned and actuals columns', async () => {
    renderWithProviders(<ForecastReport />);

    // projectName div renders "{projectId} {projectName}" so use regex
    await screen.findByText(/DPI Photopass/);

    // Planned: $25,000 for DPI Photopass
    expect(screen.getAllByText('$25,000').length).toBeGreaterThan(0);
    // Actuals YTD: $15,000 for DPI Photopass
    expect(screen.getByText('$15,000')).toBeInTheDocument();
  });

  it('shows Remaining column with correct value', async () => {
    renderWithProviders(<ForecastReport />);

    await screen.findByText(/DPI Photopass/);

    expect(screen.getByText('Remaining')).toBeInTheDocument();
    // DPI Photopass: 25000 - 15000 = 10000 remaining (appears in row + total row)
    expect(screen.getAllByText('$10,000').length).toBeGreaterThan(0);
  });

  it('shows total remaining row', async () => {
    renderWithProviders(<ForecastReport />);

    await screen.findByText(/DPI Photopass/);

    expect(screen.getByText('TOTAL REMAINING')).toBeInTheDocument();
  });
});
