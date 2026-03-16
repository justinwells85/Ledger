/**
 * Tests for AdminUsers page.
 * Spec: 18-admin-configuration.md Section 1, BR-80 through BR-84
 */
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../test/renderWithProviders';
import AdminUsers from './AdminUsers';

describe('AdminUsers', () => {
  it('renders user list with usernames and roles', async () => {
    renderWithProviders(<AdminUsers />);

    await screen.findByText('System Admin');
    expect(screen.getByText('alice')).toBeInTheDocument();
    expect(screen.getAllByText('ADMIN').length).toBeGreaterThan(0);
    expect(screen.getByText('ANALYST')).toBeInTheDocument();
  });

  it('shows + New User button', async () => {
    renderWithProviders(<AdminUsers />);

    await screen.findByText('System Admin');

    expect(screen.getByText('+ New User')).toBeInTheDocument();
  });

  it('clicking + New User opens drawer with form fields', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AdminUsers />);

    await screen.findByText('System Admin');

    await user.click(screen.getByText('+ New User'));

    expect(screen.getByRole('heading', { name: 'New User' })).toBeInTheDocument();
    expect(screen.getByText('Username', { selector: 'label, label *' })).toBeInTheDocument();
    expect(screen.getByText('Password', { selector: 'label, label *' })).toBeInTheDocument();
  });

  it('submitting New User without username shows error', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AdminUsers />);

    await screen.findByText('System Admin');

    await user.click(screen.getByText('+ New User'));
    await user.click(screen.getByText('Create User'));

    expect(screen.getByText('Username is required')).toBeInTheDocument();
  });
});
