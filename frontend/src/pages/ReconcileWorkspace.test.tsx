/**
 * Tests for ReconcileWorkspace page component.
 * Spec: 06-reconciliation.md — Reconciliation Workspace
 */
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../test/renderWithProviders';
import ReconcileWorkspace from './ReconcileWorkspace';

describe('ReconcileWorkspace', () => {
  it('renders unreconciled actuals', async () => {
    renderWithProviders(<ReconcileWorkspace />);

    await screen.findByText('Invoice March');

    // formatCurrency (no decimals) renders $25,000 for 25000
    expect(screen.getByText('$25,000')).toBeInTheDocument();
  });

  it('selecting an actual loads candidates', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ReconcileWorkspace />);

    await screen.findByText('Invoice March');

    await user.click(screen.getByText('Invoice March'));

    await screen.findByText('January Sustainment');
  });

  it('shows reconcile form when candidate is selected', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ReconcileWorkspace />);

    await screen.findByText('Invoice March');

    await user.click(screen.getByText('Invoice March'));

    await screen.findByText('January Sustainment');

    await user.click(screen.getByText('January Sustainment'));

    await screen.findByText(/Reconciling to/);
  });

  it('filters actuals by description text', async () => {
    // Spec: 06-reconciliation.md — actuals filter controls
    const user = userEvent.setup();
    renderWithProviders(<ReconcileWorkspace />);

    await screen.findByText('Invoice March');
    expect(screen.getByText('Reversal')).toBeInTheDocument();

    await user.type(screen.getByPlaceholderText('Vendor or description…'), 'Invoice');

    expect(screen.getByText('Invoice March')).toBeInTheDocument();
    expect(screen.queryByText('Reversal')).not.toBeInTheDocument();
  });

  it('filters actuals by amount sign', async () => {
    // Spec: 06-reconciliation.md — actuals filter controls
    const user = userEvent.setup();
    renderWithProviders(<ReconcileWorkspace />);

    await screen.findByText('Invoice March');

    await user.selectOptions(screen.getByLabelText(/amount filter/i), 'positive');

    expect(screen.getByText('Invoice March')).toBeInTheDocument();
    expect(screen.queryByText('Reversal')).not.toBeInTheDocument();
  });

  it('shows recently matched section with undo button after reconciliation', async () => {
    // Spec: 06-reconciliation.md — undo reconciliation
    const user = userEvent.setup();
    renderWithProviders(<ReconcileWorkspace />);

    await screen.findByText('Invoice March');
    await user.click(screen.getByText('Invoice March'));
    await screen.findByText('January Sustainment');
    await user.click(screen.getByText('January Sustainment'));
    await screen.findByText(/Reconciling to/);
    await user.click(screen.getByText('Reconcile'));

    await screen.findByText('MATCHED THIS SESSION');
    // zoneLabel is a div; .parentElement is the matchedZone container
    const matchedZone = screen.getByText('MATCHED THIS SESSION').parentElement!;
    expect(within(matchedZone).getByText('Invoice March')).toBeInTheDocument();
    expect(within(matchedZone).getByText('January Sustainment')).toBeInTheDocument();
    expect(screen.getByText('Undo')).toBeInTheDocument();
  });
});
