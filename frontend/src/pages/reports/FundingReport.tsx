/**
 * Funding Source Summary — planned budget grouped by funding source.
 * Spec: 09-reporting.md — Funding Source Summary
 */
import { useApi } from '../../hooks/useApi';
import { useTimeMachine } from '../../context/TimeMachineContext';
import { BudgetReport } from '../../api/types';
import { formatCurrency } from '../../utils/format';
import { exportCsv } from '../../utils/exportCsv';
import ApiError from '../../components/ApiError';
import styles from './Report.module.css';

interface Props { fiscalYear?: string; }

export default function FundingReport({ fiscalYear = 'FY26' }: Props) {
  const { asOfDate } = useTimeMachine();

  const path = `/reports/budget?fiscalYear=${fiscalYear}${asOfDate ? `&asOfDate=${asOfDate}` : ''}`;
  const { data: report, loading, error } = useApi<BudgetReport>(path);
  if (error) return <ApiError message={error} />;

  const byFunding = report?.rows.reduce((acc, row) => {
    acc[row.fundingSource] = (acc[row.fundingSource] ?? 0) + row.total;
    return acc;
  }, {} as Record<string, number>) ?? {};

  const fundingEntries = Object.entries(byFunding).sort((a, b) => a[0].localeCompare(b[0]));
  const grandTotal     = fundingEntries.reduce((s, [, v]) => s + v, 0);

  function handleExport() {
    if (!report) return;
    const headers = ['Funding Source', 'Total Planned', '% of Total'];
    const rows = fundingEntries.map(([source, total]) => [
      source,
      total,
      grandTotal > 0 ? ((total / grandTotal) * 100).toFixed(1) + '%' : '0%',
    ]);
    exportCsv(`funding-${fiscalYear}.csv`, headers, rows);
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
                <th>Funding Source</th>
                <th className={styles.num}>Total Planned</th>
                <th className={styles.num}>% of Total</th>
              </tr>
            </thead>
            <tbody>
              {fundingEntries.map(([source, total]) => (
                <tr key={source}>
                  <td>{source}</td>
                  <td className={styles.num}>{formatCurrency(total)}</td>
                  <td className={styles.num}>
                    {grandTotal > 0 ? ((total / grandTotal) * 100).toFixed(1) + '%' : '0%'}
                  </td>
                </tr>
              ))}
              {fundingEntries.length === 0 && (
                <tr><td colSpan={3} className={styles.empty}>No data for {fiscalYear}</td></tr>
              )}
              {fundingEntries.length > 0 && (
                <tr className={styles.grandTotalRow}>
                  <td><strong>GRAND TOTAL</strong></td>
                  <td className={`${styles.num} ${styles.totalCell}`}>{formatCurrency(grandTotal)}</td>
                  <td className={styles.num}>100%</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
