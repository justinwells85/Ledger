/**
 * Tests for ReconciliationReport page component.
 * Spec: 09-reporting.md — Reconciliation Status Report
 */
import { screen } from '@testing-library/react';
import { renderWithProviders } from '../../test/renderWithProviders';
import ReconciliationReport from './ReconciliationReport';

describe('ReconciliationReport', () => {
  it('renders reconciliation report rows', async () => {
    renderWithProviders(<ReconciliationReport />);

    await screen.findByText('January Sustainment');

    // StatusBadge and select options both render status with spaces replacing underscores
    expect(screen.getAllByText('PARTIALLY MATCHED').length).toBeGreaterThan(0);
    expect(screen.getAllByText('FULLY RECONCILED').length).toBeGreaterThan(0);
  });
});
