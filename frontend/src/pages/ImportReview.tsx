import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import { SapImport, ActualLine } from '../api/types';
import { formatCurrencyFull } from '../utils/format';
import ApiError from '../components/ApiError';
import styles from './ImportReview.module.css';

type LineFilter = 'ALL' | 'NEW' | 'DUPLICATES';

export default function ImportReview() {
  const { importId } = useParams<{ importId: string }>();
  const navigate = useNavigate();
  const { data: imp, error: impError } = useApi<SapImport>(`/imports/${importId}`);
  const { data: lines, loading, error: linesError } = useApi<ActualLine[]>(`/imports/${importId}/lines`);
  if (impError) return <ApiError message={impError} />;
  if (linesError) return <ApiError message={linesError} />;
  const [filter, setFilter] = useState<LineFilter>('ALL');
  const [acting, setActing] = useState(false);
  const [actionError, setActionError] = useState('');

  const filtered = (lines ?? []).filter(l => {
    if (filter === 'NEW') return !l.duplicate;
    if (filter === 'DUPLICATES') return l.duplicate;
    return true;
  });

  async function commit() {
    setActing(true);
    setActionError('');
    try {
      await api.post(`/imports/${importId}/commit`, {});
      navigate('/import');
    } catch (e) {
      setActionError(String(e));
      setActing(false);
    }
  }

  async function reject() {
    if (!confirm('Reject this import? This cannot be undone.')) return;
    setActing(true);
    setActionError('');
    try {
      await api.post(`/imports/${importId}/reject`, {});
      navigate('/import');
    } catch (e) {
      setActionError(String(e));
      setActing(false);
    }
  }

  const isActionable = imp?.status === 'STAGED';

  return (
    <div className={styles.page}>
      <div className={styles.breadcrumb}>
        <span className={styles.breadLink} onClick={() => navigate('/import')}>SAP Import</span>
        <span className={styles.sep}>/</span>
        <span>Review</span>
      </div>

      <h2>SAP Import — Review</h2>

      {imp && (
        <div className={styles.meta}>
          File: <strong>{imp.filename}</strong> &nbsp;·&nbsp;
          Uploaded: <strong>{new Date(imp.importedAt).toLocaleString()}</strong> &nbsp;·&nbsp;
          Status: <strong>{imp.status}</strong>
        </div>
      )}

      {/* Summary cards */}
      {imp && (
        <div className={styles.summaryRow}>
          <div className={styles.summaryCard}><div className={styles.sumLbl}>TOTAL LINES</div><div className={styles.sumVal}>{imp.totalLines}</div></div>
          <div className={styles.summaryCard}><div className={styles.sumLbl}>NEW</div><div className={`${styles.sumVal} ${styles.new}`}>{imp.newLines}</div></div>
          <div className={styles.summaryCard}><div className={styles.sumLbl}>DUPLICATES</div><div className={styles.sumVal}>{imp.duplicateLines}</div></div>
          <div className={styles.summaryCard}><div className={styles.sumLbl}>ERRORS</div><div className={`${styles.sumVal} ${imp.errorLines > 0 ? styles.error : ''}`}>{imp.errorLines}</div></div>
        </div>
      )}

      {/* Filter tabs */}
      <div className={styles.filterRow}>
        {(['ALL', 'NEW', 'DUPLICATES'] as LineFilter[]).map(f => (
          <button key={f} className={`${styles.filterBtn} ${filter === f ? styles.filterActive : ''}`} onClick={() => setFilter(f)}>
            {f}
          </button>
        ))}
      </div>

      {/* Lines table */}
      {loading ? <p>Loading lines…</p> : (
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Post Date</th>
              <th className={styles.num}>Amount</th>
              <th>Vendor</th>
              <th>WBSE</th>
              <th>Doc #</th>
              <th>Description</th>
              <th>Type</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(l => (
              <tr key={l.actualId} className={l.duplicate ? styles.dupRow : ''}>
                <td>{l.postingDate}</td>
                <td className={`${styles.num} ${l.amount < 0 ? styles.neg : ''}`}>{formatCurrencyFull(l.amount)}</td>
                <td>{l.vendorName}</td>
                <td>{l.wbse}</td>
                <td>{l.sapDocumentNumber}</td>
                <td className={styles.desc}>{l.description}</td>
                <td>{l.duplicate ? <span className={styles.dupBadge}>DUP</span> : <span className={styles.newBadge}>NEW</span>}</td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr><td colSpan={7} className={styles.empty}>No lines</td></tr>
            )}
          </tbody>
        </table>
      )}

      {/* Action bar */}
      {isActionable && (
        <div className={styles.actionBar}>
          {actionError && <span className={styles.actionError}>{actionError}</span>}
          <button className={styles.btnReject} disabled={acting} onClick={reject}>Reject Import</button>
          <button className={styles.btnCommit} disabled={acting} onClick={commit}>Commit Import</button>
        </div>
      )}
    </div>
  );
}
