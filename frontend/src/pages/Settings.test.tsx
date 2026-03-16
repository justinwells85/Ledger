/**
 * Tests for Settings page component — data-driven from system_config metadata.
 * Spec: 10-business-rules.md (BR-42), 18-admin-configuration.md Section 4 (BR-98–101)
 */
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../test/renderWithProviders';
import Settings from './Settings';

describe('Settings', () => {
  it('renders settings grouped by displayGroup with displayName labels', async () => {
    renderWithProviders(<Settings />);

    await screen.findByText('Tolerance (%)');

    expect(screen.getByText('Tolerance ($)')).toBeInTheDocument();
    expect(screen.getByText('Warning Threshold (days)')).toBeInTheDocument();
    expect(screen.getByText('Critical Threshold (days)')).toBeInTheDocument();
    expect(screen.getByText('RECONCILIATION TOLERANCE')).toBeInTheDocument();
    expect(screen.getByText('ACCRUAL AGING')).toBeInTheDocument();

    const inputs = screen.getAllByRole('textbox');
    const valueInputs = inputs.filter(
      (el) => (el as HTMLInputElement).value === '0.02'
    );
    expect(valueInputs.length).toBeGreaterThan(0);
  });

  it('shows error when saving without reason', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Settings />);

    await screen.findByText('Tolerance (%)');

    const saveButtons = screen.getAllByRole('button', { name: 'Save' });
    await user.click(saveButtons[0]);

    await screen.findByText('Reason is required');
  });

  it('saves config with reason', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Settings />);

    await screen.findByText('Tolerance (%)');

    // Find the reason input for the tolerance_percent row (first reason input)
    const reasonInputs = screen.getAllByPlaceholderText('Reason for change…');
    await user.type(reasonInputs[0], 'Adjusting tolerance per Q2 review');

    const saveButtons = screen.getAllByRole('button', { name: 'Save' });
    await user.click(saveButtons[0]);

    // A successful save clears the reason input — no network error should appear
    expect(screen.queryByText(/Error/i)).not.toBeInTheDocument();
  });
});
