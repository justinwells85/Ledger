/**
 * Shared TypeScript types matching the backend API response shapes.
 * Spec: 13-api-design.md
 */

export interface VarianceReportRow {
  contractId: string;
  contractName: string;
  projectId: string;
  projectName: string;
  totalPlanned: number;
  totalActual: number;
  totalVariance: number;
  totalVariancePercent: number;
  totalStatus: 'UNDER_BUDGET' | 'OVER_BUDGET' | 'WITHIN_TOLERANCE';
}

export interface VarianceReport {
  fiscalYear: string;
  asOfDate: string | null;
  rows: VarianceReportRow[];
}

export interface OpenAccrualsReportRow {
  contractName: string;
  projectName: string;
  milestoneName: string;
  fiscalPeriod: string;
  openAccrualCount: number;
  ageDays: number;
  accrualStatus: 'OPEN' | 'WARNING' | 'CRITICAL';
}

export interface OpenAccrualsReport {
  fiscalYear: string;
  rows: OpenAccrualsReportRow[];
}

export interface ActualLine {
  actualId: string;
  sapDocumentNumber: string;
  postingDate: string;
  amount: number;
  vendorName: string;
  wbse: string;
  description: string;
  duplicate?: boolean;
}

export interface FiscalYear {
  fiscalYear: string;
  startDate: string;
  endDate: string;
}

export interface Contract {
  contractId: string;
  name: string;
  vendor: string;
  ownerUser: string;
  startDate: string;
  status: string;
}

export interface Project {
  projectId: string;
  name: string;
  wbse: string;
  fundingSource: string;
  status: string;
}

export interface MilestoneVersion {
  versionId: string;
  versionNumber: number;
  plannedAmount: number;
  effectiveDate: string;
  reason: string;
  createdBy: string;
}

export interface Milestone {
  milestoneId: string;
  name: string;
  status: string;
}

export interface SapImport {
  importId: string;
  filename: string;
  status: string;
  importedAt: string;
  totalLines: number;
  newLines: number;
  duplicateLines: number;
  errorLines: number;
}

export interface ReconciliationStatusReportRow {
  contractName: string;
  projectName: string;
  milestoneName: string;
  fiscalPeriod: string;
  planned: number;
  invoiceTotal: number;
  accrualNet: number;
  totalActual: number;
  remaining: number;
  status: string;
  openAccrualCount: number;
}

export interface ReconciliationStatusReport {
  fiscalYear: string;
  asOfDate: string | null;
  rows: ReconciliationStatusReportRow[];
}

export interface BudgetReportRow {
  contractId: string;
  contractName: string;
  projectId: string;
  projectName: string;
  fundingSource: string;
  periods: Record<string, number>;
  total: number;
}

export interface BudgetReport {
  fiscalYear: string;
  asOfDate: string | null;
  rows: BudgetReportRow[];
  grandTotal: number;
}

export interface MilestoneCandidate {
  milestoneId: string;
  milestoneName: string;
  projectId: string;
  plannedAmount: number;
  relevanceScore: number;
}

export interface SystemConfig {
  configKey: string;
  configValue: string;
  description: string;
  dataType: string;
  displayGroup: string;
  displayName: string;
  displayOrder: number;
}

export interface JournalEntry {
  entryId: string;
  entryDate: string;
  effectiveDate: string;
  entryType: string;
  description: string;
  createdBy: string;
}

export interface User {
  userId: string;
  username: string;
  displayName: string;
  email: string;
  role: string;
  active: boolean;
}

export interface ReconciliationResponse {
  reconciliationId: string;
  actualId: string;
  milestoneId: string;
  category: string;
  matchNotes: string | null;
  reconciledAt: string;
  reconciledBy: string;
}
