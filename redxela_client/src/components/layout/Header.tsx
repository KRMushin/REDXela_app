import type { BackendStatus } from '../../types/health';
import StatusBadge from '../common/StatusBadge';

interface HeaderProps {
  backendStatus: BackendStatus;
}

function Header({ backendStatus }: HeaderProps) {
  return (
    <header className="app-header">
      <div className="header-brand">
        <span className="header-logo">📦</span>
        <h1 className="header-title">REDXela</h1>
        <span className="header-subtitle">Centro de Distribución</span>
      </div>
      <div className="header-status">
        <StatusBadge status={backendStatus} />
      </div>
    </header>
  );
}

export default Header;
