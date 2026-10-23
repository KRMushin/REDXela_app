import { Link, useLocation } from 'react-router-dom';

const navItems = [
  { tab: 'recursos', label: 'Recursos', icon: '⚙️' },
  { tab: 'pedidos', label: 'Pedidos', icon: '📋' },
  { tab: 'espera', label: 'Lista de espera', icon: '⏳' },
  { tab: 'estadisticas', label: 'Estadísticas', icon: '📊' },
  { tab: 'almacen', label: 'Almacén', icon: '📦' },
  { tab: 'formularios', label: 'Formularios', icon: '📝' },
  { tab: 'conflicto', label: 'Conflicto', icon: '🔒' },
  { tab: 'controles', label: 'Controles', icon: '🎛️' },
] as const;

function Sidebar() {
  const ubicacion = useLocation();
  const tabActual = new URLSearchParams(ubicacion.search).get('tab') ?? 'recursos';

  return (
    <nav className="app-sidebar">
      <ul className="sidebar-nav">
        {navItems.map(({ tab, label, icon }) => (
          <li key={tab}>
            <Link
              to={`/dashboard?tab=${tab}`}
              className={`sidebar-link ${tabActual === tab ? 'sidebar-link--active' : ''}`}
            >
              <span className="sidebar-icon">{icon}</span>
              <span>{label}</span>
            </Link>
          </li>
        ))}
      </ul>
    </nav>
  );
}

export default Sidebar;
