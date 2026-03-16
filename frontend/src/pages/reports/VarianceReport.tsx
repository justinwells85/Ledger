/**
 * Variance Report — planned vs actual variance by contract and project.
 * Spec: 09-reporting.md — Variance Report
 */
import { useApi } from '../../hooks/useApi';
import { useTimeMachine } from '../../context/TimeMachineContext';
import { VarianceReport as VRT } from '../../api/types';
import { formatCurrency } from '../../utils/format';
import { exportCsv } from '../../utils/exportCsv';
import ApiError from '../../components/ApiError';
import StatusBadge from '../../components/StatusBadge';
import styles from './Report.module.css';

interface Props { fiscalYear?: string; }

function exportVarianceCsv(report: VRT) {
  const header = ['Contract', 'Project', 'Planned', 'Actual', 'Variance', 'Variance%', 'Status'];
  const rows = report.rows.map(r => [
    r.contractName, r.projectName, r.totalPlanned, r.totalActual,
    r.totalVariance, r.totalVariancePercent.toFixed(2), r.totalStatus,
  ]);
  exportCsv(`variance-${report.fiscalYear}.csv`, header, rows);
}

export default function VarianceReport({ fiscalYear = 'FY26' }: Props) {
  const { asOfDate } = useTimeMachine();

  const path = `/reports/variance?fiscalYear=${fiscalYear}${asOfDate ? `&asOfDate=${asOfDate}` : ''}`;
  const { data: report, loading, error } = useApi<VRT>(path);
  if (error) return <ApiError message={error} />;

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div className={styles.controls}>
          {report && <button className={styles.exportBtn} onClick={() => exportVarianceCsv(report)}>Export CSV</button>}
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
                <th className={styles.num}>Planned</th>
                <th className={styles.num}>Actual</th>
                <th className={styles.num}>Variance</th>
                <th className={styles.num}>Var %</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {report.rows.map(r => (
                <tr key={r.projectId}>
                  <td>{r.contractName}</td>
                  <td>{r.projectName}</td>
                  <td className={styles.num}>{formatCurrency(r.totalPlanned)}</td>
                  <td className={styles.num}>{formatCurrency(r.totalActual)}</td>
                  <td className={`${styles.num} ${r.totalStatus === 'OVER_BUDGET' ? styles.over : styles.under}`}>
                    {formatCurrency(r.totalVariance)}
                  </td>
                  <td className={`${styles.num} ${r.totalStatus === 'OVER_BUDGET' ? styles.over : styles.under}`}>
                    {r.totalVariancePercent.toFixed(1)}%
                  </td>
                  <td><StatusBadge status={r.totalStatus} size="sm" /></td>
                </tr>
              ))}
              {report.rows.length === 0 && (
                <tr><td colSpan={7} className={styles.empty}>No data for {fiscalYear}</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
