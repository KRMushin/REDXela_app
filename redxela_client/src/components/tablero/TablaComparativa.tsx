import type { ResultadoPolitica } from '../../types/api';
import { ETIQUETAS_POLITICA, formatearMinutos, formatearNumero } from '../../utils/formato';

interface TablaComparativaProps {
  resultados: ResultadoPolitica[];
}

const minimo = (valores: number[]) => Math.min(...valores);
const maximo = (valores: number[]) => Math.max(...valores);

const hayDiferencia = (valores: number[]) =>
  new Set(valores.map((valor) => valor.toFixed(3))).size > 1;

const esMejor = (valor: number, mejor: number, valores: number[]) =>
  hayDiferencia(valores) && Math.abs(valor - mejor) < 0.05;

/** Tabla global; resalta el mejor valor de cada columna cuando no hay empate total. */
function TablaComparativa({ resultados }: TablaComparativaProps) {
  if (resultados.length === 0) {
    return <p className="estado-vacio">La comparación no devolvió resultados.</p>;
  }

  const esperas = resultados.map((resultado) => resultado.tiempoEsperaPromedioMinutos);
  const maximas = resultados.map((resultado) => resultado.esperaMaximaMinutos);
  const slas = resultados.map((resultado) => resultado.cumplimientoSlaPorcentaje);
  const throughputs = resultados.map((resultado) => resultado.tasaThroughputPorMinuto);

  return (
    <div className="tabla-scroll">
      <table className="tabla tabla-comparativa">
        <thead>
          <tr>
            <th>Política</th>
            <th>Espera promedio</th>
            <th>Espera máxima</th>
            <th>SLA cumplido</th>
            <th>Throughput</th>
          </tr>
        </thead>
        <tbody>
          {resultados.map((resultado) => (
            <tr key={resultado.politica}>
              <td>
                <strong>{ETIQUETAS_POLITICA[resultado.politica] ?? resultado.nombreLegible}</strong>
                <span className="texto-muted bloque">{resultado.pedidosCompletados} pedidos completados</span>
              </td>
              <td className={esMejor(resultado.tiempoEsperaPromedioMinutos, minimo(esperas), esperas) ? 'celda-mejor' : ''}>
                {formatearMinutos(resultado.tiempoEsperaPromedioMinutos)}
              </td>
              <td className={esMejor(resultado.esperaMaximaMinutos, minimo(maximas), maximas) ? 'celda-mejor' : ''}>
                {formatearMinutos(resultado.esperaMaximaMinutos)}
              </td>
              <td className={esMejor(resultado.cumplimientoSlaPorcentaje, maximo(slas), slas) ? 'celda-mejor' : ''}>
                {resultado.pedidosDentroSla}/{resultado.pedidosCompletados}
                <span className="texto-muted bloque">{formatearNumero(resultado.cumplimientoSlaPorcentaje)}%</span>
              </td>
              <td className={esMejor(resultado.tasaThroughputPorMinuto, maximo(throughputs), throughputs) ? 'celda-mejor' : ''}>
                {formatearNumero(resultado.tasaThroughputPorMinuto)}/min
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default TablaComparativa;
