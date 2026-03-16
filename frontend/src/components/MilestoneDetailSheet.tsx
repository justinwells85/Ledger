/**
 * MilestoneDetailSheet — content rendered inside BottomSheet when a cashflow
 * table cell is clicked. Shows recon summary, version history, and action forms.
 * Spec: 21-ui-ux-spec.md §6 Phase 3 Bottom Detail Card
 */
import { useState } from 'react';
import { useApi } from '../hooks/useApi';
import { useTimeMachine } from '../context/TimeMachineContext';
import { api } from '../api/client';
import FormField from './FormField';
import StatusBadge from './StatusBadge';
import { formatCurrency } from '../utils/format';
import styles from './MilestoneDetailSheet.module.css';

interface VersionResponse {
  versionId: string;
  versionNumber: number;
  plannedAmount: number;
  fiscalPeriodKey: string;
  effectiveDate: string;
  reason: string;
  createdBy: string;
}

interface ReconciliationStatus {
  plannedAmount: number;
  reconciledAmount: number;
  invoiceTotal: number;
  accrualNet: number;
  remaining: number;
  status: string;
}

interface FiscalPeriod {
  periodId: string;
  periodKey: string;
}

interface Props {
  milestoneId: string;
  fiscalPeriods: FiscalPeriod[];
  onUpdated: () => void;
}

