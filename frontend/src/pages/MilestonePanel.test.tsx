import { screen, fireEvent } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../test/server';
import { renderWithProviders } from '../test/renderWithProviders';
import MilestonePanel from './MilestonePanel';
import { MILESTONE_ID } from '../test/fixtures';

describe('MilestonePanel', () => {
  const defaultProps = {
    milestoneId: MILESTONE_ID,
    milestoneName: 'January Sustainment',
    asOfDate: null,
    fiscalPeriods: [],
  };

  // Override versions handler to return non-cancelled milestone (plannedAmount > 0)
  beforeEach(() => {
    server.use(
      http.get('/api/v1/milestones/:milestoneId/versions', () =>
        HttpResponse.json([
          { versionId: 'v1', versionNumber: 1, plannedAmount: 25000, fiscalPeriodKey: 'FY26-04-JAN', effectiveDate: '2025-11-01', reason: 'Initial', createdBy: 'system' },
        ])
      )
    );
  });

  it('shows Cancel Milestone button when milestone is not cancelled', async () => {
    renderWithProviders(<MilestonePanel {...defaultProps} />);

    await screen.findByText('Cancel Milestone');

    expect(screen.getByText('Cancel Milestone')).toBeInTheDocument();
  });

  it('clicking Cancel Milestone shows confirmation form', async () => {
    renderWithProviders(<MilestonePanel {...defaultProps} />);

    await screen.findByText('Cancel Milestone');
    fireEvent.click(screen.getByText('Cancel Milestone'));

    expect(screen.getByText('Confirm Cancel')).toBeInTheDocument();
  });

  it('submitting cancel without reason shows error', async () => {
    renderWithProviders(<MilestonePanel {...defaultProps} />);

    await screen.findByText('Cancel Milestone');
    fireEvent.click(screen.getByText('Cancel Milestone'));

    // Click confirm with empty reason
    fireEvent.click(screen.getByText('Confirm Cancel'));

    expect(screen.getByText('Reason is required')).toBeInTheDocument();
  });
});
