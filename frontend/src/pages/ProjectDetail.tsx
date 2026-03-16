/**
 * Project Detail — full view of a project with cashflow table and milestone management.
 * Spec: 21-ui-ux-spec.md §6.3 Project Detail
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
import styles from './ProjectDetail.module.css';

interface ProjectResponse {
  projectId: string;
  name: string;
  wbse: string;
  fundingSource: string;
  status: string;
  contract?: { contractId: string; name: string };
}

interface FiscalPeriod {
  periodId: string;
  periodKey: string;
}

export default function ProjectDetail() {
  const { projectId } = useParams<{ projectId: string }>();
  const { asOfDate } = useTimeMachine();
  const navigate = useNavigate();

  const [refetchKey, setRefetchKey] = useState(0);
  const { data: project, loading: pLoading } = useApi<ProjectResponse>(`/projects/${projectId}`, [refetchKey]);
  const { data: fiscalPeriods } = useApi<FiscalPeriod[]>('/fiscal-years/FY26/periods');

  // Add Milestone drawer
  const [addMsOpen, setAddMsOpen] = useState(false);
  const [msName, setMsName]         = useState('');
  const [msAmount, setMsAmount]     = useState('');
  const [msPeriod, setMsPeriod]     = useState('');
  const [msEffective, setMsEffective] = useState('');
  const [msReason, setMsReason]     = useState('Initial');
  const [msError, setMsError]       = useState('');

  // Bottom sheet for milestone cell clicks
  const [sheetMilestoneId, setSheetMilestoneId]     = useState<string | null>(null);
  const [sheetMilestoneName, setSheetMilestoneName] = useState('');
  const [sheetPeriodKey, setSheetPeriodKey]         = useState('');

  async function submitAddMilestone() {
    if (!msName.trim() || !msAmount.trim() || !msPeriod) {
      setMsError('Name, amount and fiscal period are required');
      return;
    }
    try {
      await api.post(`/projects/${projectId}/milestones`, {
        name: msName,
        plannedAmount: parseFloat(msAmount),
        fiscalPeriodId: msPeriod,
        effectiveDate: msEffective || new Date().toISOString().split('T')[0],
        reason: msReason || 'Initial',
      });
      setAddMsOpen(false);
      setRefetchKey(k => k + 1);
    } catch (e) {
      setMsError(String(e));
    }
  }

  if (pLoading) return <p className={styles.loading}>Loading…</p>;
  if (!project) return <p className={styles.loading}>Project not found.</p>;

  const contractId   = project.contract?.contractId;
  const contractName = project.contract?.name;

  return (
    <div className={styles.page}>

      {/* ── Breadcrumb ── */}
      <nav className={styles.breadcrumb}>
        <span className={styles.breadLink} onClick={() => navigate('/')}>Portfolio</span>
        {contractId && (
          <>
            <span className={styles.breadSep}>/</span>
            <span className={styles.breadLink} onClick={() => navigate(`/contracts/${contractId}`)}>
              {contractName}
            </span>
          </>
        )}
        <span className={styles.breadSep}>/</span>
        <span className={styles.breadCurrent}>{project.name}</span>
      </nav>

      {/* ── Project header ── */}
      <div className={styles.pageHeader}>
        <div className={styles.headerLeft}>
          <div className={styles.titleRow}>
            <span className={styles.projectIdBadge}>{projectId}</span>
            <h2 className={styles.pageTitle}>{project.name}</h2>
            <StatusBadge status={project.status} />
          </div>
          <div className={styles.meta}>
            <span>WBSE: <strong>{project.wbse}</strong></span>
            <span className={styles.metaDot}>·</span>
            <span>Funding: <strong>{project.fundingSource}</strong></span>
            {asOfDate && (
              <>
                <span className={styles.metaDot}>·</span>
                <span>As of: <strong>{asOfDate}</strong></span>
              </>
            )}
          </div>
        </div>
        <div className={styles.headerActions}>
          <button
            className={styles.btnGhost}
            onClick={() => navigate(`/admin/audit?entityType=PROJECT&entityId=${projectId}`)}
          >
            Audit Trail
          </button>
        </div>
      </div>

      {/* ── Milestones / cashflow ── */}
      <div className={styles.section}>
        <div className={styles.sectionHeader}>
          <h3 className={styles.sectionTitle}>CASHFLOW</h3>
          <button
            className={styles.btnSm}
            onClick={() => {
              setMsName(''); setMsAmount(''); setMsPeriod('');
              setMsEffective(''); setMsReason('Initial'); setMsError('');
              setAddMsOpen(true);
            }}
          >
            + Add Milestone
          </button>
        </div>

        <CashflowTable
          projectId={projectId!}
          fiscalYear="FY26"
          onCellClick={(milestoneId, periodKey, milestoneName) => {
            setSheetMilestoneId(milestoneId);
            setSheetMilestoneName(milestoneName);
            setSheetPeriodKey(periodKey);
          }}
        />
      </div>

      {/* ── Add milestone drawer ── */}
      <Drawer open={addMsOpen} onClose={() => setAddMsOpen(false)} title="Add Milestone">
        <FormField label="Name" required value={msName}
          onChange={e => setMsName((e.target as HTMLInputElement).value)} />
        <FormField label="Planned Amount ($)" required type="number" value={msAmount}
          onChange={e => setMsAmount((e.target as HTMLInputElement).value)} />
        <FormField label="Fiscal Period" as="select" required value={msPeriod}
          onChange={e => setMsPeriod((e.target as HTMLSelectElement).value)}>
          <option value="">— select —</option>
          {(fiscalPeriods ?? []).map(p => (
            <option key={p.periodId} value={p.periodId}>{p.periodKey}</option>
          ))}
        </FormField>
        <FormField label="Effective Date" type="date" value={msEffective}
          onChange={e => setMsEffective((e.target as HTMLInputElement).value)} />
        <FormField label="Reason" required value={msReason}
          onChange={e => setMsReason((e.target as HTMLInputElement).value)} />
        {msError && <div className={styles.formError}>{msError}</div>}
        <button className={styles.btnPrimary} onClick={submitAddMilestone}>Add Milestone</button>
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
