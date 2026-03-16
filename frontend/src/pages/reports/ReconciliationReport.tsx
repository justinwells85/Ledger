/**
 * Reconciliation Status Report — milestone-level reconciliation summary.
 * Spec: 09-reporting.md — Reconciliation Status Report
 */
import { useState } from 'react';
import { useTimeMachine } from '../../context/TimeMachineContext';
import { useApi } from '../../hooks/useApi';
import { ReconciliationStatusReport } from '../../api/types';
import { formatCurrency } from '../../utils/format';
import ApiError from '../../components/ApiError';
import StatusBadge from '../../components/StatusBadge';
import styles from './Report.module.css';

const STATUSES = ['FULLY_RECONCILED', 'PARTIALLY_MATCHED', 'OVER_BUDGET', 'UNMATCHED'];

interface Props { fiscalYear?: string; }

export default function ReconciliationReport({ fiscalYear = 'FY26' }: Props) {
  const [statusFilter, setStatusFilter] = useState('');
  const { asOfDate } = useTimeMachine();

  const path = `/reports/reconciliation-status?fiscalYear=${fiscalYear}${statusFilter ? `&status=${statusFilter}` : ''}${asOfDate ? `&asOfDate=${asOfDate}` : ''}`;
  const { data: report, loading, error } = useApi<ReconciliationStatusReport>(path);
  if (error) return <ApiError message={error} />;

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div className={styles.controls}>
          <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)} className={styles.select}>
            <option value="">All Statuses</option>
            {STATUSES.map(s => <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>)}
          </select>
        </div>
      </div>

      {loading && <p>Loading…</p>}

      {report && (
        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Contract</th>
                <th>Project</th>
                <th>Milestone</th>
                <th>Period</th>
                <th className={styles.num}>Planned</th>
                <th className={styles.num}>Invoice</th>
                <th className={styles.num}>Accrual Net</th>
                <th className={styles.num}>Total Actual</th>
                <th className={styles.num}>Remaining</th>
                <th>Status</th>
                <th className={styles.num}>Open Accruals</th>
              </tr>
            </thead>
            <tbody>
              {report.rows.map((r, i) => (
                <tr key={i}>
                  <td>{r.contractName}</td>
                  <td>{r.projectName}</td>
                  <td>{r.milestoneName}</td>
                  <td>{r.fiscalPeriod}</td>
                  <td className={styles.num}>{formatCurrency(r.planned)}</td>
                  <td className={styles.num}>{formatCurrency(r.invoiceTotal)}</td>
                  <td className={styles.num}>{formatCurrency(r.accrualNet)}</td>
                  <td className={styles.num}>{formatCurrency(r.totalActual)}</td>
                  <td className={`${styles.num} ${r.remaining < 0 ? styles.over : ''}`}>
                    {formatCurrency(r.remaining)}
                  </td>
                  <td><StatusBadge status={r.status} size="sm" /></td>
                  <td className={styles.num}>{r.openAccrualCount}</td>
                </tr>
              ))}
              {report.rows.length === 0 && (
                <tr><td colSpan={11} className={styles.empty}>No data</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
