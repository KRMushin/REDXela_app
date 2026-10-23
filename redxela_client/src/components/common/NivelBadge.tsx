import type { NivelServicio } from '../../types/snapshot';
import { claseNivel, ETIQUETAS_NIVEL } from '../../utils/formato';

interface NivelBadgeProps {
  nivel: NivelServicio;
}

/** Insignia con el color consistente del nivel de servicio. */
function NivelBadge({ nivel }: NivelBadgeProps) {
  return <span className={`insignia ${claseNivel(nivel)}`}>{ETIQUETAS_NIVEL[nivel]}</span>;
}

export default NivelBadge;
