/**
 * Admin Fiscal Year Management page.
 * Spec: 18-admin-configuration.md Section 2, BR-85 through BR-90
 */
import { useState } from 'react';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import ApiError from '../components/ApiError';
import styles from './AdminFiscalYears.module.css';

interface FiscalYear {
  fiscalYear: string;
  startDate: string;
  endDate: string;
}

export default function AdminFiscalYears() {
  const [refetchKey, setRefetchKey] = useState(0);
  const { data: fiscalYears, loading, error } = useApi<FiscalYear[]>('/fiscal-years', [refetchKey]);
  if (error) return <ApiError message={error} />;

  const [showForm, setShowForm] = useState(false);
  const [label, setLabel] = useState('');
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');

  async function create() {
    setSaving(true);
    setSaveError('');
    try {
      await api.post('/fiscal-years', { label });
      setShowForm(false);
      setLabel('');
      setRefetchKey(k => k + 1);
    } catch (err) {
      setSaveError(String(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.toolbar}>
        <h2>Fiscal Year Management</h2>
        <button className={styles.btnNew} onClick={() => { setShowForm(true); setLabel(''); setSaveError(''); }}>
          + New Fiscal Year
        </button>
      </div>

      {showForm && (
        <div className={styles.form}>
          <input
            className={styles.input}
            placeholder="e.g. FY28"
            value={label}
            onChange={e => setLabel(e.target.value)}
          />
          {saveError && <div className={styles.error}>{saveError}</div>}
          <div className={styles.formActions}>
            <button className={styles.btnCancel} onClick={() => setShowForm(false)}>Cancel</button>
            <button className={styles.btnCreate} disabled={saving} onClick={create}>
              {saving ? 'Creating…' : 'Create'}
            </button>
          </div>
        </div>
      )}

      {loading && <p>Loading…</p>}

      <div className={styles.tableWrap}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Fiscal Year</th>
              <th>Start Date</th>
              <th>End Date</th>
            </tr>
          </thead>
          <tbody>
            {(fiscalYears ?? []).map(fy => (
              <tr key={fy.fiscalYear}>
                <td>{fy.fiscalYear}</td>
                <td>{fy.startDate}</td>
                <td>{fy.endDate}</td>
              </tr>
            ))}
            {(fiscalYears ?? []).length === 0 && !loading && (
              <tr><td colSpan={3} className={styles.empty}>No fiscal years found</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
