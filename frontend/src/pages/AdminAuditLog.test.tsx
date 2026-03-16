/**
 * Tests for AdminAuditLog page.
 * Spec: 18-admin-configuration.md Section 5
 */
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../test/renderWithProviders';
import AdminAuditLog from './AdminAuditLog';

describe('AdminAuditLog', () => {
  it('renders audit log table with entries', async () => {
    renderWithProviders(<AdminAuditLog />);

    await screen.findByText('CONTRACT');
    expect(screen.getAllByText('CREATE').length).toBeGreaterThan(0);
    expect(screen.getByText('alice')).toBeInTheDocument();
  });

  it('shows filter controls', async () => {
    renderWithProviders(<AdminAuditLog />);

    await screen.findByText('CONTRACT');
    expect(screen.getByPlaceholderText('Entity ID')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('User')).toBeInTheDocument();
  });

  it('shows Export CSV button', async () => {
    renderWithProviders(<AdminAuditLog />);

    await screen.findByText('CONTRACT');
    expect(screen.getByText('Export CSV')).toBeInTheDocument();
  });

  it('filters by user when typed', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AdminAuditLog />);

    await screen.findByText('CONTRACT');
    const userInput = screen.getByPlaceholderText('User');
    await user.type(userInput, 'alice');

    // Component re-fetches; verify input was accepted
    expect(userInput).toHaveValue('alice');
  });
});
