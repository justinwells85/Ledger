/**
 * Tests for JournalViewer page component.
 * Spec: 02-journal-ledger.md — Journal Ledger viewer
 */
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../test/renderWithProviders';
import JournalViewer from './JournalViewer';

describe('JournalViewer', () => {
  it('renders journal entries', async () => {
    renderWithProviders(<JournalViewer />);

    // The type badges appear in both the select options and the table rows.
    // Use findAllByText so multiple matches are acceptable.
    await screen.findAllByText('PLAN_CREATE');

    expect(screen.getAllByText('ACTUAL_IMPORT').length).toBeGreaterThan(0);
    expect(screen.getAllByText('PLAN_ADJUST').length).toBeGreaterThan(0);
  });

  it('expands entry to show lines', async () => {
    const user = userEvent.setup();
    renderWithProviders(<JournalViewer />);

    // Description text is split across React nodes as "Initial budget ▼", match with regex
    await screen.findByText(/Initial budget/);

    await user.click(screen.getByText(/Initial budget/));

    await screen.findByText('PLANNED');
  });

  it('filters by entry type', async () => {
    const user = userEvent.setup();
    renderWithProviders(<JournalViewer />);

    await screen.findAllByText('PLAN_CREATE');

    await user.selectOptions(screen.getByRole('combobox'), 'PLAN_CREATE');

    // After filtering, PLAN_CREATE still exists (in both option and table); others only in option
    // Check ACTUAL_IMPORT type badge is gone from the table (only option remains)
    const actualImportElements = screen.getAllByText('ACTUAL_IMPORT');
    // Only the select option should remain; the table row badge should be gone
    expect(actualImportElements.every(el => el.tagName === 'OPTION')).toBe(true);
    const planAdjustElements = screen.getAllByText('PLAN_ADJUST');
    expect(planAdjustElements.every(el => el.tagName === 'OPTION')).toBe(true);
  });
});
