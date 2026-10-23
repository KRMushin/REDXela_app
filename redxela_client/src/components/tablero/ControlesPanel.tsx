import { useEffect, useState } from 'react';
import type { SnapshotEstado, TipoPolitica } from '../../types/snapshot';
import {
  ajustarTasa,
  cambiarPolitica,
  detenerGenerador,
  iniciarGenerador,
  reiniciarSimulacion,
} from '../../api/redxelaApi';
import { useAccion } from '../../hooks/useAccion';
import { ETIQUETAS_POLITICA } from '../../utils/formato';
import FeedbackOperacion from '../common/FeedbackOperacion';

interface ControlesPanelProps {
  snapshot: SnapshotEstado;
}

const POLITICAS = Object.keys(ETIQUETAS_POLITICA) as TipoPolitica[];

/** Panel 8: operación del generador, política de planificación y reinicio. */
function ControlesPanel({ snapshot }: ControlesPanelProps) {
  const [tasa, setTasa] = useState(snapshot.tasaGeneradorPorMinuto);
  const [politica, setPolitica] = useState<TipoPolitica>(snapshot.politicaPlanificacion);

  const accionIniciar = useAccion(iniciarGenerador, 'Generador iniciado.');
  const accionDetener = useAccion(detenerGenerador, 'Generador detenido.');
  const accionTasa = useAccion(ajustarTasa, 'Tasa de generación actualizada.');
  const accionPolitica = useAccion(cambiarPolitica, 'Política de planificación actualizada.');
  const accionReinicio = useAccion(reiniciarSimulacion, 'Simulación reiniciada.');

  useEffect(() => {
    setTasa(snapshot.tasaGeneradorPorMinuto);
    setPolitica(snapshot.politicaPlanificacion);
  }, [snapshot.tasaGeneradorPorMinuto, snapshot.politicaPlanificacion]);

  const reiniciar = () => {
    if (window.confirm('¿Reiniciar la simulación completa? Se perderá el estado actual.')) {
      void accionReinicio.ejecutar();
    }
  };

  return (
    <section className="panel">
      <header className="panel__encabezado">
        <div>
          <h3 className="panel__titulo">Controles de simulación</h3>
          <p className="panel__descripcion">Carga de trabajo, algoritmo del despachador y reinicio del sistema.</p>
        </div>
        <span className={`contador ${snapshot.generadorActivo ? 'contador--ok' : ''}`}>
          Generador {snapshot.generadorActivo ? 'activo' : 'detenido'}
        </span>
      </header>

      <div className="controles-grid">
        <div className="control-card">
          <h4>Generador de pedidos</h4>
          <p className="texto-muted">Tasa actual: {snapshot.tasaGeneradorPorMinuto} pedidos/minuto.</p>
          <div className="botones-fila">
            <button className="boton boton--primario" type="button" onClick={() => void accionIniciar.ejecutar()} disabled={accionIniciar.enviando || snapshot.generadorActivo}>
              Iniciar
            </button>
            <button className="boton boton--secundario" type="button" onClick={() => void accionDetener.ejecutar()} disabled={accionDetener.enviando || !snapshot.generadorActivo}>
              Detener
            </button>
          </div>
        </div>

        <div className="control-card">
          <h4>Tasa de generación</h4>
          <label className="campo">
            <span>Pedidos por minuto</span>
            <input
              type="number"
              min={1}
              step={1}
              value={tasa}
              onChange={(evento) => setTasa(Number(evento.target.value))}
            />
          </label>
          <button
            className="boton boton--primario"
            type="button"
            onClick={() => void accionTasa.ejecutar(tasa)}
            disabled={accionTasa.enviando || !Number.isInteger(tasa) || tasa <= 0}
          >
            Actualizar tasa
          </button>
        </div>

        <div className="control-card">
          <h4>Política de planificación</h4>
          <label className="campo">
            <span>Algoritmo del despachador</span>
            <select value={politica} onChange={(evento) => setPolitica(evento.target.value as TipoPolitica)}>
              {POLITICAS.map((opcion) => (
                <option value={opcion} key={opcion}>{ETIQUETAS_POLITICA[opcion]}</option>
              ))}
            </select>
          </label>
          <button
            className="boton boton--primario"
            type="button"
            onClick={() => void accionPolitica.ejecutar(politica)}
            disabled={accionPolitica.enviando || politica === snapshot.politicaPlanificacion}
          >
            Aplicar política
          </button>
        </div>

        <div className="control-card control-card--peligro">
          <h4>Reinicio</h4>
          <p className="texto-muted">Limpia pedidos, recursos, inventario y reloj simulado.</p>
          <button className="boton boton--peligro" type="button" onClick={reiniciar} disabled={accionReinicio.enviando}>
            {accionReinicio.enviando ? 'Reiniciando…' : 'Reiniciar simulación'}
          </button>
        </div>
      </div>

      <FeedbackOperacion
        mensaje={accionIniciar.mensaje ?? accionDetener.mensaje ?? accionTasa.mensaje ?? accionPolitica.mensaje ?? accionReinicio.mensaje}
        error={accionIniciar.error ?? accionDetener.error ?? accionTasa.error ?? accionPolitica.error ?? accionReinicio.error}
      />
    </section>
  );
}

export default ControlesPanel;
