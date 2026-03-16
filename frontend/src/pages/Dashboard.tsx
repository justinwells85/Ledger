/**
 * Portfolio Dashboard — entry point for P1 (Budget Owner) and P4 (Program Executive).
 * Shows KPI cards, alerts, and contract cards with project summaries.
 * Spec: 21-ui-ux-spec.md §6.1 Portfolio Dashboard
 */
import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { useTimeMachine } from '../context/TimeMachineContext';
import { VarianceReport, OpenAccrualsReport } from '../api/types';
import { formatMillions, formatCurrency } from '../utils/format';
import { api } from '../api/client';
import ApiError from '../components/ApiError';
import Drawer from '../components/Drawer';
import FormField from '../components/FormField';
import StatusBadge from '../components/StatusBadge';
import styles from './Dashboard.module.css';

interface ContractListItem {
  contractId: string;
  name: string;
  vendor: string;
  status: string;
}

interface VarianceRow {
  contractId: string;
  contractName: string;
  projectId: string;
  projectName: string;
  totalPlanned: number;
  totalActual: number;
  totalVariance: number;
  totalStatus: string;
}

const FISCAL_YEARS = ['FY26', 'FY27', 'FY25'];

export default function Dashboard() {
  const [fiscalYear, setFiscalYear] = useState('FY26');
  const { asOfDate } = useTimeMachine();
  const navigate = useNavigate();

  const [refetchKey, setRefetchKey] = useState(0);

  // New Contract panel state
  const [newContractOpen, setNewContractOpen] = useState(false);
  const [newContractName, setNewContractName] = useState('');
  const [newContractVendor, setNewContractVendor] = useState('');
  const [newContractOwner, setNewContractOwner] = useState('');
  const [newContractStartDate, setNewContractStartDate] = useState('');
  const [newContractError, setNewContractError] = useState('');

  const variancePath = `/reports/variance?fiscalYear=${fiscalYear}${asOfDate ? `&asOfDate=${asOfDate}` : ''}`;
  const accrualPath  = `/reports/open-accruals?fiscalYear=${fiscalYear}`;

  const { data: variance, loading: vLoading, error: vError } = useApi<VarianceReport>(variancePath, [refetchKey]);
  const { data: accruals,  error: aError  } = useApi<OpenAccrualsReport>(accrualPath, [refetchKey]);
  const { data: allContracts }               = useApi<ContractListItem[]>('/contracts', [refetchKey]);

  if (vError) return <ApiError message={vError} />;
  if (aError) return <ApiError message={aError} />;

  // Portfolio-level KPIs
  const totals = useMemo(() => {
    if (!variance?.rows) return { planned: 0, actual: 0, variance: 0 };
    return variance.rows.reduce(
      (acc, r) => ({ planned: acc.planned + r.totalPlanned, actual: acc.actual + r.totalActual, variance: acc.variance + r.totalVariance }),
      { planned: 0, actual: 0, variance: 0 }
    );
  }, [variance]);

  const spentPct = totals.planned > 0 ? Math.round((totals.actual / totals.planned) * 100) : 0;

  // Contract summary: all contracts merged with variance data
  const contractSummary = useMemo(() => {
    const contracts = allContracts ?? [];
    const varianceByContract = new Map<string, { planned: number; actual: number; variance: number; projects: VarianceRow[] }>();

    for (const row of (variance?.rows ?? []) as VarianceRow[]) {
      const entry = varianceByContract.get(row.contractId) ?? { planned: 0, actual: 0, variance: 0, projects: [] };
      entry.planned  += row.totalPlanned;
      entry.actual   += row.totalActual;
      entry.variance += row.totalVariance;
      entry.projects.push(row);
      varianceByContract.set(row.contractId, entry);
    }

    return contracts
      .map(c => ({
        contractId:   c.contractId,
        contractName: c.name,
        vendor:       c.vendor,
        status:       c.status,
        ...(varianceByContract.get(c.contractId) ?? { planned: 0, actual: 0, variance: 0, projects: [] }),
      }))
      .sort((a, b) => b.planned - a.planned);
  }, [allContracts, variance]);

  const openAccrualCount = accruals?.rows?.length ?? 0;
  const overBudgetCount  = (variance?.rows as VarianceRow[] ?? []).filter(r => r.totalStatus === 'OVER_BUDGET').length;

  function openNewContract() {
    setNewContractName(''); setNewContractVendor(''); setNewContractOwner('');
    setNewContractStartDate(''); setNewContractError('');
    setNewContractOpen(true);
  }

  async function submitNewContract() {
    if (!newContractName.trim())   { setNewContractError('Name is required');       return; }
    if (!newContractVendor.trim()) { setNewContractError('Vendor is required');     return; }
    if (!newContractStartDate)     { setNewContractError('Start date is required'); return; }
    try {
      const result = await api.post<{ contractId: string }>('/contracts', {
        name: newContractName, vendor: newContractVendor,
        ownerUser: newContractOwner, startDate: newContractStartDate,
        status: 'ACTIVE', createdBy: 'system',
      });
      setRefetchKey(k => k + 1);
      setNewContractOpen(false);
      navigate(`/contracts/${result.contractId}`);
    } catch (e) {
      setNewContractError(String(e));
    }
  }

  return (
    <div className={styles.page}>

      {/* ── Page header ── */}
      <div className={styles.pageHeader}>
        <h2 className={styles.pageTitle}>Portfolio</h2>
        <div className={styles.headerActions}>
          <select
            className={styles.fySelect}
            value={fiscalYear}
            onChange={e => setFiscalYear(e.target.value)}
          >
            {FISCAL_YEARS.map(fy => <option key={fy}>{fy}</option>)}
          </select>
          <button className={styles.btnPrimary} onClick={openNewContract}>
            + New Contract
          </button>
        </div>
      </div>

      {vLoading && <p className={styles.loading}>Loading…</p>}

      {/* ── KPI cards ── */}
      <div className={styles.kpiRow}>
        <div className={styles.kpiCard}>
          <div className={styles.kpiLabel}>TOTAL BUDGET</div>
          <div className={styles.kpiValue}>{formatMillions(totals.planned)}</div>
        </div>
        <div className={styles.kpiCard}>
          <div className={styles.kpiLabel}>TOTAL ACTUALS</div>
          <div className={styles.kpiValue}>{formatMillions(totals.actual)}</div>
        </div>
        <div className={styles.kpiCard}>
          <div className={styles.kpiLabel}>VARIANCE</div>
          <div className={`${styles.kpiValue} ${totals.variance < 0 ? styles.kpiOver : styles.kpiNeutral}`}>
            {formatMillions(Math.abs(totals.variance))}
          </div>
          <div className={styles.kpiSub}>{spentPct}% spent</div>
          <div className={styles.progressBar}>
            <div className={styles.progressFill} style={{ width: `${Math.min(spentPct, 100)}%` }} />
          </div>
        </div>
        <div className={styles.kpiCard}>
          <div className={styles.kpiLabel}>OPEN ACCRUALS</div>
          <div className={`${styles.kpiValue} ${openAccrualCount > 0 ? styles.kpiWarn : ''}`}>
            {openAccrualCount}
          </div>
          <div className={styles.kpiSub}>{openAccrualCount > 0 ? 'require attention' : 'all clear'}</div>
        </div>
      </div>

      {/* ── Alerts strip ── */}
      {(openAccrualCount > 0 || overBudgetCount > 0) && (
        <div className={styles.alertsStrip}>
          {openAccrualCount > 0 && (
            <span className={styles.alertChip} onClick={() => navigate('/reports')}>
              ⚠ {openAccrualCount} open accrual{openAccrualCount !== 1 ? 's' : ''} aging
            </span>
          )}
          {overBudgetCount > 0 && (
            <span className={`${styles.alertChip} ${styles.alertChipRed}`} onClick={() => navigate('/reports')}>
              ● {overBudgetCount} contract{overBudgetCount !== 1 ? 's' : ''} over budget
            </span>
          )}
        </div>
      )}

      {/* ── Contract cards ── */}
      <div className={styles.contractList}>
        {contractSummary.map(c => (
          <div key={c.contractId} className={styles.contractCard}>
            {/* Card header */}
            <div
              className={styles.cardHeader}
              onClick={() => navigate(`/contracts/${c.contractId}`)}
              role="button"
              tabIndex={0}
              onKeyDown={e => e.key === 'Enter' && navigate(`/contracts/${c.contractId}`)}
            >
              <div className={styles.cardHeaderLeft}>
                <span className={styles.contractName}>{c.contractName}</span>
                <StatusBadge status={c.status} size="sm" />
              </div>
              <div className={styles.cardHeaderRight}>
                <span className={styles.cardKpi}>
                  <span className={styles.cardKpiLabel}>Planned</span>
                  <span className={styles.cardKpiValue}>{formatMillions(c.planned)}</span>
                </span>
                <span className={styles.cardKpiDiv} />
                <span className={styles.cardKpi}>
                  <span className={styles.cardKpiLabel}>Actual</span>
                  <span className={styles.cardKpiValue}>{formatMillions(c.actual)}</span>
                </span>
                <span className={styles.cardKpiDiv} />
                <span className={styles.cardKpi}>
                  <span className={styles.cardKpiLabel}>Variance</span>
                  <span className={`${styles.cardKpiValue} ${c.variance < 0 ? styles.over : styles.neutral}`}>
                    {c.variance < 0 ? `(${formatMillions(Math.abs(c.variance))})` : formatMillions(c.variance)}
                  </span>
                </span>
                <span className={styles.navArrow}>→</span>
              </div>
            </div>

            {/* Project rows */}
            {c.projects.length > 0 && (
              <div className={styles.projectList}>
                <div className={styles.projectListHeader}>
                  <span>Project</span>
                  <span>Planned</span>
                  <span>Actual</span>
                  <span>Variance</span>
                </div>
                {c.projects.map(p => (
                  <div
                    key={p.projectId}
                    className={styles.projectRow}
                    onClick={() => navigate(`/projects/${p.projectId}`)}
                    role="button"
                    tabIndex={0}
                    onKeyDown={e => e.key === 'Enter' && navigate(`/projects/${p.projectId}`)}
                  >
                    <span className={styles.projectName}>
                      <span className={styles.projectId}>{p.projectId}</span>
                      {p.projectName}
                    </span>
                    <span className={styles.projectNum}>{formatCurrency(p.totalPlanned)}</span>
                    <span className={styles.projectNum}>{formatCurrency(p.totalActual)}</span>
                    <span className={`${styles.projectNum} ${p.totalVariance < 0 ? styles.over : styles.neutral}`}>
                      {p.totalVariance < 0
                        ? `(${formatCurrency(Math.abs(p.totalVariance))})`
                        : formatCurrency(p.totalVariance)}
                    </span>
                  </div>
                ))}
              </div>
            )}
            {c.projects.length === 0 && (
              <div className={styles.noProjects}>No projects yet — click to add one</div>
            )}
          </div>
        ))}

        {contractSummary.length === 0 && !vLoading && (
          <div className={styles.emptyState}>
            <p>No contracts for {fiscalYear}.</p>
            <button className={styles.btnPrimary} onClick={openNewContract}>+ New Contract</button>
          </div>
        )}
      </div>

      {/* ── New Contract drawer ── */}
      <Drawer open={newContractOpen} onClose={() => setNewContractOpen(false)} title="New Contract">
        <FormField label="Name" required value={newContractName} onChange={e => setNewContractName((e.target as HTMLInputElement).value)} />
        <FormField label="Vendor" required value={newContractVendor} onChange={e => setNewContractVendor((e.target as HTMLInputElement).value)} />
        <FormField label="Owner" value={newContractOwner} onChange={e => setNewContractOwner((e.target as HTMLInputElement).value)} />
        <FormField label="Start Date" type="date" required value={newContractStartDate} onChange={e => setNewContractStartDate((e.target as HTMLInputElement).value)} />
        {newContractError && <div className={styles.formError}>{newContractError}</div>}
        <button className={styles.btnPrimary} onClick={submitNewContract}>Create Contract</button>
      </Drawer>
    </div>
  );
}
