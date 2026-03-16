/**
 * CashflowTable — milestone × fiscal-period grid.
 * Rows = milestones, columns = fiscal periods in the year.
 * Each milestone lives in exactly one period; other cells show —.
 * Spec: 21-ui-ux-spec.md §3 Cashflow Table
 */
import { useApi } from '../hooks/useApi';
import { formatCurrency } from '../utils/format';
import styles from './CashflowTable.module.css';

interface MilestoneRow {
  milestoneId: string;
  name: string;
  status?: string;
  currentVersion: {
    plannedAmount: number;
    fiscalPeriodKey: string;
  };
}

interface FiscalPeriod {
  periodId: string;
  periodKey: string;
}

interface ReconStatus {
  plannedAmount: number;
  reconciledAmount: number;
  remaining: number;
  status: string;
}

interface Props {
  projectId: string;
  fiscalYear?: string;
  /** Optional: already-fetched milestones to avoid redundant API call */
  milestones?: MilestoneRow[];
  /** Optional: already-fetched periods */
  periods?: FiscalPeriod[];
  onCellClick?: (milestoneId: string, periodKey: string, milestoneName: string) => void;
}

/** Shorten "FY26-04-JAN" → "Jan" for column headers */
function shortPeriod(key: string): string {
  const parts = key.split('-');
  return parts[parts.length - 1]
    ? parts[parts.length - 1].charAt(0) + parts[parts.length - 1].slice(1).toLowerCase()
    : key;
}

function healthDot(variance: number): string {
  if (variance < 0) return styles.dotRed;
  if (variance === 0) return styles.dotNeutral;
  return styles.dotGreen;
}

function MilestoneReconCell({
  milestoneId,
  milestoneName,
  periodKey,
  plannedAmount,
  onCellClick,
}: {
  milestoneId: string;
  milestoneName: string;
  periodKey: string;
  plannedAmount: number;
  onCellClick?: (milestoneId: string, periodKey: string, milestoneName: string) => void;
}) {
  const { data: recon } = useApi<ReconStatus>(`/reconciliation/status/${milestoneId}`);

  const actual = recon?.reconciledAmount ?? null;
  const variance = actual !== null ? plannedAmount - actual : null;

  const cellCls = [
    styles.dataCell,
    onCellClick ? styles.clickable : '',
    variance !== null && variance < 0 ? styles.over : '',
  ].filter(Boolean).join(' ');

  return (
    <td
      className={cellCls}
      onClick={() => onCellClick?.(milestoneId, periodKey, milestoneName)}
    >
      <div className={styles.planned}>{formatCurrency(plannedAmount)}</div>
      {actual !== null && (
        <div className={styles.actual}>{formatCurrency(actual)}</div>
      )}
      {variance !== null && (
        <div className={`${styles.variance} ${variance < 0 ? styles.varOver : variance === 0 ? styles.varZero : styles.varUnder}`}>
          {variance < 0 ? `(${formatCurrency(Math.abs(variance))})` : `${formatCurrency(variance)}`}
        </div>
      )}
    </td>
  );
}

export default function CashflowTable({ projectId, fiscalYear = 'FY26', milestones: propMilestones, periods: propPeriods, onCellClick }: Props) {
  const { data: fetchedMilestones, loading: mLoading } = useApi<MilestoneRow[]>(
    propMilestones ? null : `/projects/${projectId}/milestones`
  );
  const { data: fetchedPeriods } = useApi<FiscalPeriod[]>(
    propPeriods ? null : `/fiscal-years/${fiscalYear}/periods`
  );

  const milestones = propMilestones ?? fetchedMilestones ?? [];
  const periods = propPeriods ?? fetchedPeriods ?? [];

  if (mLoading) {
    return <div className={styles.loading}>Loading…</div>;
  }

  if (milestones.length === 0) {
    return <div className={styles.empty}>No milestones yet.</div>;
  }

  // Build period totals
  const periodTotals = new Map<string, number>();
  for (const m of milestones) {
    const pk = m.currentVersion.fiscalPeriodKey;
    periodTotals.set(pk, (periodTotals.get(pk) ?? 0) + m.currentVersion.plannedAmount);
  }

  const grandTotal = milestones.reduce((s, m) => s + m.currentVersion.plannedAmount, 0);

  return (
    <div className={styles.wrap}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th className={styles.nameHeader}>Milestone</th>
            {periods.map(p => (
              <th key={p.periodId} className={styles.periodHeader}>
                {shortPeriod(p.periodKey)}
              </th>
            ))}
            <th className={styles.totalHeader}>Total</th>
          </tr>
        </thead>
        <tbody>
          {milestones.map(m => {
            const milestonePeriodKey = m.currentVersion.fiscalPeriodKey;
            return (
              <tr key={m.milestoneId} className={styles.row}>
                <td className={styles.nameCell}>
                  <span className={`${styles.dot} ${healthDot(0)}`} />
                  {m.name}
                </td>
                {periods.map(p => {
                  if (p.periodKey !== milestonePeriodKey) {
                    return <td key={p.periodId} className={styles.emptyCell}>—</td>;
                  }
                  return (
                    <MilestoneReconCell
                      key={p.periodId}
                      milestoneId={m.milestoneId}
                      milestoneName={m.name}
                      periodKey={p.periodKey}
                      plannedAmount={m.currentVersion.plannedAmount}
                      onCellClick={onCellClick}
                    />
                  );
                })}
                <td className={styles.totalCell}>
                  {formatCurrency(m.currentVersion.plannedAmount)}
                </td>
              </tr>
            );
          })}
        </tbody>
        <tfoot>
          <tr className={styles.summaryRow}>
            <td className={styles.summaryLabel}>Total</td>
            {periods.map(p => {
              const t = periodTotals.get(p.periodKey);
              return (
                <td key={p.periodId} className={styles.summaryCell}>
                  {t ? formatCurrency(t) : '—'}
                </td>
              );
            })}
            <td className={styles.summaryTotal}>{formatCurrency(grandTotal)}</td>
          </tr>
        </tfoot>
      </table>
    </div>
  );
}
