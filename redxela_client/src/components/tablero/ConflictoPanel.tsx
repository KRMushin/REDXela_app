import type { SnapshotEstado } from '../../types/snapshot';
import { cambiarModoPrevencion, forzarConflicto, resolverConflicto } from '../../api/redxelaApi';
import { useAccion } from '../../hooks/useAccion';
import { ETIQUETAS_MODO, ETIQUETAS_RECURSO } from '../../utils/formato';
import FeedbackOperacion from '../common/FeedbackOperacion';

interface ConflictoPanelProps {
  snapshot: SnapshotEstado;
}

/** Panel 7: alerta, diagnóstico y resolución manual del interbloqueo. */
function ConflictoPanel({ snapshot }: ConflictoPanelProps) {
  const reporte = snapshot.interbloqueo;
  const modo = snapshot.modoPrevencion;
  const accionModo = useAccion(cambiarModoPrevencion, 'Modo de prevención actualizado.');
  const accionForzar = useAccion(forzarConflicto, 'Escenario de conflicto generado.');
  const accionResolver = useAccion(resolverConflicto, 'Interbloqueo resuelto; el ganador continúa.');

  const alternarModo = () => {
    const siguiente = modo === 'PREVENCION_ACTIVA' ? 'SIN_PREVENCION' : 'PREVENCION_ACTIVA';
    void accionModo.ejecutar(siguiente);
  };

  return (
    <section className={`panel ${reporte.hayInterbloqueo ? 'panel--alerta' : ''}`}>
      <header className="panel__encabezado">
        <div>
          <h3 className="panel__titulo">Control de interbloqueos</h3>
          <p className="panel__descripcion">Estado del grafo de espera y recuperación manual del sistema.</p>
        </div>
        <span className={`contador ${reporte.hayInterbloqueo ? 'contador--peligro' : 'contador--ok'}`}>
          {reporte.hayInterbloqueo ? 'Interbloqueo detectado' : 'Sin interbloqueo'}
        </span>
      </header>

      <div className="conflicto-controles">
        <div>
          <span className="mini-etiqueta">Modo actual</span>
          <strong className={modo === 'SIN_PREVENCION' ? 'texto-peligro' : 'texto-ok'}>
            {ETIQUETAS_MODO[modo]}
          </strong>
        </div>
        <button className="boton boton--secundario" type="button" onClick={alternarModo} disabled={accionModo.enviando}>
          {modo === 'PREVENCION_ACTIVA' ? 'Desactivar prevención' : 'Activar prevención'}
        </button>
        <button className="boton boton--peligro" type="button" onClick={() => void accionForzar.ejecutar()} disabled={accionForzar.enviando}>
          {accionForzar.enviando ? 'Provocando…' : 'Provocar conflicto'}
        </button>
      </div>

      {reporte.hayInterbloqueo ? (
        <div className="conflicto-alerta">
          <p className="conflicto-alerta__titulo">Espera circular detectada</p>
          <div className="conflicto-datos">
            <div>
              <span className="mini-etiqueta">Recursos en disputa</span>
              <div className="chips">
                {reporte.recursosEnDisputa.map((recurso) => (
                  <span className="chip chip--peligro" key={recurso}>{ETIQUETAS_RECURSO[recurso]}</span>
                ))}
              </div>
            </div>
            <div>
              <span className="mini-etiqueta">Pedidos involucrados</span>
              <div className="chips">
                {reporte.pedidosInvolucrados.map((pedido) => (
                  <span className="chip chip--espera" key={pedido}>{pedido}</span>
                ))}
              </div>
            </div>
          </div>
          <p className="conflicto-alerta__explicacion">{reporte.explicacion ?? 'El detector confirmó un ciclo de espera.'}</p>
          <div className="conflicto-ganadores">
            <span className="mini-etiqueta">Elegir el pedido que continúa</span>
            {reporte.pedidosInvolucrados.map((pedido) => (
              <button
                className="boton boton--primario boton--pequeno"
                type="button"
                key={pedido}
                disabled={accionResolver.enviando}
                onClick={() => void accionResolver.ejecutar(pedido)}
              >
                Continuar este · {pedido}
              </button>
            ))}
          </div>
        </div>
      ) : (
        <p className="estado-vacio estado-vacio--ok">
          No hay ciclos de espera. Con prevención activa, la regla de orden global evita la espera circular.
        </p>
      )}

      <FeedbackOperacion
        mensaje={accionModo.mensaje ?? accionForzar.mensaje ?? accionResolver.mensaje}
        error={accionModo.error ?? accionForzar.error ?? accionResolver.error}
      />
    </section>
  );
}

export default ConflictoPanel;
