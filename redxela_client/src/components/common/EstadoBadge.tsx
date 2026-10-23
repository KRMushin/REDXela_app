import type { EstadoPedido } from '../../types/snapshot';
import { claseEstado, ETIQUETAS_ESTADO } from '../../utils/formato';

interface EstadoBadgeProps {
  estado: EstadoPedido;
}

/** Insignia del estado del ciclo de vida del pedido. */
function EstadoBadge({ estado }: EstadoBadgeProps) {
  return <span className={`insignia ${claseEstado(estado)}`}>{ETIQUETAS_ESTADO[estado]}</span>;
}

export default EstadoBadge;
