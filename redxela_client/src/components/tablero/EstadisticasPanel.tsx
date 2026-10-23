import type { Estadisticas, RecursoEstado } from '../../types/snapshot';
import EncabezadoPanel from '../common/EncabezadoPanel';
import BarraProgreso from '../common/BarraProgreso';
import { formatearMinutos, formatearNumero, porcentaje } from '../../utils/formato';

interface EstadisticasPanelProps {
  estadisticas: Estadisticas;
  recursos: RecursoEstado[];
}

/** Panel 4: métricas globales y ocupación derivada de cada recurso. */
function EstadisticasPanel({ estadisticas, recursos }: EstadisticasPanelProps) {
  const tarjetas = [
    { etiqueta: 'Pedidos completados', valor: String(estadisticas.pedidosCompletados) },
    { etiqueta: 'Espera promedio', valor: formatearMinutos(estadisticas.tiempoEsperaPromedioMinutos) },
    { etiqueta: 'Procesamiento promedio', valor: formatearMinutos(estadisticas.tiempoProcesamientoPromedioMinutos) },
    { etiqueta: 'Cumplimiento SLA', valor: `${formatearNumero(estadisticas.cumplimientoSlaPorcentaje)}%` },
    { etiqueta: 'Throughput', valor: `${formatearNumero(estadisticas.tasaThroughputPorMinuto)}/min` },
  ];

  return (
    <section className="panel">
      <EncabezadoPanel titulo="Estadísticas" descripcion="Indicadores del rendimiento y ocupación del centro de distribución." />
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
    </section>
  );
}

export default EstadisticasPanel;
