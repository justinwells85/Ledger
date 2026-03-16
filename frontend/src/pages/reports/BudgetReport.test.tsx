/**
 * Tests for BudgetReport page component.
 * Spec: 09-reporting.md — Budget Plan Report
 */
import { screen } from '@testing-library/react';
import { renderWithProviders } from '../../test/renderWithProviders';
import BudgetReport from './BudgetReport';

describe('BudgetReport', () => {
  it('renders budget report table with period columns', async () => {
    renderWithProviders(<BudgetReport />);

    await screen.findByText('Globant ADM');

    expect(screen.getByText('FY26-04-JAN')).toBeInTheDocument();
    expect(screen.getByText('FY26-05-FEB')).toBeInTheDocument();
    expect(screen.getAllByText('$45,000').length).toBeGreaterThan(0);
  });

  it('shows grand total row', async () => {
    renderWithProviders(<BudgetReport />);

    await screen.findByText('Globant ADM');

    expect(screen.getByText('GRAND TOTAL')).toBeInTheDocument();
  });
});
