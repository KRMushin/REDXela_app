import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import './styles/layout.css';
import Header from './components/layout/Header';
import Sidebar from './components/layout/Sidebar';
import DashboardPage from './pages/DashboardPage';
import PedidosPage from './pages/PedidosPage';
import InventarioPage from './pages/InventarioPage';
import RecursosPage from './pages/RecursosPage';
import PlanificacionPage from './pages/PlanificacionPage';
import DeadlocksPage from './pages/DeadlocksPage';
import EstadisticasPage from './pages/EstadisticasPage';
import { useBackendHealth } from './hooks/useBackendHealth';

function App() {
  const { status } = useBackendHealth();

  return (
    <BrowserRouter>
      <div className="app-layout">
        <Header backendStatus={status} />
        <div className="app-body">
          <Sidebar />
          <main className="app-content">
            <Routes>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<DashboardPage backendStatus={status} />} />
              <Route path="/pedidos" element={<PedidosPage />} />
              <Route path="/inventario" element={<InventarioPage />} />
              <Route path="/recursos" element={<RecursosPage />} />
              <Route path="/planificacion" element={<PlanificacionPage />} />
              <Route path="/deadlocks" element={<DeadlocksPage />} />
              <Route path="/estadisticas" element={<EstadisticasPage />} />
            </Routes>
          </main>
        </div>
      </div>
    </BrowserRouter>
  );
}

export default App;
