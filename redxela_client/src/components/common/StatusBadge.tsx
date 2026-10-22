import type { BackendStatus } from '../../types/health';

interface StatusBadgeProps {
  status: BackendStatus;
}

const statusConfig: Record<BackendStatus, { label: string; className: string; dot: string }> = {
  checking: {
    label: 'Verificando conexión...',
    className: 'badge badge--checking',
    dot: '🟡',
  },
  connected: {
    label: 'Backend conectado',
    className: 'badge badge--connected',
    dot: '🟢',
  },
  disconnected: {
    label: 'Backend desconectado',
    className: 'badge badge--disconnected',
    dot: '🔴',
  },
};

function StatusBadge({ status }: StatusBadgeProps) {
  const config = statusConfig[status];
  return (
    <span className={config.className}>
      {config.dot} {config.label}
    </span>
  );
}

export default StatusBadge;
