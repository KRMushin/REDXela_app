/** Paleta estable para identificar lotes en el mapa del almacén. */

const PALETA_LOTES = [
  '#4cc9f0',
  '#f72585',
  '#b5179e',
  '#4895ef',
  '#43aa8b',
  '#f9c74f',
  '#f8961e',
  '#90be6d',
  '#577590',
  '#e76f51',
];

/** Deriva un color determinista del identificador del lote. */
export function colorLote(loteId: string): string {
  if (!loteId) return '#8892b0';
  let hash = 0;
  for (let i = 0; i < loteId.length; i += 1) {
    hash = (hash * 31 + loteId.charCodeAt(i)) | 0;
  }
  return PALETA_LOTES[Math.abs(hash) % PALETA_LOTES.length];
}
