import { useApi } from '../hooks/useApi';

interface FiscalYear { fiscalYear: string; }

interface Props {
  value: string;
  onChange: (value: string) => void;
}

export default function FiscalYearSelect({ value, onChange }: Props) {
  const { data: years } = useApi<FiscalYear[]>('/fiscal-years');
  return (
    <select value={value} onChange={e => onChange(e.target.value)}>
      {years?.map(y => (
        <option key={y.fiscalYear} value={y.fiscalYear}>{y.fiscalYear}</option>
      ))}
    </select>
  );
}
