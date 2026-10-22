import { NavLink } from 'react-router-dom';

const navItems = [
  { path: '/dashboard', label: 'Dashboard', icon: '🏠' },
  { path: '/pedidos', label: 'Pedidos', icon: '📋' },
  { path: '/inventario', label: 'Inventario', icon: '📦' },
  { path: '/recursos', label: 'Recursos', icon: '⚙️' },
  { path: '/planificacion', label: 'Planificación', icon: '📅' },
  { path: '/deadlocks', label: 'Deadlocks', icon: '🔒' },
  { path: '/estadisticas', label: 'Estadísticas', icon: '📊' },
];

function Sidebar() {
  return (
    <nav className="app-sidebar">
      <ul className="sidebar-nav">
        {navItems.map(({ path, label, icon }) => (
          <li key={path}>
            <NavLink
              to={path}
              className={({ isActive }) =>
                isActive ? 'sidebar-link sidebar-link--active' : 'sidebar-link'
              }
            >
              <span className="sidebar-icon">{icon}</span>
              <span>{label}</span>
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
}

export default Sidebar;
