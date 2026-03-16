/**
 * Tests for Layout component — admin nav visibility by role.
 * Spec: 18-admin-configuration.md, BR-82
 */
import { screen } from '@testing-library/react';
import { renderWithProviders } from '../test/renderWithProviders';
import Layout from './Layout';

function makeToken(payload: object): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
    .replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
  const body = btoa(JSON.stringify(payload))
    .replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
  return `${header}.${body}.fakesig`;
}

describe('Layout admin nav', () => {
  afterEach(() => {
    localStorage.removeItem('ledger_token');
  });

  it('shows Users nav link for ADMIN role', () => {
    localStorage.setItem('ledger_token', makeToken({ sub: 'admin', role: 'ADMIN', displayName: 'Admin' }));
    renderWithProviders(<Layout />);

    expect(screen.getByText('Users')).toBeInTheDocument();
  });

  it('hides Users nav link for ANALYST role', () => {
    localStorage.setItem('ledger_token', makeToken({ sub: 'alice', role: 'ANALYST', displayName: 'Alice' }));
    renderWithProviders(<Layout />);

    expect(screen.queryByText('Users')).not.toBeInTheDocument();
  });

  it('shows displayName and role badge for logged in user', () => {
    localStorage.setItem('ledger_token', makeToken({ sub: 'admin', role: 'ADMIN', displayName: 'System Admin' }));
    renderWithProviders(<Layout />);

    expect(screen.getByText('System Admin')).toBeInTheDocument();
    // ADMIN appears in role badge + admin nav section label
    expect(screen.getAllByText('ADMIN').length).toBeGreaterThan(0);
  });

  it('shows sign out button', () => {
    localStorage.setItem('ledger_token', makeToken({ sub: 'admin', role: 'ADMIN', displayName: 'Admin' }));
    renderWithProviders(<Layout />);

    expect(screen.getByRole('button', { name: 'Sign out' })).toBeInTheDocument();
  });
});
