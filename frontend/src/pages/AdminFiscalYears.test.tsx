/**
 * Tests for AdminFiscalYears page.
 * Spec: 18-admin-configuration.md Section 2, BR-85 through BR-90
 */
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../test/renderWithProviders';
import AdminFiscalYears from './AdminFiscalYears';

describe('AdminFiscalYears', () => {
  it('renders list of existing fiscal years', async () => {
    renderWithProviders(<AdminFiscalYears />);

    await screen.findByText('FY26');
    expect(screen.getByText('2025-10-01')).toBeInTheDocument();
    expect(screen.getByText('2026-09-30')).toBeInTheDocument();
  });

  it('shows + New Fiscal Year button', async () => {
    renderWithProviders(<AdminFiscalYears />);

    await screen.findByText('FY26');
    expect(screen.getByText('+ New Fiscal Year')).toBeInTheDocument();
  });

  it('clicking + New Fiscal Year shows input form', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AdminFiscalYears />);

    await screen.findByText('FY26');
    await user.click(screen.getByText('+ New Fiscal Year'));

    expect(screen.getByPlaceholderText('e.g. FY28')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Create' })).toBeInTheDocument();
  });

  it('submitting the form closes the form on success', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AdminFiscalYears />);

    await screen.findByText('FY26');
    await user.click(screen.getByText('+ New Fiscal Year'));
    await user.type(screen.getByPlaceholderText('e.g. FY28'), 'FY28');
    await user.click(screen.getByRole('button', { name: 'Create' }));

    // Form closes after successful POST
    await screen.findByText('FY26'); // list still present
    expect(screen.queryByPlaceholderText('e.g. FY28')).not.toBeInTheDocument();
  });
});
