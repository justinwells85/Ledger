/**
 * Open Accruals Report — aging accruals requiring attention.
 * Spec: 09-reporting.md — Open Accruals Report
 */
import { useApi } from '../../hooks/useApi';
import { OpenAccrualsReport as OAR } from '../../api/types';
import ApiError from '../../components/ApiError';
import StatusBadge from '../../components/StatusBadge';
import styles from './Report.module.css';

interface Props { fiscalYear?: string; }

export default function OpenAccrualsReport({ fiscalYear = 'FY26' }: Props) {
  const { data: report, loading, error } = useApi<OAR>(`/reports/open-accruals?fiscalYear=${fiscalYear}`);
  if (error) return <ApiError message={error} />;

  return (
    <div className={styles.page}>
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
                <th className={styles.num}>Open Accruals</th>
                <th className={styles.num}>Age (days)</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {report.rows.map((r, i) => (
                <tr key={i}>
                  <td>{r.contractName}</td>
                  <td>{r.projectName}</td>
                  <td>{r.milestoneName}</td>
                  <td>{r.fiscalPeriod}</td>
                  <td className={styles.num}>{r.openAccrualCount}</td>
                  <td className={`${styles.num} ${r.accrualStatus === 'CRITICAL' ? styles.over : r.accrualStatus === 'WARNING' ? styles.warn : ''}`}>
                    {r.ageDays}
                  </td>
                  <td><StatusBadge status={r.accrualStatus} size="sm" /></td>
                </tr>
              ))}
              {report.rows.length === 0 && !loading && (
                <tr><td colSpan={7} className={styles.empty}>No open accruals ✓</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
