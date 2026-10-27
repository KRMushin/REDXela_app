import type { ResultadoPolitica } from '../../types/api';
import type { NivelServicio } from '../../types/snapshot';
import NivelBadge from '../common/NivelBadge';
import { ETIQUETAS_POLITICA, formatearMinutos } from '../../utils/formato';

interface TablaNivelesComparadorProps {
  resultados: ResultadoPolitica[];
}

const NIVELES: NivelServicio[] = ['EXPRES', 'PRIORITARIO', 'ESTANDAR', 'PROGRAMADO', 'ECONOMICO'];

/** Espera promedio de cada nivel de servicio para las tres políticas. */
function TablaNivelesComparador({ resultados }: TablaNivelesComparadorProps) {
  return (
    <div className="tabla-scroll">
      <table className="tabla tabla--niveles">
        <thead>
          <tr>
            <th>Nivel de servicio</th>
            {resultados.map((resultado) => (
              <th key={resultado.politica}>{ETIQUETAS_POLITICA[resultado.politica]}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {NIVELES.map((nivel) => {
            const valores = resultados
              .map((resultado) => resultado.porNivel.find((metrica) => metrica.nivel === nivel))
              .map((metrica) => metrica?.tiempoEsperaPromedioMinutos)
              .filter((valor): valor is number => typeof valor === 'number');
            const mejor = valores.length > 0 ? Math.min(...valores) : 0;
            const hayDiferencia = new Set(valores.map((valor) => valor.toFixed(3))).size > 1;

            return (
              <tr key={nivel}>
                <td><NivelBadge nivel={nivel} /></td>
                {resultados.map((resultado) => {
                  const metrica = resultado.porNivel.find((item) => item.nivel === nivel);
                  const resaltar = metrica && hayDiferencia
                    && Math.abs(metrica.tiempoEsperaPromedioMinutos - mejor) < 0.05;
                  return (
                    <td className={resaltar ? 'celda-mejor' : ''} key={resultado.politica}>
                      {metrica ? formatearMinutos(metrica.tiempoEsperaPromedioMinutos) : '—'}
                    </td>
                  );
                })}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

export default TablaNivelesComparador;
