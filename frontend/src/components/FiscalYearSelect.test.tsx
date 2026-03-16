import { render, screen } from '@testing-library/react';
import { server } from '../test/server';
import { http, HttpResponse } from 'msw';
import FiscalYearSelect from './FiscalYearSelect';

test('renders fiscal year options from API', async () => {
  server.use(
    http.get('/api/v1/fiscal-years', () =>
      HttpResponse.json([{ fiscalYear: 'FY26' }, { fiscalYear: 'FY25' }])
    )
  );
  render(<FiscalYearSelect value="FY26" onChange={() => {}} />);
  expect(await screen.findByRole('option', { name: 'FY26' })).toBeInTheDocument();
  expect(await screen.findByRole('option', { name: 'FY25' })).toBeInTheDocument();
});
