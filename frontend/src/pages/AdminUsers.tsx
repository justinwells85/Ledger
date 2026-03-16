/**
 * Admin User Management page.
 * Spec: 18-admin-configuration.md Section 1, BR-80 through BR-84
 */
import { useState } from 'react';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { User } from '../api/types';
import ApiError from '../components/ApiError';
import Drawer from '../components/Drawer';
import FormField from '../components/FormField';
import styles from './AdminUsers.module.css';

const ROLES = ['ADMIN', 'FINANCE_MANAGER', 'ANALYST', 'READ_ONLY'];

export default function AdminUsers() {
  const [refetchKey, setRefetchKey] = useState(0);
  const { data: users, loading, error } = useApi<User[]>('/admin/users', [refetchKey]);
  if (error) return <ApiError message={error} />;

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [username, setUsername] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('ANALYST');
  const [password, setPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');

  function openDrawer() {
    setUsername(''); setDisplayName(''); setEmail('');
    setRole('ANALYST'); setPassword('');
    setFieldErrors({}); setSaveError('');
    setDrawerOpen(true);
  }

  async function createUser() {
    const e: Record<string, string> = {};
    if (!username.trim()) e.username = 'Username is required';
    if (!displayName.trim()) e.displayName = 'Display name is required';
    if (!email.trim()) e.email = 'Email is required';
    if (!password.trim()) e.password = 'Password is required';
    if (Object.keys(e).length > 0) { setFieldErrors(e); return; }
    setSaving(true); setSaveError('');
    try {
      await api.post('/admin/users', { username, displayName, email, role, password });
      setDrawerOpen(false);
      setRefetchKey(k => k + 1);
    } catch (err) {
      setSaveError(String(err));
    } finally {
      setSaving(false);
    }
  }

  async function toggleActive(user: User) {
    const path = user.active
      ? `/admin/users/${user.userId}/deactivate`
      : `/admin/users/${user.userId}/reactivate`;
    await api.post(path, {});
    setRefetchKey(k => k + 1);
  }

  return (
    <div className={styles.page}>
      <div className={styles.toolbar}>
        <h2>User Management</h2>
        <button className={styles.btnNew} onClick={openDrawer}>+ New User</button>
      </div>

      {loading && <p>Loading…</p>}

      <div className={styles.tableWrap}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Username</th>
              <th>Display Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {(users ?? []).map(u => (
              <tr key={u.userId} className={!u.active ? styles.inactiveRow : ''}>
                <td>{u.username}</td>
                <td>{u.displayName}</td>
                <td>{u.email}</td>
                <td><span className={`${styles.roleBadge} ${styles[u.role.toLowerCase()]}`}>{u.role}</span></td>
                <td>{u.active ? 'Active' : 'Inactive'}</td>
                <td>
                  <button
                    className={u.active ? styles.btnDeactivate : styles.btnReactivate}
                    onClick={() => toggleActive(u)}
                  >
                    {u.active ? 'Deactivate' : 'Reactivate'}
                  </button>
                </td>
              </tr>
            ))}
            {(users ?? []).length === 0 && !loading && (
              <tr><td colSpan={6} className={styles.empty}>No users found</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <Drawer open={drawerOpen} title="New User" onClose={() => setDrawerOpen(false)}>
        <FormField
          label="Username"
          required
          error={fieldErrors.username}
          value={username}
          onChange={e => setUsername((e.target as HTMLInputElement).value)}
        />
        <FormField
          label="Display Name"
          required
          error={fieldErrors.displayName}
          value={displayName}
          onChange={e => setDisplayName((e.target as HTMLInputElement).value)}
        />
        <FormField
          label="Email"
          required
          type="email"
          error={fieldErrors.email}
          value={email}
          onChange={e => setEmail((e.target as HTMLInputElement).value)}
        />
        <FormField
          label="Role"
          as="select"
          value={role}
          onChange={e => setRole((e.target as HTMLSelectElement).value)}
        >
          {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
        </FormField>
        <FormField
          label="Password"
          required
          type="password"
          error={fieldErrors.password}
          value={password}
          onChange={e => setPassword((e.target as HTMLInputElement).value)}
        />
        {saveError && <div className={styles.saveError}>{saveError}</div>}
        <div className={styles.drawerActions}>
          <button className={styles.btnCancel} onClick={() => setDrawerOpen(false)}>Cancel</button>
          <button className={styles.btnCreate} disabled={saving} onClick={createUser}>
            {saving ? 'Creating…' : 'Create User'}
          </button>
        </div>
      </Drawer>
    </div>
  );
}
