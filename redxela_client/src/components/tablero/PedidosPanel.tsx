import type { PedidoProceso } from '../../types/snapshot';
import EncabezadoPanel from '../common/EncabezadoPanel';
import NivelBadge from '../common/NivelBadge';
import EstadoBadge from '../common/EstadoBadge';
import BarraProgreso from '../common/BarraProgreso';
import { ETIQUETAS_RECURSO, formatearSegundos } from '../../utils/formato';

interface PedidosPanelProps {
  pedidos: PedidoProceso[];
}

/** Panel 2: procesos en ejecución o bloqueados, con avance y tiempo restante. */
function PedidosPanel({ pedidos }: PedidosPanelProps) {
  return (
    <section className="panel">
      <EncabezadoPanel
        titulo="Pedidos en proceso"
        descripcion="Estado del ciclo de vida, progreso y recursos retenidos por cada pedido."
        extra={<span className="contador">{pedidos.length} activos</span>}
      />
      {pedidos.length === 0 ? (
        <p className="estado-vacio">No hay pedidos en proceso en este momento.</p>
      ) : (
        <div className="tabla-scroll">
          <table className="tabla">
            <thead>
              <tr>
                <th>Pedido</th>
                <th>Cliente / nivel</th>
                <th>Estado</th>
                <th>Progreso</th>
                <th>Restante</th>
                <th>Recursos</th>
              </tr>
            </thead>
            <tbody>
              {pedidos.map((pedido) => (
                <tr key={pedido.id}>
                  <td><strong>{pedido.id}</strong><span className="texto-muted bloque">{pedido.totalArticulos} artículos</span></td>
                  <td>{pedido.clienteNombre}<NivelBadge nivel={pedido.nivelServicio} /></td>
                  <td><EstadoBadge estado={pedido.estado} /></td>
                  <td className="celda-progreso">
                    <BarraProgreso valor={pedido.progreso} tono={pedido.estado === 'BLOQUEADO' ? 'alerta' : 'normal'} />
                    <span>{pedido.progreso}%</span>
                  </td>
                  <td>{formatearSegundos(pedido.tiempoRestanteEstimadoSegundos)}</td>
                  <td>
                    <div className="chips">
                      {pedido.recursosRetenidos.map((recurso) => (
                        <span className="chip chip--uso" key={recurso}>{ETIQUETAS_RECURSO[recurso]}</span>
                      ))}
                      {pedido.recursoEsperado && (
                        <span className="chip chip--espera">
                          Espera: {ETIQUETAS_RECURSO[pedido.recursoEsperado]}
                        </span>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export default PedidosPanel;