export default function MilestoneDetailSheet({ milestoneId, fiscalPeriods, onUpdated }: Props) {
  const { asOfDate } = useTimeMachine();
  const [refetchKey, setRefetchKey] = useState(0);

  const { data: versions } = useApi<VersionResponse[]>(
    `/milestones/${milestoneId}/versions`, [refetchKey]
  );
  const { data: status } = useApi<ReconciliationStatus>(
    `/reconciliation/status/${milestoneId}${asOfDate ? `?asOfDate=${asOfDate}` : ''}`,
    [refetchKey]
  );

  // New version form
  const [showNewVersion, setShowNewVersion] = useState(false);
  const [nvAmount, setNvAmount] = useState('');
  const [nvPeriod, setNvPeriod] = useState('');
  const [nvEffective, setNvEffective] = useState('');
  const [nvReason, setNvReason] = useState('');
  const [nvError, setNvError] = useState('');
  const [saving, setSaving] = useState(false);

  // Cancel form
  const [showCancel, setShowCancel] = useState(false);
  const [cancelEffective, setCancelEffective] = useState('');
  const [cancelReason, setCancelReason] = useState('');
  const [cancelError, setCancelError] = useState('');
  const [cancelling, setCancelling] = useState(false);

  const sortedVersions = [...(versions ?? [])].sort((a, b) => a.versionNumber - b.versionNumber);
  const latestVersion = sortedVersions[sortedVersions.length - 1];
  const isCancelled = latestVersion?.plannedAmount === 0;

  async function submitNewVersion() {
    if (!nvAmount.trim() || !nvReason.trim()) {
      setNvError('Amount and reason are required');
      return;
    }
    setSaving(true);
    try {
      await api.post(`/milestones/${milestoneId}/versions`, {
        plannedAmount: parseFloat(nvAmount),
        fiscalPeriodId: nvPeriod || undefined,
        effectiveDate: nvEffective || new Date().toISOString().split('T')[0],
        reason: nvReason,
      });
      setShowNewVersion(false);
      setRefetchKey(k => k + 1);
      onUpdated();
    } catch (e) {
      setNvError(String(e));
    } finally {
      setSaving(false);
    }
  }

  async function submitCancel() {
    if (!cancelReason.trim()) { setCancelError('Reason is required'); return; }
    if (!cancelEffective.trim()) { setCancelError('Effective date is required'); return; }
    setCancelling(true);
    try {
      await api.post(`/milestones/${milestoneId}/cancel`, {
        effectiveDate: cancelEffective,
        reason: cancelReason,
      });
      setShowCancel(false);
      setRefetchKey(k => k + 1);
      onUpdated();
    } catch (e) {
      setCancelError(String(e));
    } finally {
      setCancelling(false);
    }
  }

  return (
    <div className={styles.root}>
      <div className={styles.grid}>

        {/* ── Left: Reconciliation summary ── */}
        <div className={styles.section}>
          <div className={styles.sectionTitle}>RECONCILIATION</div>

          {status ? (
            <>
              <div className={styles.reconGrid}>
                <div className={styles.reconRow}>
                  <span className={styles.reconLabel}>Planned</span>
                  <span className={styles.reconValue}>{formatCurrency(status.plannedAmount)}</span>
                </div>
                <div className={styles.reconRow}>
                  <span className={styles.reconLabel}>Invoiced</span>
                  <span className={styles.reconValue}>{formatCurrency(status.invoiceTotal)}</span>
                </div>
                <div className={styles.reconRow}>
                  <span className={styles.reconLabel}>Accrual net</span>
                  <span className={styles.reconValue}>{formatCurrency(status.accrualNet)}</span>
                </div>
                <div className={`${styles.reconRow} ${styles.reconRowTotal}`}>
                  <span className={styles.reconLabel}>Remaining</span>
                  <span className={`${styles.reconValue} ${status.remaining < 0 ? styles.over : styles.under}`}>
                    {status.remaining < 0
                      ? `(${formatCurrency(Math.abs(status.remaining))})`
                      : formatCurrency(status.remaining)}
                  </span>
                </div>
              </div>
              <div className={styles.reconStatus}>
                <StatusBadge status={status.status} />
              </div>
            </>
          ) : (
            <div className={styles.empty}>No reconciliation data</div>
          )}

          {/* Cancel milestone */}
          {!isCancelled && (
            <div className={styles.dangerZone}>
              <button
                className={styles.btnDanger}
                onClick={() => {
                  setCancelEffective(new Date().toISOString().split('T')[0]);
                  setCancelReason('');
                  setCancelError('');
                  setShowCancel(v => !v);
                }}
              >
                {showCancel ? 'Close' : 'Cancel Milestone'}
              </button>

              {showCancel && (
                <div className={styles.inlineForm}>
                  <FormField label="Effective Date" type="date" required
                    value={cancelEffective}
                    onChange={e => setCancelEffective((e.target as HTMLInputElement).value)}
                  />
                  <FormField label="Reason" required
                    value={cancelReason}
                    onChange={e => setCancelReason((e.target as HTMLInputElement).value)}
                    error={cancelError}
                  />
                  <button className={styles.btnDanger} disabled={cancelling} onClick={submitCancel}>
                    {cancelling ? 'Cancelling…' : 'Confirm Cancel'}
                  </button>
                </div>
              )}
            </div>
          )}
        </div>

        {/* ── Right: Version history ── */}
        <div className={styles.section}>
          <div className={styles.sectionHeaderRow}>
            <div className={styles.sectionTitle}>VERSION HISTORY</div>
            {!isCancelled && (
              <button
                className={styles.btnSm}
                onClick={() => {
                  setNvAmount('');
                  setNvPeriod('');
                  setNvEffective('');
                  setNvReason('');
                  setNvError('');
                  setShowNewVersion(v => !v);
                }}
              >
                {showNewVersion ? 'Cancel' : '+ New Version'}
              </button>
            )}
          </div>

          {showNewVersion && (
            <div className={styles.inlineForm}>
              <FormField label="New Amount ($)" required type="number"
                value={nvAmount}
                onChange={e => setNvAmount((e.target as HTMLInputElement).value)}
              />
              <FormField label="Fiscal Period" as="select"
                value={nvPeriod}
                onChange={e => setNvPeriod((e.target as HTMLSelectElement).value)}
              >
                <option value="">— keep current —</option>
                {fiscalPeriods.map(p => (
                  <option key={p.periodId} value={p.periodId}>{p.periodKey}</option>
                ))}
              </FormField>
              <FormField label="Effective Date" type="date"
                value={nvEffective}
                onChange={e => setNvEffective((e.target as HTMLInputElement).value)}
              />
              <FormField label="Reason" required
                value={nvReason}
                onChange={e => setNvReason((e.target as HTMLInputElement).value)}
                error={nvError}
              />
              <button className={styles.btnPrimary} disabled={saving} onClick={submitNewVersion}>
                {saving ? 'Saving…' : 'Save Version'}
              </button>
            </div>
          )}

          <div className={styles.versionList}>
            {sortedVersions.map((v, idx) => {
              const prev = idx > 0 ? sortedVersions[idx - 1] : null;
              const delta = prev !== null ? v.plannedAmount - prev.plannedAmount : null;
              const cancelled = v.plannedAmount === 0;
              return (
                <div key={v.versionId} className={styles.versionRow}>
                  <span className={styles.versionNum}>v{v.versionNumber}</span>
                  <span className={styles.versionAmount}>
                    {cancelled ? (
                      <StatusBadge status="CANCELLED" size="sm" />
                    ) : (
                      formatCurrency(v.plannedAmount)
                    )}
                  </span>
                  {delta !== null && (
                    <span className={`${styles.versionDelta} ${delta >= 0 ? styles.deltaPos : styles.deltaNeg}`}>
                      {delta >= 0 ? '+' : ''}{formatCurrency(delta)}
                    </span>
                  )}
                  <span className={styles.versionMeta}>{v.effectiveDate}</span>
                  <span className={styles.versionReason}>{v.reason}</span>
                </div>
              );
            })}
            {sortedVersions.length === 0 && (
              <div className={styles.empty}>No versions</div>
            )}
          </div>
        </div>

      </div>
    </div>
  );
}
