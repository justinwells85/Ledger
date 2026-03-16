/**
 * Budget Plan Report — milestone planned amounts by fiscal period (matrix view).
 * Spec: 09-reporting.md — Budget Plan Report
 */
import { useState } from 'react';
import { useApi } from '../../hooks/useApi';
import { useTimeMachine } from '../../context/TimeMachineContext';
import { BudgetReport as BudgetReportType } from '../../api/types';
import { formatCurrency } from '../../utils/format';
import { exportCsv } from '../../utils/exportCsv';
import ApiError from '../../components/ApiError';
import styles from './Report.module.css';

interface Props { fiscalYear?: string; }

function exportBudgetCsv(report: BudgetReportType) {
  const periods = [...new Set(report.rows.flatMap(r => Object.keys(r.periods)))].sort();
  const header  = ['Contract', 'Project', 'Funding', ...periods, 'Total'];
  const rows    = report.rows.map(r => [
    r.contractName, r.projectName, r.fundingSource,
    ...periods.map(p => r.periods[p] ?? 0),
    r.total,
  ]);
  exportCsv(`budget-${report.fiscalYear}.csv`, header, rows);
}

export default function BudgetReport({ fiscalYear = 'FY26' }: Props) {
  const [groupBy, setGroupBy]           = useState('month');
  const [projectFilter, setProjectFilter] = useState('');
  const { asOfDate }                    = useTimeMachine();

  const path = `/reports/budget?fiscalYear=${fiscalYear}&groupBy=${groupBy}${asOfDate ? `&asOfDate=${asOfDate}` : ''}`;
  const { data: report, loading, error } = useApi<BudgetReportType>(path);
  if (error) return <ApiError message={error} />;

  const periods = report
    ? [...new Set(report.rows.flatMap(r => Object.keys(r.periods)))].sort()
    : [];

  const filteredRows = report
    ? report.rows.filter(r =>
        projectFilter === '' ||
        r.contractName.toLowerCase().includes(projectFilter.toLowerCase()) ||
        r.projectName.toLowerCase().includes(projectFilter.toLowerCase())
      )
    : [];

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div className={styles.controls}>
          <select value={groupBy} onChange={e => setGroupBy(e.target.value)} className={styles.select}>
            <option value="month">By Month</option>
            <option value="quarter">By Quarter</option>
          </select>
          <input
            className={styles.filterInput}
            placeholder="Filter by contract or project…"
            value={projectFilter}
            onChange={e => setProjectFilter(e.target.value)}
          />
          {report && <button className={styles.exportBtn} onClick={() => exportBudgetCsv(report)}>Export CSV</button>}
        </div>
      </div>

      {loading && <p>Loading…</p>}

      {report && (
        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Contract / Project</th>
                <th>Funding</th>
                {periods.map(p => <th key={p} className={styles.num}>{p}</th>)}
                <th className={styles.num}>Total</th>
              </tr>
            </thead>
            <tbody>
              {filteredRows.map(row => (
                <tr key={row.projectId}>
                  <td>
                    <div className={styles.contractName}>{row.contractName}</div>
                    <div className={styles.projectName}>{row.projectId} {row.projectName}</div>
                  </td>
                  <td>{row.fundingSource}</td>
                  {periods.map(p => (
                    <td key={p} className={styles.num}>
                      {row.periods[p] != null ? formatCurrency(row.periods[p]) : '—'}
                    </td>
                  ))}
                  <td className={`${styles.num} ${styles.totalCell}`}>{formatCurrency(row.total)}</td>
                </tr>
              ))}
              {filteredRows.length === 0 && (
                <tr>
                  <td colSpan={3 + periods.length + 1} className={styles.empty}>
                    No data for {fiscalYear}
                  </td>
                </tr>
              )}
              {filteredRows.length > 0 && (
                <tr className={styles.grandTotalRow}>
                  <td colSpan={2}><strong>GRAND TOTAL</strong></td>
                  {periods.map(p => (
                    <td key={p} className={styles.num}>
                      {formatCurrency(filteredRows.reduce((s, r) => s + (r.periods[p] ?? 0), 0))}
                    </td>
                  ))}
                  <td className={`${styles.num} ${styles.totalCell}`}>
                    {formatCurrency(filteredRows.reduce((s, r) => s + r.total, 0))}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
