import { useState, useRef, DragEvent, ChangeEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { api } from '../api/client';
import type { SapImport } from '../api/types';
import ApiError from '../components/ApiError';
import styles from './SapImport.module.css';

function statusClass(status: string) {
  if (status === 'COMMITTED') return styles.committed;
  if (status === 'REJECTED') return styles.rejected;
  return styles.staged;
}

export default function SapImport() {
  const navigate = useNavigate();
  const { data: imports, loading, error: importsError } = useApi<SapImport[]>('/imports');
  if (importsError) return <ApiError message={importsError} />;
  const [dragging, setDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState('');
  const fileRef = useRef<HTMLInputElement>(null);

  async function handleFile(file: File) {
    setUploading(true);
    setUploadError('');
    try {
      const fd = new FormData();
      fd.append('file', file);
      const result = await api.upload<SapImport>('/imports/upload', fd);
      navigate(`/import/${result.importId}`);
    } catch (e) {
      setUploadError(String(e));
    } finally {
      setUploading(false);
    }
  }

  function onDrop(e: DragEvent) {
    e.preventDefault();
    setDragging(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  }

  function onFileChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) handleFile(file);
  }

  return (
    <div className={styles.page}>
      <h2>SAP Import</h2>

      {/* Drop zone */}
      <div
        className={`${styles.dropZone} ${dragging ? styles.dragOver : ''}`}
        onDragOver={e => { e.preventDefault(); setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        onClick={() => fileRef.current?.click()}
      >
        <input ref={fileRef} type="file" accept=".csv,.xlsx,.xls" style={{ display: 'none' }} onChange={onFileChange} />
        {uploading ? (
          <p>Uploading…</p>
        ) : (
          <>
            <p className={styles.dropMain}>Drag and drop SAP export file here</p>
            <p className={styles.dropSub}>or click to browse</p>
            <p className={styles.dropHint}>Accepted: .csv, .xlsx, .xls</p>
          </>
        )}
      </div>

      {uploadError && <div className={styles.error}>{uploadError}</div>}

      {/* Import history */}
      <section>
        <h3 className={styles.sectionTitle}>IMPORT HISTORY</h3>
        {loading && <p>Loading…</p>}
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Date</th>
              <th>File</th>
              <th className={styles.num}>New</th>
              <th className={styles.num}>Dup</th>
              <th className={styles.num}>Err</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {(imports ?? []).map(imp => (
              <tr
                key={imp.importId}
                className={styles.clickRow}
                onClick={() => navigate(`/import/${imp.importId}`)}
              >
                <td>{new Date(imp.importedAt).toLocaleDateString()}</td>
                <td>{imp.filename}</td>
                <td className={styles.num}>{imp.newLines}</td>
                <td className={styles.num}>{imp.duplicateLines}</td>
                <td className={styles.num}>{imp.errorLines}</td>
                <td><span className={`${styles.badge} ${statusClass(imp.status)}`}>{imp.status}</span></td>
              </tr>
            ))}
            {(imports ?? []).length === 0 && !loading && (
              <tr><td colSpan={6} className={styles.empty}>No import history</td></tr>
            )}
          </tbody>
        </table>
      </section>
    </div>
  );
}
