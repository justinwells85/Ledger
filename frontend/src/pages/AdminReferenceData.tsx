/**
 * Admin Reference Data Management page.
 * Spec: 18-admin-configuration.md Section 3, BR-91 through BR-97
 */
import { useState } from 'react';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import ApiError from '../components/ApiError';
import styles from './AdminReferenceData.module.css';

type TabType = 'FUNDING_SOURCE' | 'CONTRACT_STATUS' | 'PROJECT_STATUS' | 'RECONCILIATION_CATEGORY';

const TABS: { type: TabType; label: string }[] = [
  { type: 'FUNDING_SOURCE', label: 'Funding Sources' },
  { type: 'CONTRACT_STATUS', label: 'Contract Statuses' },
  { type: 'PROJECT_STATUS', label: 'Project Statuses' },
  { type: 'RECONCILIATION_CATEGORY', label: 'Reconciliation Categories' },
];

interface RefEntry {
  code: string;
  displayName: string;
  description: string | null;
  active: boolean;
  sortOrder: number;
  affectsAccrualLifecycle?: boolean;
}

export default function AdminReferenceData() {
  const [activeTab, setActiveTab] = useState<TabType>('FUNDING_SOURCE');
  return (
    <div className={styles.page}>
      <h2>Reference Data</h2>
      <div className={styles.tabs}>
        {TABS.map(tab => (
          <button
            key={tab.type}
            className={activeTab === tab.type ? `${styles.tab} ${styles.tabActive}` : styles.tab}
            onClick={() => setActiveTab(tab.type)}
          >
            {tab.label}
          </button>
        ))}
      </div>
      <TabContent type={activeTab} />
    </div>
  );
}

function TabContent({ type }: { type: TabType }) {
  const [refetchKey, setRefetchKey] = useState(0);
  const { data: entries, loading, error } = useApi<RefEntry[]>(
    `/admin/reference-data/${type}`, [type, refetchKey]
  );

  const [showForm, setShowForm] = useState(false);
  const [code, setCode] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [description, setDescription] = useState('');
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');

  if (error) return <ApiError message={error} />;

  async function create() {
    setSaving(true);
    setSaveError('');
    try {
      await api.post(`/admin/reference-data/${type}`, { code, displayName, description });
      setShowForm(false);
      setCode(''); setDisplayName(''); setDescription('');
      setRefetchKey(k => k + 1);
    } catch (err) {
      setSaveError(String(err));
    } finally {
      setSaving(false);
    }
  }

  async function toggleActive(entryCode: string) {
    try {
      await api.post(`/admin/reference-data/${type}/${entryCode}/toggle-active`, {});
      setRefetchKey(k => k + 1);
    } catch (err) {
      alert(String(err));
    }
  }

  return (
    <div className={styles.tabContent}>
      <div className={styles.toolbar}>
        <button className={styles.btnAdd} onClick={() => { setShowForm(true); setCode(''); setDisplayName(''); setDescription(''); setSaveError(''); }}>
          + Add
        </button>
      </div>

      {showForm && (
        <div className={styles.form}>
          <input className={styles.input} placeholder="CODE" value={code} onChange={e => setCode(e.target.value.toUpperCase())} />
          <input className={styles.input} placeholder="Display Name" value={displayName} onChange={e => setDisplayName(e.target.value)} />
          <input className={styles.input} placeholder="Description (optional)" value={description} onChange={e => setDescription(e.target.value)} />
          {saveError && <div className={styles.error}>{saveError}</div>}
          <div className={styles.formActions}>
            <button className={styles.btnCancel} onClick={() => setShowForm(false)}>Cancel</button>
            <button className={styles.btnCreate} disabled={saving} onClick={create}>
              {saving ? 'Saving…' : 'Save'}
            </button>
          </div>
        </div>
      )}

      {loading && <p>Loading…</p>}

      <table className={styles.table}>
        <thead>
          <tr>
            <th>Code</th>
            <th>Display Name</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {(entries ?? []).map(e => (
            <tr key={e.code} className={!e.active ? styles.inactiveRow : ''}>
              <td className={styles.mono}>{e.code}</td>
              <td>{e.displayName}</td>
              <td>{e.active ? 'Active' : 'Inactive'}</td>
              <td>
                <button
                  className={e.active ? styles.btnDeactivate : styles.btnReactivate}
                  onClick={() => toggleActive(e.code)}
                >
                  {e.active ? 'Deactivate' : 'Reactivate'}
                </button>
              </td>
            </tr>
          ))}
          {(entries ?? []).length === 0 && !loading && (
            <tr><td colSpan={4} className={styles.empty}>No entries found</td></tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
