import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../test/server';
import { renderWithProviders } from '../test/renderWithProviders';
import SapImport from './SapImport';

describe('SapImport', () => {
  it('renders import history table', async () => {
    renderWithProviders(<SapImport />);

    await screen.findByText('SAP_FY26_Jan.csv');

    expect(screen.getByText('COMMITTED')).toBeInTheDocument();
  });

  it('shows empty state when no imports', async () => {
    server.use(
      http.get('/api/v1/imports', () => HttpResponse.json([]))
    );

    renderWithProviders(<SapImport />);

    await screen.findByText('No import history');
  });
});
