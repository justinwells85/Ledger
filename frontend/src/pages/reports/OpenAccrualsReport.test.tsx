/**
 * Tests for OpenAccrualsReport page component.
 * Spec: 09-reporting.md — Open Accruals Report
 */
import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../test/server';
import { renderWithProviders } from '../../test/renderWithProviders';
import OpenAccrualsReport from './OpenAccrualsReport';

describe('OpenAccrualsReport', () => {
  it('renders open accruals table', async () => {
    renderWithProviders(<OpenAccrualsReport />);

    await screen.findByText('January Sustainment');

    expect(screen.getByText('WARNING')).toBeInTheDocument();
    expect(screen.getByText('65')).toBeInTheDocument();
  });

  it('shows no alerts when no open accruals', async () => {
    server.use(
      http.get('/api/v1/reports/open-accruals', () =>
        HttpResponse.json({ fiscalYear: 'FY26', rows: [] })
      )
    );

    renderWithProviders(<OpenAccrualsReport />);

    await screen.findByText(/No open accruals/);
  });
});
