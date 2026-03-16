import { screen, fireEvent } from '@testing-library/react';
import { renderWithProviders } from '../test/renderWithProviders';
import Dashboard from './Dashboard';

describe('Dashboard', () => {
  it('renders KPI cards after data loads', async () => {
    renderWithProviders(<Dashboard />);

    await screen.findByText('Globant ADM');

    expect(screen.getByText('TOTAL BUDGET')).toBeInTheDocument();
    expect(screen.getByText('TOTAL ACTUALS')).toBeInTheDocument();
  });

  it('shows over budget count in alerts', async () => {
    renderWithProviders(<Dashboard />);

    await screen.findByText(/1 contract over budget/);
  });

  it('shows + New Contract button', async () => {
    renderWithProviders(<Dashboard />);

    await screen.findByText('Globant ADM');

    expect(screen.getByText('+ New Contract')).toBeInTheDocument();
  });

  it('clicking + New Contract opens drawer with Name and Vendor fields', async () => {
    renderWithProviders(<Dashboard />);

    await screen.findByText('Globant ADM');

    fireEvent.click(screen.getByText('+ New Contract'));

    // Drawer title
    expect(screen.getByRole('heading', { name: 'New Contract' })).toBeInTheDocument();
    // FormField labels
    expect(screen.getByText('Name', { selector: 'label, label *' })).toBeInTheDocument();
    expect(screen.getByText('Vendor', { selector: 'label, label *' })).toBeInTheDocument();
  });

  it('submitting New Contract without Name shows an error', async () => {
    renderWithProviders(<Dashboard />);

    await screen.findByText('Globant ADM');

    fireEvent.click(screen.getByText('+ New Contract'));
    fireEvent.click(screen.getByText('Create Contract'));

    expect(screen.getByText('Name is required')).toBeInTheDocument();
  });
});
