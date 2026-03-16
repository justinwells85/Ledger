import { useState, useEffect } from 'react';
import { api } from '../api/client';
import { useTimeMachine } from '../context/TimeMachineContext';

/**
 * Simple data fetching hook with loading/error state.
 * Automatically appends ?asOfDate= when the Time Machine is active.
 * The optional `deps` array can be used to trigger re-fetches (e.g. after mutations).
 */
export function useApi<T>(path: string | null, deps: unknown[] = []) {
  const { asOfDate } = useTimeMachine();
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!path) return;
    const url = asOfDate
      ? `${path}${path.includes('?') ? '&' : '?'}asOfDate=${asOfDate}`
      : path;
    let cancelled = false;
    setLoading(true);
    setError(null);
    api.get<T>(url)
      .then(d => { if (!cancelled) setData(d); })
      .catch(e => { if (!cancelled) setError(String(e)); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [path, asOfDate, ...deps]);

  return { data, loading, error };
}
