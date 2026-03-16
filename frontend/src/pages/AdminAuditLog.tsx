/**
 * Admin Audit Log Viewer page.
 * Spec: 18-admin-configuration.md Section 5
 */
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import ApiError from '../components/ApiError';
import styles from './AdminAuditLog.module.css';

interface AuditLogEntry {
  auditId: string;
  entityType: string;
  entityId: string;
  action: string;
  createdBy: string;
  reason: string | null;
  createdAt: string;
  changes: Record<string, { before: string; after: string }> | null;
}

export default function AdminAuditLog() {
  const [searchParams] = useSearchParams();
  const [entityType, setEntityType] = useState(() => searchParams.get('entityType') ?? '');
  const [entityId, setEntityId] = useState(() => searchParams.get('entityId') ?? '');
  const [createdBy, setCreatedBy] = useState('');
  const [action, setAction] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const params = new URLSearchParams();
  if (entityType) params.set('entityType', entityType);
  if (entityId) params.set('entityId', entityId);
  if (createdBy) params.set('createdBy', createdBy);
  if (action) params.set('action', action);
  if (from) params.set('from', from);
  if (to) params.set('to', to);
  const queryString = params.toString();

  const { data: entries, loading, error } = useApi<AuditLogEntry[]>(
    `/audit${queryString ? '?' + queryString : ''}`,
    [entityType, entityId, createdBy, action, from, to]
  );

  if (error) return <ApiError message={error} />;

  function buildCsvUrl() {
    const p = new URLSearchParams(params);
    return `/api/v1/audit/export.csv${p.toString() ? '?' + p.toString() : ''}`;
  }

  return (
    <div className={styles.page}>
      <div className={styles.toolbar}>
        <h2>Audit Log</h2>
        <a href={buildCsvUrl()} download="audit-log.csv" className={styles.btnExport}>
          Export CSV
        </a>
      </div>

      <div className={styles.filters}>
        <select className={styles.filterSelect} value={entityType} onChange={e => setEntityType(e.target.value)}>
          <option value="">All Types</option>
          <option value="CONTRACT">CONTRACT</option>
          <option value="PROJECT">PROJECT</option>
          <option value="CONFIGURATION">CONFIGURATION</option>
          <option value="MILESTONE">MILESTONE</option>
        </select>
        <input
          className={styles.filterInput}
          placeholder="Entity ID"
          value={entityId}
          onChange={e => setEntityId(e.target.value)}
        />
        <select className={styles.filterSelect} value={action} onChange={e => setAction(e.target.value)}>
          <option value="">All Actions</option>
          <option value="CREATE">CREATE</option>
          <option value="UPDATE">UPDATE</option>
          <option value="STATUS_CHANGE">STATUS_CHANGE</option>
        </select>
        <input
          className={styles.filterInput}
          placeholder="User"
          value={createdBy}
          onChange={e => setCreatedBy(e.target.value)}
        />
        <input type="date" className={styles.filterInput} value={from} onChange={e => setFrom(e.target.value)} />
        <input type="date" className={styles.filterInput} value={to} onChange={e => setTo(e.target.value)} />
      </div>

      {loading && <p>Loading…</p>}

      <div className={styles.tableWrap}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Timestamp</th>
              <th>Entity Type</th>
              <th>Entity ID</th>
              <th>Action</th>
              <th>User</th>
              <th>Reason</th>
              <th>Changes</th>
            </tr>
          </thead>
          <tbody>
            {(entries ?? []).map(entry => (
              <>
                <tr key={entry.auditId}>
                  <td className={styles.mono}>{new Date(entry.createdAt).toLocaleString()}</td>
                  <td>{entry.entityType}</td>
                  <td className={styles.mono}>{entry.entityId}</td>
                  <td>{entry.action}</td>
                  <td>{entry.createdBy}</td>
                  <td>{entry.reason ?? '—'}</td>
                  <td>
                    {entry.changes && Object.keys(entry.changes).length > 0 && (
                      <button
                        className={styles.btnExpand}
                        onClick={() => setExpandedId(expandedId === entry.auditId ? null : entry.auditId)}
                      >
                        {expandedId === entry.auditId ? 'Hide' : `${Object.keys(entry.changes).length} field(s)`}
                      </button>
                    )}
                  </td>
                </tr>
                {expandedId === entry.auditId && entry.changes && (
                  <tr key={`${entry.auditId}-detail`} className={styles.detailRow}>
                    <td colSpan={7}>
                      <table className={styles.changesTable}>
                        <thead>
                          <tr><th>Field</th><th>Before</th><th>After</th></tr>
                        </thead>
                        <tbody>
                          {Object.entries(entry.changes).map(([field, diff]) => (
                            <tr key={field}>
                              <td>{field}</td>
                              <td className={styles.before}>{diff.before}</td>
                              <td className={styles.after}>{diff.after}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </td>
                  </tr>
                )}
              </>
            ))}
            {(entries ?? []).length === 0 && !loading && (
              <tr><td colSpan={7} className={styles.empty}>No audit entries found</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
