import type { ElementoEspera, TipoPolitica } from '../../types/snapshot';
import EncabezadoPanel from '../common/EncabezadoPanel';
import NivelBadge from '../common/NivelBadge';
import EstadoBadge from '../common/EstadoBadge';
import { ETIQUETAS_POLITICA, formatearMinutos, formatearNumero } from '../../utils/formato';

interface ListaEsperaPanelProps {
  elementos: ElementoEspera[];
  politica: TipoPolitica;
}

/** Panel 3: ready queue ordenada y explicación del lugar de cada proceso. */
function ListaEsperaPanel({ elementos, politica }: ListaEsperaPanelProps) {
  return (
    <section className="panel">
      <EncabezadoPanel
        titulo="Lista de espera"
        descripcion="Pedidos listos por orden de atención y pedidos pendientes de stock o recursos."
        extra={<span className="contador">{ETIQUETAS_POLITICA[politica]}</span>}
      />
      {elementos.length === 0 ? (
        <p className="estado-vacio">La lista de espera está vacía.</p>
      ) : (
        <div className="tabla-scroll">
          <table className="tabla">
            <thead>
              <tr>
                <th>#</th>
                <th>Pedido</th>
                <th>Cliente / nivel</th>
                <th>Estado</th>
                <th>Artículos</th>
                <th>Espera</th>
                <th>Prioridad</th>
                <th>Motivo de la posición</th>
              </tr>
            </thead>
            <tbody>
              {elementos.map((elemento, indice) => (
                <tr key={elemento.id}>
                  <td><span className="posicion">{indice + 1}</span></td>
                  <td><strong>{elemento.id}</strong></td>
                  <td>{elemento.clienteNombre}<NivelBadge nivel={elemento.nivelServicio} /></td>
                  <td><EstadoBadge estado={elemento.estado} /></td>
                  <td>{elemento.totalArticulos}</td>
                  <td>{formatearMinutos(elemento.tiempoEsperaMinutos)}</td>
                  <td>{formatearNumero(elemento.prioridadCalculada, 2)}</td>
                  <td className="celda-motivo">{elemento.motivoPosicion}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export default ListaEsperaPanel;
