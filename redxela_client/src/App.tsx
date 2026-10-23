import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import './styles/layout.css';
import './styles/tablero.css';
import Header from './components/layout/Header';
import Sidebar from './components/layout/Sidebar';
import DashboardPage from './pages/DashboardPage';
import { useSnapshot } from './hooks/useSnapshot';
import type { BackendStatus } from './types/health';

function App() {
  const { snapshot, conectado, error } = useSnapshot();
  const estadoConexion: BackendStatus = conectado ? 'connected' : error ? 'disconnected' : 'checking';

  return (
    <BrowserRouter>
      <div className="app-layout">
        <Header backendStatus={estadoConexion} />
        <div className="app-body">
          <Sidebar />
          <main className="app-content">
            <Routes>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route
                path="/dashboard"
                element={<DashboardPage snapshot={snapshot} conectado={conectado} error={error} />}
              />
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </main>
        </div>
      </div>
    </BrowserRouter>
  );
}

export default App;
