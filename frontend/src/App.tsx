import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { TimeMachineProvider } from './context/TimeMachineContext';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import ErrorBoundary from './components/ErrorBoundary';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import ContractDetail from './pages/ContractDetail';
import ProjectDetail from './pages/ProjectDetail';
import MilestoneDetail from './pages/MilestoneDetail';
import SapImport from './pages/SapImport';
import ImportReview from './pages/ImportReview';
import ReconcileWorkspace from './pages/ReconcileWorkspace';
import ReportsHub from './pages/reports/ReportsHub';
import BudgetReport from './pages/reports/BudgetReport';
import VarianceReport from './pages/reports/VarianceReport';
import ReconciliationReport from './pages/reports/ReconciliationReport';
import ForecastReport from './pages/reports/ForecastReport';
import FundingReport from './pages/reports/FundingReport';
import OpenAccrualsReport from './pages/reports/OpenAccrualsReport';
import JournalViewer from './pages/JournalViewer';
import Settings from './pages/Settings';
import LoginPage from './pages/LoginPage';
import AdminUsers from './pages/AdminUsers';
import AdminFiscalYears from './pages/AdminFiscalYears';
import AdminAuditLog from './pages/AdminAuditLog';
import AdminReferenceData from './pages/AdminReferenceData';

function AppRoutes() {
  const { token } = useAuth();
  if (!token) return <LoginPage />;
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="contracts" element={<Dashboard />} />
          <Route path="reports" element={<ReportsHub />} />
          <Route path="contracts/:contractId" element={<ContractDetail />} />
          <Route path="projects/:projectId" element={<ProjectDetail />} />
          <Route path="milestones/:milestoneId" element={<MilestoneDetail />} />
          <Route path="import" element={<SapImport />} />
          <Route path="import/:importId" element={<ImportReview />} />
          <Route path="reconcile" element={<ReconcileWorkspace />} />
          <Route path="reports/budget" element={<BudgetReport />} />
          <Route path="reports/variance" element={<VarianceReport />} />
          <Route path="reports/reconciliation" element={<ReconciliationReport />} />
          <Route path="reports/forecast" element={<ForecastReport />} />
          <Route path="reports/funding" element={<FundingReport />} />
          <Route path="reports/accruals" element={<OpenAccrualsReport />} />
          <Route path="journal" element={<JournalViewer />} />
          <Route path="settings" element={<Settings />} />
          <Route path="admin/users" element={<AdminUsers />} />
          <Route path="admin/fiscal-years" element={<AdminFiscalYears />} />
          <Route path="admin/audit" element={<AdminAuditLog />} />
          <Route path="admin/reference-data" element={<AdminReferenceData />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <TimeMachineProvider>
          <AppRoutes />
        </TimeMachineProvider>
      </AuthProvider>
    </ErrorBoundary>
  );
}
