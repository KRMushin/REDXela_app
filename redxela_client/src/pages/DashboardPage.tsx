import type { BackendStatus } from '../types/health';
import StatusBadge from '../components/common/StatusBadge';

interface DashboardPageProps {
  backendStatus: BackendStatus;
}

const modules = [
  { name: 'Pedidos', description: 'Recepción y procesamiento de pedidos de clientes.' },
  { name: 'Inventario', description: 'Control de existencias en el centro de distribución.'},
  { name: 'Recursos', description: 'Gestión de recursos compartidos y semáforos.' },
  { name: 'Planificación', description: 'Algoritmos de planificación de procesos.' },
  { name: 'Deadlocks', description: 'Detección y resolución de interbloqueos.' },
  { name: 'Estadísticas', description: 'Métricas y reportes del simulador.' },
];

function DashboardPage({ backendStatus }: DashboardPageProps) {
  return (
    <div className="page">
      <div className="page-header">
        <h2 className="page-title">Centro de Distribución REDXela</h2>
        <p className="page-subtitle">Sistema de simulación y administración de recursos</p>
      </div>

      <div className="dashboard-status">
        <span className="dashboard-status-label">Estado del sistema:</span>
        <StatusBadge status={backendStatus} />
      </div>

      <div className="module-grid">
        {modules.map(({ name, description}) => (
          <div key={name} className="module-card">
            <h3 className="module-card-title">{name}</h3>
            <p className="module-card-description">{description}</p>
            <span className="module-card-badge">Pendiente de implementación</span>
          </div>
        ))}
      </div>
    </div>
  );
}

export default DashboardPage;
