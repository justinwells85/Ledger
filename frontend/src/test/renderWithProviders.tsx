import { ReactElement } from 'react';
import { render, RenderOptions } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { TimeMachineProvider } from '../context/TimeMachineContext';
import { AuthProvider } from '../contexts/AuthContext';

interface Options extends Omit<RenderOptions, 'wrapper'> {
  initialEntries?: string[];
}

export function renderWithProviders(ui: ReactElement, { initialEntries = ['/'], ...opts }: Options = {}) {
  return render(
    <AuthProvider>
      <TimeMachineProvider>
        <MemoryRouter initialEntries={initialEntries}>
          {ui}
        </MemoryRouter>
      </TimeMachineProvider>
    </AuthProvider>,
    opts
  );
}
