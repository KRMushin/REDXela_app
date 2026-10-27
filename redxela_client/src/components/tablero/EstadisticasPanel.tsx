import type { Estadisticas, RecursoEstado } from '../../types/snapshot';
import EncabezadoPanel from '../common/EncabezadoPanel';
import BarraProgreso from '../common/BarraProgreso';
import NivelBadge from '../common/NivelBadge';
import { useEstadisticasDetalladas } from '../../hooks/useEstadisticasDetalladas';
import { formatearMinutos, formatearNumero, porcentaje } from '../../utils/formato';

interface EstadisticasPanelProps {
  estadisticas: Estadisticas;
  recursos: RecursoEstado[];
}

/** Panel 4: métricas globales, ocupación y detalle por nivel de servicio. */
function EstadisticasPanel({ estadisticas, recursos }: EstadisticasPanelProps) {
  const { detalle, cargando, error } = useEstadisticasDetalladas();

  const tarjetas = [
    { etiqueta: 'Pedidos completados', valor: String(estadisticas.pedidosCompletados) },
    { etiqueta: 'En espera de stock', valor: String(estadisticas.pedidosEnEsperaStock) },
    { etiqueta: 'En espera de recursos', valor: String(estadisticas.pedidosEnEsperaRecursos) },
    { etiqueta: 'Espera promedio', valor: formatearMinutos(estadisticas.tiempoEsperaPromedioMinutos) },
    { etiqueta: 'Procesamiento promedio', valor: formatearMinutos(estadisticas.tiempoProcesamientoPromedioMinutos) },
    { etiqueta: 'Cumplimiento SLA', valor: `${formatearNumero(estadisticas.cumplimientoSlaPorcentaje)}%` },
    { etiqueta: 'Throughput', valor: `${formatearNumero(estadisticas.tasaThroughputPorMinuto)}/min` },
  ];

  const extra = cargando
    ? <span className="contador">Actualizando detalle…</span>
    : detalle
      ? <span className="contador">Detalle por nivel</span>
      : null;

  return (
    <section className="panel">
      <EncabezadoPanel
        titulo="Estadísticas"
        descripcion="Indicadores del rendimiento y ocupación del centro de distribución."
        extra={extra}
      />
      <div className="metricas-grid">
        {tarjetas.map((tarjeta) => (
          <article className="metrica" key={tarjeta.etiqueta}>
            <span className="metrica__etiqueta">{tarjeta.etiqueta}</span>
            <strong className="metrica__valor">{tarjeta.valor}</strong>
          </article>
        ))}
      </div>
      <h4 className="subtitulo">Ocupación por recurso</h4>
      <div className="ocupacion-grid">
        {recursos.map((recurso) => {
          const uso = porcentaje(recurso.enUso, recurso.total);
          return (
            <div className="ocupacion" key={recurso.tipo}>
              <div className="ocupacion__cabecera">
                <span>{recurso.nombre}</span>
                <strong>{formatearNumero(uso, 0)}%</strong>
              </div>
              <BarraProgreso valor={uso} tono={uso >= 100 ? 'peligro' : 'normal'} />
            </div>
          );
        })}
      </div>

      <h4 className="subtitulo">Detalle por nivel de servicio</h4>
      {error && <p className="feedback__mensaje feedback__mensaje--error" role="alert">Estadísticas detalladas: {error}</p>}
      {detalle ? (
        <div className="tabla-scroll">
          <table className="tabla tabla--niveles">
            <thead>
              <tr>
                <th>Nivel</th>
                <th>Completados</th>
                <th>Espera prom.</th>
                <th>Procesamiento prom.</th>
                <th>SLA cumplido</th>
              </tr>
            </thead>
            <tbody>
              {detalle.porNivel.map((metrica) => (
                <tr key={metrica.nivel}>
                  <td><NivelBadge nivel={metrica.nivel} /></td>
                  <td>{metrica.pedidosCompletados}</td>
                  <td>{formatearMinutos(metrica.tiempoEsperaPromedioMinutos)}</td>
                  <td>{formatearMinutos(metrica.tiempoProcesamientoPromedioMinutos)}</td>
                  <td>
                    {metrica.pedidosDentroSla}/{metrica.pedidosCompletados}
                    <span className="texto-muted bloque">{formatearNumero(metrica.cumplimientoSlaPorcentaje)}%</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : !cargando && (
        <p className="estado-vacio">El detalle por nivel no está disponible.</p>
      )}
    </section>
  );
}

export default EstadisticasPanel;
