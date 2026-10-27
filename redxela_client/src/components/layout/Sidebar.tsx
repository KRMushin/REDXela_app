import { Link, useLocation } from 'react-router-dom';

const navItems = [
  { tab: 'recursos', label: 'Recursos' },
  { tab: 'pedidos', label: 'Pedidos' },
  { tab: 'espera', label: 'Lista de espera' },
  { tab: 'estadisticas', label: 'Estadísticas' },
  { tab: 'comparador', label: 'Comparador' },
  { tab: 'almacen', label: 'Almacén' },
  { tab: 'formularios', label: 'Formularios' },
  { tab: 'conflicto', label: 'Conflicto' },
  { tab: 'controles', label: 'Controles' },
] as const;

function Sidebar() {
  const ubicacion = useLocation();
  const tabActual = new URLSearchParams(ubicacion.search).get('tab') ?? 'recursos';

  return (
    <nav className="app-sidebar">
      <ul className="sidebar-nav">
        {navItems.map(({ tab, label }) => (
          <li key={tab}>
            <Link
              to={`/dashboard?tab=${tab}`}
              className={`sidebar-link ${tabActual === tab ? 'sidebar-link--active' : ''}`}
            >
              <span>{label}</span>
            </Link>
          </li>
        ))}
      </ul>
    </nav>
  );
}

export default Sidebar;
