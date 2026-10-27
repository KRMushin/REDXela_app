import type { BackendStatus } from '../../types/health';

interface StatusBadgeProps {
  status: BackendStatus;
}

const statusConfig: Record<BackendStatus, { label: string; className: string }> = {
  checking: {
    label: 'Verificando conexión...',
    className: 'badge badge--checking',
  },
  connected: {
    label: 'Backend conectado',
    className: 'badge badge--connected',
  },
  disconnected: {
    label: 'Backend desconectado',
    className: 'badge badge--disconnected',
  },
};

function StatusBadge({ status }: StatusBadgeProps) {
  const config = statusConfig[status];
  return (
    <span className={config.className}>
      {config.label}
    </span>
  );
}

export default StatusBadge;
