/**
 * Tests for VarianceReport page component.
 * Spec: 09-reporting.md — Variance Report
 */
import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../test/server';
import { renderWithProviders } from '../../test/renderWithProviders';
import VarianceReport from './VarianceReport';

describe('VarianceReport', () => {
  it('renders variance report rows', async () => {
    renderWithProviders(<VarianceReport />);

    // Both rows have 'Globant ADM' — use findAllByText to accept multiple matches
    await screen.findAllByText('Globant ADM');

    // StatusBadge renders status with spaces replacing underscores
    expect(screen.getByText('UNDER BUDGET')).toBeInTheDocument();
    expect(screen.getByText('OVER BUDGET')).toBeInTheDocument();
  });

  it('shows empty state for different fiscal year', async () => {
    server.use(
      http.get('/api/v1/reports/variance', () =>
        HttpResponse.json({ fiscalYear: 'FY26', rows: [] })
      )
    );

    renderWithProviders(<VarianceReport />);

    await screen.findByText(/No data for/);
  });
});
