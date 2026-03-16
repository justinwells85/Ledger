/**
 * Contract Detail — full view of a contract with project cards and cashflow tables.
 * Spec: 21-ui-ux-spec.md §6.2 Contract Detail
 */
import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { useTimeMachine } from '../context/TimeMachineContext';
import { api } from '../api/client';
import Drawer from '../components/Drawer';
import FormField from '../components/FormField';
import StatusBadge from '../components/StatusBadge';
import CashflowTable from '../components/CashflowTable';
import BottomSheet from '../components/BottomSheet';
import MilestoneDetailSheet from '../components/MilestoneDetailSheet';
import { formatMillions, formatCurrency } from '../utils/format';
import styles from './ContractDetail.module.css';

interface ContractResponse {
  contractId: string;
  name: string;
  vendor: string;
  ownerUser: string;
  startDate: string;
  endDate: string | null;
  status: string;
}

interface ProjectResponse {
  projectId: string;
  name: string;
  wbse: string;
  fundingSource: string;
  status: string;
}

interface VarianceRow {
  projectId: string;
  projectName: string;
  totalPlanned: number;
  totalActual: number;
  totalVariance: number;
  totalStatus: string;
}

interface VarianceReport { rows: VarianceRow[] }

export default function ContractDetail() {
  const { contractId } = useParams<{ contractId: string }>();
  const { asOfDate } = useTimeMachine();
  const navigate = useNavigate();

  const [refetchKey, setRefetchKey] = useState(0);
  const { data: contract, loading: cLoading } = useApi<ContractResponse>(`/contracts/${contractId}`, [refetchKey]);
  const { data: projects } = useApi<ProjectResponse[]>(`/contracts/${contractId}/projects`, [refetchKey]);
  const { data: variance } = useApi<VarianceReport>(
    contractId ? `/reports/variance?fiscalYear=FY26&contractId=${contractId}${asOfDate ? `&asOfDate=${asOfDate}` : ''}` : null,
    [refetchKey]
  );
  const { data: fiscalPeriods } = useApi<{ periodId: string; periodKey: string }[]>('/fiscal-years/FY26/periods');

  const [editOpen, setEditOpen]             = useState(false);
  const [addProjectOpen, setAddProjectOpen] = useState(false);

  // Bottom sheet for milestone cell clicks
  const [sheetMilestoneId, setSheetMilestoneId]   = useState<string | null>(null);
  const [sheetMilestoneName, setSheetMilestoneName] = useState('');
  const [sheetPeriodKey, setSheetPeriodKey]         = useState('');

  // Edit form
  const [editName, setEditName]     = useState('');
  const [editOwner, setEditOwner]   = useState('');
  const [editStatus, setEditStatus] = useState('');
  const [editReason, setEditReason] = useState('');
  const [editError, setEditError]   = useState('');

  // Add project form
  const [projId, setProjId]         = useState('');
  const [projName, setProjName]     = useState('');
  const [projWbse, setProjWbse]     = useState('');
  const [projFunding, setProjFunding] = useState('OPEX');
  const [projError, setProjError]   = useState('');

  function openEdit() {
    if (!contract) return;
    setEditName(contract.name); setEditOwner(contract.ownerUser);
    setEditStatus(contract.status); setEditReason(''); setEditError('');
    setEditOpen(true);
  }

  async function submitEdit() {
    if (!editReason.trim()) { setEditError('Reason is required'); return; }
    try {
      await api.put(`/contracts/${contractId}`, { name: editName, ownerUser: editOwner, status: editStatus, reason: editReason });
      setEditOpen(false); setRefetchKey(k => k + 1);
    } catch (e) { setEditError(String(e)); }
  }

  async function submitAddProject() {
    if (!projId.trim() || !projName.trim() || !projWbse.trim()) {
      setProjError('Project ID, name and WBSE are required'); return;
    }
    try {
      await api.post(`/contracts/${contractId}/projects`, {
        projectId: projId, name: projName, wbse: projWbse,
        fundingSource: projFunding, status: 'ACTIVE', createdBy: 'system',
      });
      setAddProjectOpen(false); setRefetchKey(k => k + 1);
    } catch (e) { setProjError(String(e)); }
  }

  function handleCellClick(milestoneId: string, periodKey: string, milestoneName: string) {
    setSheetMilestoneId(milestoneId);
    setSheetMilestoneName(milestoneName);
    setSheetPeriodKey(periodKey);
  }

  const varianceMap = new Map(variance?.rows?.map(r => [r.projectId, r]) ?? []);

  const contractTotals = variance?.rows?.reduce(
    (a, r) => ({ planned: a.planned + r.totalPlanned, actual: a.actual + r.totalActual, variance: a.variance + r.totalVariance }),
    { planned: 0, actual: 0, variance: 0 }
  ) ?? { planned: 0, actual: 0, variance: 0 };

  if (cLoading) return <p className={styles.loading}>Loading…</p>;
  if (!contract) return <p className={styles.loading}>Contract not found.</p>;

  return (
    <div className={styles.page}>

      {/* ── Breadcrumb ── */}
      <nav className={styles.breadcrumb}>
        <span className={styles.breadLink} onClick={() => navigate('/')}>Portfolio</span>
        <span className={styles.breadSep}>/</span>
        <span className={styles.breadCurrent}>{contract.name}</span>
      </nav>

      {/* ── Contract header ── */}
      <div className={styles.pageHeader}>
        <div className={styles.headerLeft}>
          <div className={styles.titleRow}>
            <h2 className={styles.pageTitle}>{contract.name}</h2>
            <StatusBadge status={contract.status} />
          </div>
          <div className={styles.meta}>
            <span>Vendor: <strong>{contract.vendor}</strong></span>
            <span className={styles.metaDot}>·</span>
            <span>Owner: <strong>{contract.ownerUser}</strong></span>
            {contract.startDate && (
              <>
                <span className={styles.metaDot}>·</span>
                <span>From: <strong>{contract.startDate}</strong></span>
              </>
            )}
          </div>
        </div>
        <div className={styles.headerActions}>
          <button className={styles.btnGhost} onClick={() => navigate(`/admin/audit?entityType=CONTRACT&entityId=${contractId}`)}>
            Audit Trail
          </button>
          <button className={styles.btnPrimary} onClick={openEdit}>Edit</button>
        </div>
      </div>

      {/* ── KPI strip ── */}
      <div className={styles.kpiStrip}>
        <div className={styles.kpiItem}>
          <div className={styles.kpiLabel}>PLANNED</div>
          <div className={styles.kpiValue}>{formatMillions(contractTotals.planned)}</div>
        </div>
        <div className={styles.kpiDivider} />
        <div className={styles.kpiItem}>
          <div className={styles.kpiLabel}>ACTUAL</div>
          <div className={styles.kpiValue}>{formatMillions(contractTotals.actual)}</div>
        </div>
        <div className={styles.kpiDivider} />
        <div className={styles.kpiItem}>
          <div className={styles.kpiLabel}>VARIANCE</div>
          <div className={`${styles.kpiValue} ${contractTotals.variance < 0 ? styles.kpiOver : styles.kpiNeutral}`}>
            {contractTotals.variance < 0
              ? `(${formatMillions(Math.abs(contractTotals.variance))})`
              : formatMillions(contractTotals.variance)}
          </div>
        </div>
      </div>

      {/* ── Projects section ── */}
      <div className={styles.projectsSection}>
        <div className={styles.sectionHeader}>
          <h3 className={styles.sectionTitle}>PROJECTS</h3>
          <button
            className={styles.btnSm}
            onClick={() => { setProjId(''); setProjName(''); setProjWbse(''); setProjFunding('OPEX'); setProjError(''); setAddProjectOpen(true); }}
          >
            + Add Project
          </button>
        </div>

        {(projects ?? []).length === 0 && (
          <div className={styles.emptyState}>No projects yet.</div>
        )}

        <div className={styles.projectCards}>
          {(projects ?? []).map(p => {
            const v = varianceMap.get(p.projectId);
            return (
              <div key={p.projectId} className={styles.projectCard}>
                {/* Project card header */}
                <div
                  className={styles.projectCardHeader}
                  onClick={() => navigate(`/projects/${p.projectId}`)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={e => e.key === 'Enter' && navigate(`/projects/${p.projectId}`)}
                >
                  <div className={styles.projectCardLeft}>
                    <span className={styles.projectId}>{p.projectId}</span>
                    <span className={styles.projectName}>{p.name}</span>
                    <span className={styles.projectMeta}>{p.wbse}</span>
                    <span className={styles.projectMeta}>{p.fundingSource}</span>
                    <StatusBadge status={p.status} size="sm" />
                  </div>
                  {v && (
                    <div className={styles.projectCardRight}>
                      <span className={styles.projKpi}>
                        <span className={styles.projKpiLabel}>Planned</span>
                        <span className={styles.projKpiValue}>{formatCurrency(v.totalPlanned)}</span>
                      </span>
                      <span className={styles.projKpi}>
                        <span className={styles.projKpiLabel}>Actual</span>
                        <span className={styles.projKpiValue}>{formatCurrency(v.totalActual)}</span>
                      </span>
                      <span className={styles.projKpi}>
                        <span className={styles.projKpiLabel}>Variance</span>
                        <span className={`${styles.projKpiValue} ${v.totalVariance < 0 ? styles.over : styles.neutral}`}>
                          {v.totalVariance < 0
                            ? `(${formatCurrency(Math.abs(v.totalVariance))})`
                            : formatCurrency(v.totalVariance)}
                        </span>
                      </span>
                      <span className={styles.navArrow}>→</span>
                    </div>
                  )}
                </div>

                {/* Cashflow table */}
                <div className={styles.cashflowWrap}>
                  <CashflowTable
                    projectId={p.projectId}
                    fiscalYear="FY26"
                    onCellClick={(milestoneId, periodKey) => handleCellClick(milestoneId, periodKey, p.name)}
                  />
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* ── Edit contract drawer ── */}
      <Drawer open={editOpen} onClose={() => setEditOpen(false)} title="Edit Contract">
        <FormField label="Name" required value={editName} onChange={e => setEditName((e.target as HTMLInputElement).value)} />
        <FormField label="Owner" required value={editOwner} onChange={e => setEditOwner((e.target as HTMLInputElement).value)} />
        <FormField label="Status" as="select" value={editStatus} onChange={e => setEditStatus((e.target as HTMLSelectElement).value)}>
          <option value="ACTIVE">ACTIVE</option>
          <option value="CLOSED">CLOSED</option>
          <option value="TERMINATED">TERMINATED</option>
        </FormField>
        <FormField label="Reason for change" required value={editReason} onChange={e => setEditReason((e.target as HTMLInputElement).value)} error={editError} />
        <button className={styles.btnPrimary} onClick={submitEdit}>Save Changes</button>
      </Drawer>

      {/* ── Add project drawer ── */}
      <Drawer open={addProjectOpen} onClose={() => setAddProjectOpen(false)} title="Add Project">
        <FormField label="Project ID (e.g. PR13752)" required value={projId} onChange={e => setProjId((e.target as HTMLInputElement).value)} />
        <FormField label="Name" required value={projName} onChange={e => setProjName((e.target as HTMLInputElement).value)} />
        <FormField label="WBSE" required value={projWbse} onChange={e => setProjWbse((e.target as HTMLInputElement).value)} />
        <FormField label="Funding Source" as="select" value={projFunding} onChange={e => setProjFunding((e.target as HTMLSelectElement).value)}>
          <option value="OPEX">OPEX</option>
          <option value="CAPEX">CAPEX</option>
          <option value="OTHER_TEAM">OTHER_TEAM</option>
        </FormField>
        {projError && <div className={styles.formError}>{projError}</div>}
        <button className={styles.btnPrimary} onClick={submitAddProject}>Add Project</button>
      </Drawer>

      {/* ── Milestone detail bottom sheet ── */}
      <BottomSheet
        open={!!sheetMilestoneId}
        onClose={() => setSheetMilestoneId(null)}
        title={sheetMilestoneName}
        subtitle={sheetPeriodKey}
      >
        {sheetMilestoneId && (
          <MilestoneDetailSheet
            milestoneId={sheetMilestoneId}
            fiscalPeriods={fiscalPeriods ?? []}
            onUpdated={() => setRefetchKey(k => k + 1)}
          />
        )}
      </BottomSheet>
    </div>
  );
}
