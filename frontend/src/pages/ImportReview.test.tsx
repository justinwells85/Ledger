import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Routes, Route } from 'react-router-dom';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { TimeMachineProvider } from '../context/TimeMachineContext';
import ImportReview from './ImportReview';
import { IMPORT_ID } from '../test/fixtures';

function renderImportReview() {
  return render(
    <TimeMachineProvider>
      <MemoryRouter initialEntries={[`/import/${IMPORT_ID}`]}>
        <Routes>
          <Route path="/import/:importId" element={<ImportReview />} />
        </Routes>
      </MemoryRouter>
    </TimeMachineProvider>
  );
}

describe('ImportReview', () => {
  it('renders import metadata and lines', async () => {
    renderImportReview();

    await screen.findByText('SAP_FY26_Jan.csv');

    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('filters lines by NEW', async () => {
    const user = userEvent.setup();
    renderImportReview();

    await screen.findByText('Invoice');

    await user.click(screen.getByRole('button', { name: 'NEW' }));

    expect(screen.getByText('Invoice')).toBeInTheDocument();
    expect(screen.getByText('Accrual')).toBeInTheDocument();
    expect(screen.queryByText('Old line')).not.toBeInTheDocument();
  });

  it('shows Commit and Reject buttons for STAGED import', async () => {
    renderImportReview();

    await screen.findByText('Commit Import');

    expect(screen.getByRole('button', { name: 'Reject Import' })).toBeInTheDocument();
  });
});
