/**
 * Reports Hub — unified tabbed container for all report views.
 * Manages shared fiscal year selector; passes fiscalYear down to each report.
 * Spec: 21-ui-ux-spec.md §6.5 Reports Hub
 */
import { useState } from 'react';
import BudgetReport from './BudgetReport';
import VarianceReport from './VarianceReport';
import ReconciliationReport from './ReconciliationReport';
import ForecastReport from './ForecastReport';
import FundingReport from './FundingReport';
import OpenAccrualsReport from './OpenAccrualsReport';
import styles from './ReportsHub.module.css';

type TabId = 'forecast' | 'variance' | 'reconciliation' | 'accruals' | 'funding' | 'budget';

const TABS: { id: TabId; label: string }[] = [
  { id: 'forecast',       label: 'Forecast & Budget' },
  { id: 'variance',       label: 'Variance' },
  { id: 'reconciliation', label: 'Reconciliation Status' },
  { id: 'accruals',       label: 'Open Accruals' },
  { id: 'funding',        label: 'Funding' },
  { id: 'budget',         label: 'Budget Plan' },
];

const FISCAL_YEARS = ['FY26', 'FY27', 'FY25'];

export default function ReportsHub() {
  const [activeTab, setActiveTab]   = useState<TabId>('forecast');
  const [fiscalYear, setFiscalYear] = useState('FY26');

  return (
    <div className={styles.hub}>

      {/* ── Hub header ── */}
      <div className={styles.hubHeader}>
        <h2 className={styles.hubTitle}>Reports</h2>
        <select
          className={styles.fySelect}
          value={fiscalYear}
          onChange={e => setFiscalYear(e.target.value)}
          aria-label="Fiscal year"
        >
          {FISCAL_YEARS.map(fy => <option key={fy}>{fy}</option>)}
        </select>
      </div>

      {/* ── Tab bar ── */}
      <div className={styles.tabBar}>
        {TABS.map(tab => (
          <button
            key={tab.id}
            className={`${styles.tab} ${activeTab === tab.id ? styles.tabActive : ''}`}
            onClick={() => setActiveTab(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* ── Tab content ── */}
      <div className={styles.tabContent}>
        {activeTab === 'forecast'       && <ForecastReport       fiscalYear={fiscalYear} />}
        {activeTab === 'variance'       && <VarianceReport       fiscalYear={fiscalYear} />}
        {activeTab === 'reconciliation' && <ReconciliationReport fiscalYear={fiscalYear} />}
        {activeTab === 'accruals'       && <OpenAccrualsReport   fiscalYear={fiscalYear} />}
        {activeTab === 'funding'        && <FundingReport        fiscalYear={fiscalYear} />}
        {activeTab === 'budget'         && <BudgetReport         fiscalYear={fiscalYear} />}
      </div>
    </div>
  );
}
