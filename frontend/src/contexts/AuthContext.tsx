import { createContext, useContext, useState, ReactNode } from 'react';

function decodeToken(token: string): { role?: string; displayName?: string } {
  try {
    const payload = token.split('.')[1];
    return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
  } catch {
    return {};
  }
}

interface AuthState {
  token: string | null;
  role: string | null;
  displayName: string | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('ledger_token'));
  const [role, setRole] = useState<string | null>(() => {
    const t = localStorage.getItem('ledger_token');
    return t ? (decodeToken(t).role ?? null) : null;
  });
  const [displayName, setDisplayName] = useState<string | null>(() => {
    const t = localStorage.getItem('ledger_token');
    return t ? (decodeToken(t).displayName ?? null) : null;
  });

  async function login(username: string, password: string) {
    const res = await fetch('/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });
    if (!res.ok) throw new Error('Invalid credentials');
    const data = await res.json();
    localStorage.setItem('ledger_token', data.token);
    setToken(data.token);
    const decoded = decodeToken(data.token);
    setRole(decoded.role ?? data.role ?? null);
    setDisplayName(decoded.displayName ?? data.displayName ?? null);
  }

  function logout() {
    localStorage.removeItem('ledger_token');
    setToken(null);
    setRole(null);
    setDisplayName(null);
  }

  return (
    <AuthContext.Provider value={{ token, role, displayName, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
