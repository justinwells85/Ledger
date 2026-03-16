/**
 * Tests for AdminReferenceData page.
 * Spec: 18-admin-configuration.md Section 3, BR-91 through BR-97
 */
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../test/renderWithProviders';
import AdminReferenceData from './AdminReferenceData';

describe('AdminReferenceData', () => {
  it('renders tabs for each reference data type', async () => {
    renderWithProviders(<AdminReferenceData />);

    expect(screen.getByText('Funding Sources')).toBeInTheDocument();
    expect(screen.getByText('Contract Statuses')).toBeInTheDocument();
    expect(screen.getByText('Project Statuses')).toBeInTheDocument();
    expect(screen.getByText('Reconciliation Categories')).toBeInTheDocument();
  });

  it('shows funding sources by default', async () => {
    renderWithProviders(<AdminReferenceData />);

    await screen.findByText('OPEX');
    expect(screen.getByText('CAPEX')).toBeInTheDocument();
  });

  it('clicking Contract Statuses tab shows contract statuses', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AdminReferenceData />);

    await screen.findByText('OPEX');
    await user.click(screen.getByText('Contract Statuses'));

    await screen.findByText('ACTIVE');
    expect(screen.getByText('CLOSED')).toBeInTheDocument();
  });

  it('shows + Add button in active tab', async () => {
    renderWithProviders(<AdminReferenceData />);

    await screen.findByText('OPEX');
    expect(screen.getByText('+ Add')).toBeInTheDocument();
  });
});
