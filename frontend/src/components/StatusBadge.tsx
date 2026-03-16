import styles from './StatusBadge.module.css';

interface Props {
  status: string;
  size?: 'sm' | 'md';
}

const STATUS_CLASS: Record<string, string> = {
  ACTIVE:    'active',
  CLOSED:    'closed',
  TERMINATED:'closed',
  DRAFT:     'draft',
  COMMITTED: 'committed',
  STAGED:    'staged',
  REJECTED:  'rejected',
  OVER_BUDGET:   'over',
  UNDER_BUDGET:  'ok',
  ON_TRACK:      'ok',
  FULLY_RECONCILED:    'committed',
  PARTIALLY_MATCHED:   'staged',
  UNRECONCILED:        'draft',
  UNMATCHED:           'draft',
  WARNING: 'staged',
  CRITICAL: 'rejected',
  OPEN:    'draft',
  CANCELLED: 'closed',
};

export default function StatusBadge({ status, size = 'md' }: Props) {
  const cls = STATUS_CLASS[status] ?? 'draft';
  return (
    <span className={`${styles.badge} ${styles[cls]} ${size === 'sm' ? styles.sm : ''}`}>
      {status.replace(/_/g, ' ')}
    </span>
  );
}
