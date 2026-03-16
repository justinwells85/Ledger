/**
 * ReconcileWorkspace — three-zone layout for matching actuals to milestones.
 * Zone 1 (top): recently matched pairs with undo.
 * Zone 2 (bottom-left): unmatched actuals with filter.
 * Zone 3 (bottom-right): milestone candidates + assignment form.
 * Spec: 21-ui-ux-spec.md §6.4 Reconciliation Workspace
 */
import { useState } from 'react';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import { ActualLine, MilestoneCandidate, ReconciliationResponse } from '../api/types';
import { formatCurrency } from '../utils/format';
import ApiError from '../components/ApiError';
import styles from './ReconcileWorkspace.module.css';

type Category = 'INVOICE' | 'ACCRUAL' | 'ACCRUAL_REVERSAL' | 'ALLOCATION';

const CATEGORY_LABELS: Record<Category, string> = {
  INVOICE:          'Invoice',
  ACCRUAL:          'Accrual',
  ACCRUAL_REVERSAL: 'Reversal',
  ALLOCATION:       'Allocation',
};

interface RecentItem {
  reconciliationId: string;
  description: string;
  milestoneName: string;
  amount: number;
}

export default function ReconcileWorkspace() {
  const [refetchKey, setRefetchKey] = useState(0);
  const { data: actuals, loading, error: actualsError } = useApi<ActualLine[]>('/reconciliation/unreconciled', [refetchKey]);
  const [selectedActual, setSelectedActual] = useState<ActualLine | null>(null);
  const [selectedCandidate, setSelectedCandidate] = useState<MilestoneCandidate | null>(null);

  const { data: candidates, loading: candLoading, error: candError } = useApi<MilestoneCandidate[]>(
    selectedActual ? `/reconciliation/candidates/${selectedActual.actualId}` : null
  );

  if (actualsError) return <ApiError message={actualsError} />;
  if (candError)    return <ApiError message={candError} />;

  // Filter state
  const [vendorFilter, setVendorFilter]     = useState('');
  const [amountFilter, setAmountFilter]     = useState<'all' | 'positive' | 'negative'>('all');

  // Assignment form
  const [category, setCategory] = useState<Category>('INVOICE');
  const [notes, setNotes]       = useState('');
  const [saving, setSaving]     = useState(false);
  const [saveError, setSaveError] = useState('');

  // Recently matched + undo
  const [recentItems, setRecentItems]     = useState<RecentItem[]>([]);
  const [undoId, setUndoId]               = useState<string | null>(null);
  const [undoReason, setUndoReason]       = useState('');
  const [undoing, setUndoing]             = useState(false);
  const [undoError, setUndoError]         = useState('');

  const filteredActuals = (actuals ?? []).filter(a => {
    const text = vendorFilter.toLowerCase();
    const matchText =
      text === '' ||
      a.vendorName?.toLowerCase().includes(text) ||
      a.description?.toLowerCase().includes(text);
    const matchAmt =
      amountFilter === 'all' ||
      (amountFilter === 'positive' && a.amount >= 0) ||
      (amountFilter === 'negative' && a.amount < 0);
    return matchText && matchAmt;
  });

  const totalUnmatched = (actuals ?? []).reduce((s, a) => s + a.amount, 0);

  function selectActual(a: ActualLine) {
    setSelectedActual(a);
    setSelectedCandidate(null);
    setSaveError('');
  }

  async function submitReconcile() {
    if (!selectedActual || !selectedCandidate) return;
    setSaving(true);
    setSaveError('');
    try {
      const result = await api.post<ReconciliationResponse>('/reconciliation', {
        actualId:    selectedActual.actualId,
        milestoneId: selectedCandidate.milestoneId,
        category,
        matchNotes:  notes || null,
      });
      setRecentItems(prev => [{
        reconciliationId: result.reconciliationId,
        description:  selectedActual.description || selectedActual.vendorName,
        milestoneName: selectedCandidate.milestoneName,
        amount:        selectedActual.amount,
      }, ...prev]);
      setSelectedActual(null);
      setSelectedCandidate(null);
      setNotes('');
      setRefetchKey(k => k + 1);
    } catch (e) {
      setSaveError(String(e));
    } finally {
      setSaving(false);
    }
  }

  async function confirmUndo() {
    if (!undoId || !undoReason.trim()) return;
    setUndoing(true);
    setUndoError('');
    try {
      await api.delete(`/reconciliation/${undoId}?reason=${encodeURIComponent(undoReason)}`);
      setRecentItems(prev => prev.filter(r => r.reconciliationId !== undoId));
      setUndoId(null);
      setUndoReason('');
      setRefetchKey(k => k + 1);
    } catch (e) {
      setUndoError(String(e));
    } finally {
      setUndoing(false);
    }
  }

  return (
    <div className={styles.page}>

      {/* ── Page header ── */}
      <div className={styles.pageHeader}>
        <h2 className={styles.pageTitle}>Reconciliation</h2>
        {actuals && (
          <div className={styles.headerStats}>
            <span className={styles.statChip}>
              <span className={styles.statNum}>{actuals.length}</span> unmatched
            </span>
            <span className={`${styles.statChip} ${totalUnmatched < 0 ? styles.statNeg : ''}`}>
              {totalUnmatched < 0
                ? `(${formatCurrency(Math.abs(totalUnmatched))})`
                : formatCurrency(totalUnmatched)}
            </span>
          </div>
        )}
      </div>

      {/* ── Zone 1: Recently matched ── */}
      {recentItems.length > 0 && (
        <div className={styles.matchedZone}>
          <div className={styles.zoneLabel}>MATCHED THIS SESSION</div>
          <div className={styles.matchedList}>
            {recentItems.map(item => (
              <div key={item.reconciliationId} className={styles.matchedRow}>
                {undoId === item.reconciliationId ? (
                  <div className={styles.undoForm}>
                    <input
                      className={styles.undoInput}
                      placeholder="Reason for undo (required)"
                      value={undoReason}
                      onChange={e => setUndoReason(e.target.value)}
                      autoFocus
                    />
                    {undoError && <div className={styles.errorText}>{undoError}</div>}
                    <div className={styles.undoActions}>
                      <button
                        className={styles.btnGhost}
                        onClick={() => { setUndoId(null); setUndoReason(''); setUndoError(''); }}
                      >
                        Cancel
                      </button>
                      <button
                        className={styles.btnDanger}
                        disabled={undoing || !undoReason.trim()}
                        onClick={confirmUndo}
                      >
                        {undoing ? 'Undoing…' : 'Confirm Undo'}
                      </button>
                    </div>
                  </div>
                ) : (
                  <>
                    <span className={styles.matchedPair}>
                      <span className={styles.matchedDesc}>{item.description}</span>
                      <span className={styles.matchedArrow}>→</span>
                      <span className={styles.matchedMilestone}>{item.milestoneName}</span>
                    </span>
                    <span className={`${styles.matchedAmount} ${item.amount < 0 ? styles.neg : ''}`}>
                      {item.amount < 0 ? `(${formatCurrency(Math.abs(item.amount))})` : formatCurrency(item.amount)}
                    </span>
                    <button
                      className={styles.btnUndo}
                      onClick={() => { setUndoId(item.reconciliationId); setUndoReason(''); }}
                    >
                      Undo
                    </button>
                  </>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ── Zone 2 + 3: Bottom two columns ── */}
      <div className={styles.workspace}>

        {/* ── Left: Unmatched actuals ── */}
        <div className={styles.panel}>
          <div className={styles.panelHeader}>
            <span className={styles.panelTitle}>UNMATCHED ACTUALS</span>
            {actuals && (
              <span className={styles.panelCount}>{filteredActuals.length}</span>
            )}
          </div>

          <div className={styles.filterBar}>
            <input
              className={styles.filterInput}
              placeholder="Vendor or description…"
              value={vendorFilter}
              onChange={e => setVendorFilter(e.target.value)}
            />
            <select
              className={styles.filterSelect}
              value={amountFilter}
              onChange={e => setAmountFilter(e.target.value as typeof amountFilter)}
              aria-label="Amount filter"
            >
              <option value="all">All</option>
              <option value="positive">Positive</option>
              <option value="negative">Negative</option>
            </select>
          </div>

          {loading && <div className={styles.hint}>Loading…</div>}

          <div className={styles.actualsList}>
            {filteredActuals.map(a => (
              <div
                key={a.actualId}
                className={`${styles.actualRow} ${selectedActual?.actualId === a.actualId ? styles.actualRowSelected : ''}`}
                onClick={() => selectActual(a)}
                role="button"
                tabIndex={0}
                onKeyDown={e => e.key === 'Enter' && selectActual(a)}
              >
                <div className={styles.actualTop}>
                  <span className={styles.actualVendor}>{a.vendorName}</span>
                  <span className={`${styles.actualAmount} ${a.amount < 0 ? styles.neg : ''}`}>
                    {a.amount < 0 ? `(${formatCurrency(Math.abs(a.amount))})` : formatCurrency(a.amount)}
                  </span>
                </div>
                <div className={styles.actualBottom}>
                  <span className={styles.actualDate}>{a.postingDate}</span>
                  {a.description && (
                    <span className={styles.actualDesc}>{a.description}</span>
                  )}
                </div>
              </div>
            ))}
            {filteredActuals.length === 0 && !loading && (
              <div className={styles.empty}>
                {vendorFilter || amountFilter !== 'all'
                  ? 'No actuals match filter.'
                  : 'All actuals reconciled ✓'}
              </div>
            )}
          </div>
        </div>

        {/* ── Right: Candidates + assignment ── */}
        <div className={styles.panel}>
          <div className={styles.panelHeader}>
            <span className={styles.panelTitle}>MATCH TO MILESTONE</span>
          </div>

          {!selectedActual && (
            <div className={styles.emptyRight}>
              <div className={styles.emptyRightIcon}>←</div>
              <div className={styles.emptyRightText}>Select an actual to see milestone candidates</div>
            </div>
          )}

          {selectedActual && (
            <div className={styles.rightContent}>
              {/* Selected actual context strip */}
              <div className={styles.contextStrip}>
                <div className={styles.contextRow}>
                  <span className={styles.contextVendor}>{selectedActual.vendorName}</span>
                  <span className={`${styles.contextAmount} ${selectedActual.amount < 0 ? styles.neg : ''}`}>
                    {selectedActual.amount < 0
                      ? `(${formatCurrency(Math.abs(selectedActual.amount))})`
                      : formatCurrency(selectedActual.amount)}
                  </span>
                </div>
                <div className={styles.contextMeta}>
                  {selectedActual.postingDate}
                  {selectedActual.description && ` · ${selectedActual.description}`}
                </div>
              </div>

              {/* Candidate list */}
              {candLoading && <div className={styles.hint}>Loading candidates…</div>}

              <div className={styles.candidateList}>
                {(candidates ?? []).map(c => (
                  <div
                    key={c.milestoneId}
                    className={`${styles.candidateCard} ${selectedCandidate?.milestoneId === c.milestoneId ? styles.candidateSelected : ''}`}
                    onClick={() => setSelectedCandidate(c)}
                    role="button"
                    tabIndex={0}
                    onKeyDown={e => e.key === 'Enter' && setSelectedCandidate(c)}
                  >
                    <div className={styles.candidateScore}>
                      {c.relevanceScore}
                    </div>
                    <div className={styles.candidateBody}>
                      <div className={styles.candidateName}>{c.milestoneName}</div>
                      <div className={styles.candidateMeta}>
                        {c.projectId} · Plan: {formatCurrency(c.plannedAmount)}
                      </div>
                    </div>
                  </div>
                ))}
                {(candidates ?? []).length === 0 && !candLoading && (
                  <div className={styles.hint}>No candidates found.</div>
                )}
              </div>

              {/* Assignment form */}
              {selectedCandidate && (
                <div className={styles.assignForm}>
                  <div className={styles.assignTarget}>
                    Reconciling to <strong>{selectedCandidate.milestoneName}</strong>
                  </div>

                  <div className={styles.categoryChips}>
                    {(Object.keys(CATEGORY_LABELS) as Category[]).map(cat => (
                      <button
                        key={cat}
                        className={`${styles.categoryChip} ${category === cat ? styles.categoryChipActive : ''}`}
                        onClick={() => setCategory(cat)}
                      >
                        {CATEGORY_LABELS[cat]}
                      </button>
                    ))}
                  </div>

                  <textarea
                    className={styles.notes}
                    placeholder="Notes (optional)"
                    value={notes}
                    onChange={e => setNotes(e.target.value)}
                    rows={2}
                  />

                  {saveError && <div className={styles.errorText}>{saveError}</div>}

                  <div className={styles.assignActions}>
                    <button className={styles.btnGhost} onClick={() => setSelectedCandidate(null)}>
                      Cancel
                    </button>
                    <button className={styles.btnPrimary} disabled={saving} onClick={submitReconcile}>
                      {saving ? 'Saving…' : 'Reconcile'}
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
