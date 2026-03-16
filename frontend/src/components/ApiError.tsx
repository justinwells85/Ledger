interface Props { message?: string; }
export default function ApiError({ message }: Props) {
  return (
    <div style={{ padding: '1rem', color: '#ef4444', background: '#1e1e2e', borderRadius: 4 }}>
      Error loading data{message ? `: ${message}` : ''}. Please refresh the page.
    </div>
  );
}
