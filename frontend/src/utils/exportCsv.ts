/**
 * Converts rows and headers to a CSV string and triggers a browser download.
 */
export function exportCsv(filename: string, headers: (string | number)[], rows: (string | number)[][]): void {
  const csv = [headers, ...rows].map(r => r.join(',')).join('\n');
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
  a.download = filename;
  a.click();
}
