import { useState } from 'react';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import FormField from '../components/FormField';
import { formatCurrencyFull } from '../utils/format';
import styles from './Detail.module.css';

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
  milestoneName: string;
  asOfDate: string | null;
  fiscalPeriods: FiscalPeriod[];
}

function statusClass(status: string) {
  if (status === 'FULLY_RECONCILED') return styles.statusGreen;
  if (status === 'OVER_BUDGET') return styles.statusRed;
  if (status === 'PARTIALLY_MATCHED') return styles.statusYellow;
  return styles.statusGray;
}

export default function MilestonePanel({ milestoneId, asOfDate, fiscalPeriods }: Props) {
  const [refetchKey, setRefetchKey] = useState(0);
  const { data: versions } = useApi<VersionResponse[]>(`/milestones/${milestoneId}/versions`, [refetchKey]);
  const { data: status } = useApi<ReconciliationStatus>(
    `/reconciliation/status/${milestoneId}${asOfDate ? `?asOfDate=${asOfDate}` : ''}`,
    [refetchKey]
  );

  const [newVersionOpen, setNewVersionOpen] = useState(false);
  const [nvAmount, setNvAmount] = useState('');
  const [nvPeriod, setNvPeriod] = useState('');
  const [nvEffective, setNvEffective] = useState('');
  const [nvReason, setNvReason] = useState('');
  const [nvError, setNvError] = useState('');
  const [saving, setSaving] = useState(false);

  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancelEffective, setCancelEffective] = useState('');
  const [cancelReason, setCancelReason] = useState('');
  const [cancelError, setCancelError] = useState('');
  const [cancelling, setCancelling] = useState(false);

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
      setNewVersionOpen(false);
      setRefetchKey(k => k + 1);
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
      setCancelOpen(false);
      setRefetchKey(k => k + 1);
    } catch (e) {
      setCancelError(String(e));
    } finally {
      setCancelling(false);
    }
  }

  // Determine if milestone is already cancelled: latest version has plannedAmount === 0
  const sortedVersions = [...(versions ?? [])].sort((a, b) => a.versionNumber - b.versionNumber);
  const latestVersion = sortedVersions[sortedVersions.length - 1];
  const isCancelled = latestVersion ? latestVersion.plannedAmount === 0 : false;

  return (
    <div className={styles.milestoneExpand}>
      <div className={styles.expandGrid}>
        {/* Reconciliation summary */}
        <div className={styles.expandSection}>
          <h4>RECONCILIATION SUMMARY</h4>
          {status ? (
            <table className={styles.table} style={{ fontSize: '0.8rem' }}>
              <tbody>
                <tr><td>Planned</td><td className={styles.num}>{formatCurrencyFull(status.plannedAmount)}</td></tr>
                <tr><td>Invoice Total</td><td className={styles.num}>{formatCurrencyFull(status.invoiceTotal)}</td></tr>
                <tr><td>Accrual Net</td><td className={styles.num}>{formatCurrencyFull(status.accrualNet)}</td></tr>
                <tr><td>Remaining</td><td className={`${styles.num} ${status.remaining < 0 ? styles.over : styles.under}`}>{formatCurrencyFull(status.remaining)}</td></tr>
                <tr>
                  <td>Status</td>
                  <td><span className={`${styles.statusBadge} ${statusClass(status.status)}`}>{status.status}</span></td>
                </tr>
              </tbody>
            </table>
          ) : (
            <p style={{ fontSize: '0.8rem', color: '#999' }}>No reconciliation data</p>
          )}

          {!isCancelled && (
            <div style={{ marginTop: '0.75rem' }}>
              <button className={styles.btnDanger} onClick={() => { setCancelEffective(new Date().toISOString().split('T')[0]); setCancelReason(''); setCancelError(''); setCancelOpen(v => !v); }}>
                {cancelOpen ? 'Close' : 'Cancel Milestone'}
              </button>
            </div>
          )}

          {cancelOpen && !isCancelled && (
            <div style={{ background: '#fff', border: '1px solid #ddd', borderRadius: 4, padding: '0.75rem', marginTop: '0.75rem' }}>
              <FormField label="Effective Date" type="date" required value={cancelEffective} onChange={e => setCancelEffective((e.target as HTMLInputElement).value)} />
              <FormField label="Reason" required value={cancelReason} onChange={e => setCancelReason((e.target as HTMLInputElement).value)} error={cancelError} />
              <button className={styles.btnDanger} disabled={cancelling} onClick={submitCancel}>
                {cancelling ? 'Cancelling…' : 'Confirm Cancel'}
              </button>
            </div>
          )}
        </div>

        {/* Version history */}
        <div className={styles.expandSection}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
            <h4 style={{ margin: 0 }}>VERSION HISTORY</h4>
            <button className={styles.btnSm} onClick={() => { setNvAmount(''); setNvPeriod(''); setNvEffective(''); setNvReason(''); setNvError(''); setNewVersionOpen(v => !v); }}>
              {newVersionOpen ? 'Cancel' : '+ New Version'}
            </button>
          </div>

          {newVersionOpen && (
            <div style={{ background: '#fff', border: '1px solid #ddd', borderRadius: 4, padding: '0.75rem', marginBottom: '0.75rem' }}>
              <FormField label="New Amount ($)" required type="number" value={nvAmount} onChange={e => setNvAmount((e.target as HTMLInputElement).value)} />
              <FormField label="Fiscal Period" as="select" value={nvPeriod} onChange={e => setNvPeriod((e.target as HTMLSelectElement).value)}>
                <option value="">— keep current —</option>
                {fiscalPeriods.map(p => <option key={p.periodId} value={p.periodId}>{p.periodKey}</option>)}
              </FormField>
              <FormField label="Effective Date" type="date" value={nvEffective} onChange={e => setNvEffective((e.target as HTMLInputElement).value)} />
              <FormField label="Reason *" required value={nvReason} onChange={e => setNvReason((e.target as HTMLInputElement).value)} error={nvError} />
              <button className={styles.btn} disabled={saving} onClick={submitNewVersion}>
                {saving ? 'Saving…' : 'Save Version'}
              </button>
            </div>
          )}

          {sortedVersions.map((v, idx) => {
            const prev = idx > 0 ? sortedVersions[idx - 1] : null;
            const delta = prev !== null ? v.plannedAmount - prev.plannedAmount : null;
            const cancelled = v.plannedAmount === 0;
            return (
              <div key={v.versionId} className={styles.versionRow}>
                <span style={{ minWidth: 28, fontWeight: 600 }}>v{v.versionNumber}</span>
                {cancelled ? (
                  <span className={`${styles.statusBadge} ${styles.statusRed}`}>CANCELLED</span>
                ) : (
                  <span style={{ flex: 1 }}>{formatCurrencyFull(v.plannedAmount)}</span>
                )}
                {delta === null ? (
                  <span style={{ fontSize: '0.75rem', color: '#999', minWidth: 70 }}>Initial</span>
                ) : (
                  <span style={{ fontSize: '0.75rem', minWidth: 70, color: delta >= 0 ? '#27ae60' : '#c0392b' }}>
                    {delta >= 0 ? '+' : ''}{formatCurrencyFull(delta)}
                  </span>
                )}
                <span style={{ fontSize: '0.75rem', color: '#888', minWidth: 80 }}>{v.createdBy}</span>
                <span style={{ color: '#888', minWidth: 80 }}>{v.effectiveDate}</span>
                <span style={{ color: '#666', flex: 2, fontSize: '0.75rem', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{v.reason}</span>
              </div>
            );
          })}
          {(versions ?? []).length === 0 && <p style={{ fontSize: '0.8rem', color: '#999' }}>No versions</p>}
        </div>
      </div>
    </div>
  );
}
