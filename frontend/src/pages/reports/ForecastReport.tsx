/**
 * Forecast Report — planned vs actuals with remaining budget per project.
 * Spec: 09-reporting.md — Forecast Report
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

export default function ForecastReport({ fiscalYear = 'FY26' }: Props) {
  const { asOfDate } = useTimeMachine();

  const path = `/reports/variance?fiscalYear=${fiscalYear}${asOfDate ? `&asOfDate=${asOfDate}` : ''}`;
  const { data: report, loading, error } = useApi<VRT>(path);
  if (error) return <ApiError message={error} />;

  const totalPlanned   = report?.rows.reduce((s, r) => s + r.totalPlanned, 0) ?? 0;
  const totalActual    = report?.rows.reduce((s, r) => s + r.totalActual, 0) ?? 0;
  const totalRemaining = report?.rows.reduce((s, r) => s + Math.max(0, r.totalPlanned - r.totalActual), 0) ?? 0;

  function handleExport() {
    if (!report) return;
    const headers = ['Contract', 'Project', 'Planned', 'Actuals YTD', 'Remaining', 'Status'];
    const rows = report.rows.map(r => [
      r.contractName, r.projectName,
      r.totalPlanned, r.totalActual,
      Math.max(0, r.totalPlanned - r.totalActual),
      r.totalStatus,
    ]);
    exportCsv(`forecast-${report.fiscalYear}.csv`, headers, rows);
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div className={styles.controls}>
          {report && <button className={styles.exportBtn} onClick={handleExport}>Export CSV</button>}
        </div>
      </div>

      {loading && <p>Loading…</p>}

      {report && (
        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Contract / Project</th>
                <th className={styles.num}>Planned</th>
                <th className={styles.num}>Actuals YTD</th>
                <th className={styles.num}>Remaining</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {report.rows.map(r => {
                const remaining = Math.max(0, r.totalPlanned - r.totalActual);
                return (
                  <tr key={r.projectId}>
                    <td>
                      <div className={styles.contractName}>{r.contractName}</div>
                      <div className={styles.projectName}>{r.projectId} {r.projectName}</div>
                    </td>
                    <td className={styles.num}>{formatCurrency(r.totalPlanned)}</td>
                    <td className={styles.num}>{formatCurrency(r.totalActual)}</td>
                    <td className={styles.num}>{formatCurrency(remaining)}</td>
                    <td><StatusBadge status={r.totalStatus} size="sm" /></td>
                  </tr>
                );
              })}
              {report.rows.length === 0 && (
                <tr><td colSpan={5} className={styles.empty}>No data for {fiscalYear}</td></tr>
              )}
              {report.rows.length > 0 && (
                <tr className={styles.grandTotalRow}>
                  <td><strong>TOTAL REMAINING</strong></td>
                  <td className={styles.num}>{formatCurrency(totalPlanned)}</td>
                  <td className={styles.num}>{formatCurrency(totalActual)}</td>
                  <td className={`${styles.num} ${styles.totalCell}`}>{formatCurrency(totalRemaining)}</td>
                  <td></td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
