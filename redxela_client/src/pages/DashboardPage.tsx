import { useSearchParams } from 'react-router-dom';
import type { SnapshotEstado } from '../types/snapshot';
import { useDatosReferencia } from '../hooks/useDatosReferencia';
import RecursosPanel from '../components/tablero/RecursosPanel';
import PedidosPanel from '../components/tablero/PedidosPanel';
import ListaEsperaPanel from '../components/tablero/ListaEsperaPanel';
import EstadisticasPanel from '../components/tablero/EstadisticasPanel';
import FormulariosPanel from '../components/tablero/FormulariosPanel';
import AlmacenPanel from '../components/tablero/AlmacenPanel';
import ConflictoPanel from '../components/tablero/ConflictoPanel';
import ControlesPanel from '../components/tablero/ControlesPanel';
import { ETIQUETAS_MODO, ETIQUETAS_POLITICA, formatearHora, formatearMinutos } from '../utils/formato';

interface DashboardPageProps {
  snapshot: SnapshotEstado | null;
  conectado: boolean;
  error: string | null;
}

const TABS = [
  { id: 'recursos', etiqueta: 'Recursos', icono: '⚙️' },
  { id: 'pedidos', etiqueta: 'Pedidos', icono: '📋' },
  { id: 'espera', etiqueta: 'Lista de espera', icono: '⏳' },
  { id: 'estadisticas', etiqueta: 'Estadísticas', icono: '📊' },
  { id: 'almacen', etiqueta: 'Almacén', icono: '📦' },
  { id: 'formularios', etiqueta: 'Formularios', icono: '📝' },
  { id: 'conflicto', etiqueta: 'Conflicto', icono: '🔒' },
  { id: 'controles', etiqueta: 'Controles', icono: '🎛️' },
] as const;

type TabId = (typeof TABS)[number]['id'];

const esTabValida = (valor: string): valor is TabId =>
  TABS.some((tab) => tab.id === valor);

/** Tablero único: todos los paneles comparten el snapshot del stream. */
function DashboardPage({ snapshot, conectado, error }: DashboardPageProps) {
  const [parametros, setParametros] = useSearchParams();
  const tabSolicitada = parametros.get('tab') ?? 'recursos';
  const tabActiva: TabId = esTabValida(tabSolicitada) ? tabSolicitada : 'recursos';
  const referencia = useDatosReferencia();

  const cambiarTab = (tab: TabId) => {
    setParametros({ tab }, { replace: true });
  };

  return (
    <div className="page tablero">
      <div className="tablero__cabecera">
        <div>
          <h2 className="page-title">Tablero en vivo</h2>
          <p className="page-subtitle">Centro de distribución REDXela · telemetría del sistema</p>
        </div>
        <div className="tablero__resumen">
          <span className={`punto-conexion ${conectado ? 'punto-conexion--ok' : 'punto-conexion--off'}`} />
          <span>{conectado ? 'En vivo' : error ? 'Desconectado' : 'Conectando…'}</span>
          {snapshot && <span className="texto-muted">· {formatearHora(snapshot.timestamp)}</span>}
        </div>
      </div>

      {error && (
        <div className="alerta-conexion" role="status">
          <strong>Conexión en tiempo real:</strong> {error}
        </div>
      )}

      {snapshot && (
        <div className="tablero__meta">
          <span className="meta-chip">Tiempo simulado: <strong>{formatearMinutos(snapshot.tiempoSimuladoMinutos)}</strong></span>
          <span className="meta-chip">Generador: <strong>{snapshot.tasaGeneradorPorMinuto}/min</strong></span>
          <span className="meta-chip">Política: <strong>{ETIQUETAS_POLITICA[snapshot.politicaPlanificacion]}</strong></span>
          <span className={`meta-chip ${snapshot.modoPrevencion === 'SIN_PREVENCION' ? 'meta-chip--alerta' : ''}`}>
            {ETIQUETAS_MODO[snapshot.modoPrevencion]}
          </span>
        </div>
      )}

      <div className="tabs" role="tablist" aria-label="Paneles del tablero">
        {TABS.map((tab) => (
          <button
            className={`tab ${tabActiva === tab.id ? 'tab--activa' : ''}`}
            type="button"
            role="tab"
            aria-selected={tabActiva === tab.id}
            key={tab.id}
            onClick={() => cambiarTab(tab.id)}
          >
            <span aria-hidden="true">{tab.icono}</span> {tab.etiqueta}
          </button>
        ))}
      </div>

      {!snapshot ? (
        <section className="panel">
          <p className="estado-vacio">
            Esperando el primer snapshot del backend. Verifique que el servicio esté disponible y que
            VITE_API_URL apunte a la API correcta.
          </p>
        </section>
      ) : (
        <div className="tab-panel" role="tabpanel">
          {tabActiva === 'recursos' && <RecursosPanel recursos={snapshot.recursos} />}
          {tabActiva === 'pedidos' && <PedidosPanel pedidos={snapshot.pedidosEnProceso} />}
          {tabActiva === 'espera' && (
            <ListaEsperaPanel elementos={snapshot.listaEspera} politica={snapshot.politicaPlanificacion} />
          )}
          {tabActiva === 'estadisticas' && (
            <EstadisticasPanel estadisticas={snapshot.estadisticas} recursos={snapshot.recursos} />
          )}
          {tabActiva === 'almacen' && <AlmacenPanel almacen={snapshot.almacen} />}
          {tabActiva === 'formularios' && (
            <FormulariosPanel
              clientes={referencia.clientes}
              productos={referencia.productos}
              cargando={referencia.cargando}
              error={referencia.error}
              onClienteCreado={referencia.agregarCliente}
            />
          )}
          {tabActiva === 'conflicto' && <ConflictoPanel snapshot={snapshot} />}
          {tabActiva === 'controles' && <ControlesPanel snapshot={snapshot} />}
        </div>
      )}
    </div>
  );
}

export default DashboardPage;
