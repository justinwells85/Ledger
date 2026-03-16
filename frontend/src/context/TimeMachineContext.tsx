import { createContext, useContext, useState, ReactNode } from 'react';

interface TimeMachineContextValue {
  asOfDate: string | null;
  setAsOfDate: (date: string | null) => void;
}

const TimeMachineContext = createContext<TimeMachineContextValue>({
  asOfDate: null,
  setAsOfDate: () => {},
});

export function TimeMachineProvider({ children }: { children: ReactNode }) {
  const [asOfDate, setAsOfDate] = useState<string | null>(null);
  return (
    <TimeMachineContext.Provider value={{ asOfDate, setAsOfDate }}>
      {children}
    </TimeMachineContext.Provider>
  );
}

export function useTimeMachine() {
  return useContext(TimeMachineContext);
}
