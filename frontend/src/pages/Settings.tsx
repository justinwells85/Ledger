import { useState, useEffect } from 'react';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import { SystemConfig } from '../api/types';
import ApiError from '../components/ApiError';
import styles from './Settings.module.css';

interface EditState {
  value: string;
  reason: string;
  error: string;
  saving: boolean;
}

export default function Settings() {
  const { data: configs, loading, error } = useApi<SystemConfig[]>('/config');
  if (error) return <ApiError message={error} />;
  const [edits, setEdits] = useState<Record<string, EditState>>({});

  useEffect(() => {
    if (configs) {
      const initial: Record<string, EditState> = {};
      for (const c of configs) {
        initial[c.configKey] = { value: c.configValue, reason: '', error: '', saving: false };
      }
      setEdits(initial);
    }
  }, [configs]);

  function setField(key: string, field: keyof EditState, val: string) {
    setEdits(prev => ({ ...prev, [key]: { ...prev[key], [field]: val } }));
  }

  async function save(key: string) {
    const edit = edits[key];
    if (!edit.reason.trim()) {
      setField(key, 'error', 'Reason is required');
      return;
    }
    setEdits(prev => ({ ...prev, [key]: { ...prev[key], saving: true, error: '' } }));
    try {
      await api.put(`/config/${key}`, { value: edit.value, reason: edit.reason });
      setEdits(prev => ({ ...prev, [key]: { ...prev[key], saving: false, reason: '' } }));
    } catch (e) {
      setEdits(prev => ({ ...prev, [key]: { ...prev[key], saving: false, error: String(e) } }));
    }
  }

  if (loading) return <p>Loading…</p>;

  // Group configs dynamically by displayGroup, sorted by displayOrder
  const sortedConfigs = [...(configs ?? [])].sort((a, b) => {
    const groupCmp = a.displayGroup.localeCompare(b.displayGroup);
    return groupCmp !== 0 ? groupCmp : a.displayOrder - b.displayOrder;
  });

  const groups = sortedConfigs.reduce((acc, c) => {
    const g = c.displayGroup || 'GENERAL';
    if (!acc[g]) acc[g] = [];
    acc[g].push(c);
    return acc;
  }, {} as Record<string, SystemConfig[]>);

  function renderRow(c: SystemConfig) {
    const edit = edits[c.configKey];
    if (!edit) return null;
    return (
      <tr key={c.configKey}>
        <td className={styles.settingLabel}>{c.displayName || c.description || c.configKey}</td>
        <td>
          <input
            className={styles.valueInput}
            value={edit.value}
            onChange={e => setField(c.configKey, 'value', e.target.value)}
          />
        </td>
        <td>
          <input
            className={styles.reasonInput}
            placeholder="Reason for change…"
            value={edit.reason}
            onChange={e => setField(c.configKey, 'reason', e.target.value)}
          />
          {edit.error && <div className={styles.error}>{edit.error}</div>}
        </td>
        <td>
          <button className={styles.saveBtn} disabled={edit.saving} onClick={() => save(c.configKey)}>
            {edit.saving ? 'Saving…' : 'Save'}
          </button>
        </td>
      </tr>
    );
  }

  return (
    <div className={styles.page}>
      <h2>Settings</h2>
      {Object.entries(groups).map(([groupName, groupConfigs]) => (
        <section key={groupName} className={styles.section}>
          <h3 className={styles.sectionTitle}>{groupName}</h3>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Setting</th>
                <th>Value</th>
                <th>Reason</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {groupConfigs.map(renderRow)}
            </tbody>
          </table>
        </section>
      ))}
    </div>
  );
}
