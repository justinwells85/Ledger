import { InputHTMLAttributes } from 'react';
import styles from './FormField.module.css';

interface FormFieldProps extends InputHTMLAttributes<HTMLInputElement | HTMLSelectElement> {
  label: string;
  required?: boolean;
  error?: string;
  as?: 'input' | 'select';
  children?: React.ReactNode;
}

export default function FormField({ label, required, error, as = 'input', children, ...props }: FormFieldProps) {
  return (
    <div className={styles.field}>
      <label className={styles.label}>
        {label}{required && <span className={styles.req}>*</span>}
      </label>
      {as === 'select' ? (
        <select className={`${styles.control} ${error ? styles.hasError : ''}`} {...(props as any)}>
          {children}
        </select>
      ) : (
        <input className={`${styles.control} ${error ? styles.hasError : ''}`} {...(props as any)} />
      )}
      {error && <div className={styles.error}>{error}</div>}
    </div>
  );
}
