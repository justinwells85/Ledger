/**
 * Tests for MilestoneDetail / MilestonePanel version history display.
 * T44 — Spec: 04-milestone-versioning.md, 13-api-design.md Section 5
 */
import { screen } from '@testing-library/react';
import { Routes, Route } from 'react-router-dom';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '../test/server';
import { TimeMachineProvider } from '../context/TimeMachineContext';
import MilestoneDetail from './MilestoneDetail';
import { MILESTONE_ID, versionsWithDeltaFixture } from '../test/fixtures';

function renderMilestoneDetail() {
  return render(
    <TimeMachineProvider>
      <MemoryRouter initialEntries={[`/milestones/${MILESTONE_ID}`]}>
        <Routes>
          <Route path="/milestones/:milestoneId" element={<MilestoneDetail />} />
        </Routes>
      </MemoryRouter>
    </TimeMachineProvider>
  );
}

describe('MilestoneDetail version history', () => {
  beforeEach(() => {
    server.use(
      http.get('/api/v1/milestones/:milestoneId/versions', () =>
        HttpResponse.json(versionsWithDeltaFixture)
      )
    );
  });

  it('renders versions in ascending order by versionNumber', async () => {
    renderMilestoneDetail();

    const v1 = await screen.findByText('v1');
    const v2 = screen.getByText('v2');
    const v3 = screen.getByText('v3');

    expect(v1).toBeInTheDocument();
    expect(v2).toBeInTheDocument();
    expect(v3).toBeInTheDocument();

    // Confirm DOM order: v1 before v2 before v3
    const all = screen.getAllByText(/^v\d+$/);
    const labels = all.map(el => el.textContent);
    expect(labels.indexOf('v1')).toBeLessThan(labels.indexOf('v2'));
    expect(labels.indexOf('v2')).toBeLessThan(labels.indexOf('v3'));
  });

  it('shows Initial label for v1 and positive delta for v2', async () => {
    renderMilestoneDetail();

    await screen.findByText('v1');

    // "Initial" delta label for the first version
    expect(screen.getByText('Initial')).toBeInTheDocument();
    // v2 delta: 30000 - 25000 = +$5,000.00
    expect(screen.getByText('+$5,000.00')).toBeInTheDocument();
  });

  it('shows CANCELLED badge for version with plannedAmount 0', async () => {
    renderMilestoneDetail();

    await screen.findByText('v3');

    expect(screen.getByText('CANCELLED')).toBeInTheDocument();
  });

  it('shows createdBy for each version', async () => {
    renderMilestoneDetail();

    await screen.findByText('v1');

    expect(screen.getByText('system')).toBeInTheDocument();
    expect(screen.getByText('alice')).toBeInTheDocument();
    expect(screen.getByText('bob')).toBeInTheDocument();
  });
});
