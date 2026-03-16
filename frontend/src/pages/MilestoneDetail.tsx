import { useParams, useNavigate } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { useTimeMachine } from '../context/TimeMachineContext';
import { FiscalYear } from '../api/types';
import MilestonePanel from './MilestonePanel';
import styles from './Detail.module.css';

interface FiscalPeriod { periodId: string; periodKey: string; }

export default function MilestoneDetail() {
  const { milestoneId } = useParams<{ milestoneId: string }>();
  const { asOfDate } = useTimeMachine();
  const navigate = useNavigate();
  const { data: fiscalYears } = useApi<FiscalYear[]>(`/fiscal-years`);
  const latestFiscalYear = fiscalYears && fiscalYears.length > 0
    ? [...fiscalYears].sort((a, b) => b.fiscalYear.localeCompare(a.fiscalYear))[0].fiscalYear
    : null;
  const { data: fiscalPeriods } = useApi<FiscalPeriod[]>(latestFiscalYear ? `/fiscal-years/${latestFiscalYear}/periods` : null);

  if (!milestoneId) return <p>No milestone ID.</p>;

  return (
    <div className={styles.page}>
      <div className={styles.breadcrumb}>
        <span className={styles.breadLink} onClick={() => navigate('/')}>Dashboard</span>
        <span className={styles.sep}>/</span>
        <span>Milestone Detail</span>
      </div>
      <h2>Milestone</h2>
      <MilestonePanel
        milestoneId={milestoneId}
        milestoneName=""
        asOfDate={asOfDate}
        fiscalPeriods={fiscalPeriods ?? []}
      />
    </div>
  );
}
