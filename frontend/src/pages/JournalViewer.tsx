import { useState } from 'react';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import { JournalEntry } from '../api/types';
import ApiError from '../components/ApiError';
import styles from './JournalViewer.module.css';

interface JournalLine {
  lineId: string;
  account: string;
  projectId: string | null;
  milestoneId: string | null;
  fiscalPeriodId: string;
  debit: number;
  credit: number;
}

const ENTRY_TYPES = ['', 'PLAN_CREATE', 'PLAN_ADJUST', 'PLAN_CANCEL', 'ACTUAL_IMPORT', 'RECONCILE', 'RECONCILE_UNDO'];

export default function JournalViewer() {
  const { data: entries, loading, error } = useApi<JournalEntry[]>('/journal');
  if (error) return <ApiError message={error} />;
  const [typeFilter, setTypeFilter] = useState('');
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [lines, setLines] = useState<Record<string, JournalLine[]>>({});
  const [loadingLines, setLoadingLines] = useState<string | null>(null);
  const [page, setPage] = useState(0);

  const PAGE_SIZE = 25;

  const filtered = (entries ?? []).filter(e => !typeFilter || e.entryType === typeFilter);
  const paginated = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);
  const totalPages = Math.ceil(filtered.length / PAGE_SIZE);

  async function toggleExpand(entryId: string) {
    if (expandedId === entryId) {
      setExpandedId(null);
      return;
    }
    setExpandedId(entryId);
    if (!lines[entryId]) {
      setLoadingLines(entryId);
      try {
        const l = await api.get<JournalLine[]>(`/journal/${entryId}/lines`);
        setLines(prev => ({ ...prev, [entryId]: l }));
      } finally {
        setLoadingLines(null);
      }
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <h2>Journal Ledger</h2>
        <div className={styles.controls}>
          <select value={typeFilter} onChange={e => { setTypeFilter(e.target.value); setPage(0); }} className={styles.select}>
            <option value="">All Types</option>
            {ENTRY_TYPES.filter(Boolean).map(t => <option key={t} value={t}>{t}</option>)}
          </select>
        </div>
      </div>

      {loading && <p>Loading…</p>}

      <div className={styles.tableWrap}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>Date</th>
            <th>Type</th>
            <th>Description</th>
            <th>By</th>
          </tr>
        </thead>
        <tbody>
          {paginated.map(e => (
            <>
              <tr key={e.entryId} className={styles.entryRow} onClick={() => toggleExpand(e.entryId)}>
                <td>{e.effectiveDate}</td>
                <td><span className={`${styles.typeBadge} ${styles[e.entryType] ?? ''}`}>{e.entryType}</span></td>
                <td>{e.description} {expandedId === e.entryId ? '▲' : '▼'}</td>
                <td>{e.createdBy}</td>
              </tr>
              {expandedId === e.entryId && (
                <tr key={`${e.entryId}-lines`}>
                  <td colSpan={4} style={{ padding: 0 }}>
                    <div className={styles.linesPanel}>
                      {loadingLines === e.entryId && <p className={styles.hint}>Loading lines…</p>}
                      {lines[e.entryId] && (
                        <table className={styles.linesTable}>
                          <thead>
                            <tr>
                              <th>Account</th>
                              <th>Project</th>
                              <th>Milestone</th>
                              <th className={styles.num}>Debit</th>
                              <th className={styles.num}>Credit</th>
                            </tr>
                          </thead>
                          <tbody>
                            {lines[e.entryId].map(l => (
                              <tr key={l.lineId}>
                                <td>{l.account}</td>
                                <td>{l.projectId ?? '—'}</td>
                                <td>{l.milestoneId ? l.milestoneId.slice(0, 8) + '…' : '—'}</td>
                                <td className={styles.num}>{l.debit > 0 ? `$${l.debit.toLocaleString()}` : ''}</td>
                                <td className={styles.num}>{l.credit > 0 ? `$${l.credit.toLocaleString()}` : ''}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      )}
                    </div>
                  </td>
                </tr>
              )}
            </>
          ))}
          {filtered.length === 0 && !loading && (
            <tr><td colSpan={4} className={styles.empty}>No journal entries</td></tr>
          )}
        </tbody>
      </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className={styles.pagination}>
          <button disabled={page === 0} onClick={() => setPage(p => p - 1)} className={styles.pageBtn}>← Prev</button>
          <span className={styles.pageInfo}>Page {page + 1} of {totalPages}</span>
          <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)} className={styles.pageBtn}>Next →</button>
        </div>
      )}
    </div>
  );
}
