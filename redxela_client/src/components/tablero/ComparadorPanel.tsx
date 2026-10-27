import { useState } from 'react';
import type { FormEvent } from 'react';
import EncabezadoPanel from '../common/EncabezadoPanel';
import TablaComparativa from './TablaComparativa';
import TablaNivelesComparador from './TablaNivelesComparador';
import { useComparadorPoliticas } from '../../hooks/useComparadorPoliticas';
import type { FiltrosComparacion } from '../../hooks/useComparadorPoliticas';
import { formatearHora } from '../../utils/formato';

/** Panel 9: comparación aislada de las políticas de planificación. */
function ComparadorPanel() {
  const [semilla, setSemilla] = useState('');
  const [cantidad, setCantidad] = useState('250');
  const { comparacion, cargando, comparando, error, comparar } = useComparadorPoliticas();

  const semillaNumero = semilla.trim() === '' ? undefined : Number(semilla);
  const cantidadNumero = cantidad.trim() === '' ? undefined : Number(cantidad);
  const semillaValida = semillaNumero === undefined
    || (Number.isInteger(semillaNumero) && semillaNumero >= 0);
  const cantidadValida = cantidadNumero === undefined
    || (Number.isInteger(cantidadNumero) && cantidadNumero >= 1 && cantidadNumero <= 1000);
  const formularioValido = semillaValida && cantidadValida;

  const ejecutar = (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    if (!formularioValido || comparando) return;
    const filtros: FiltrosComparacion = {
      semilla: semillaNumero,
      cantidadPedidos: cantidadNumero,
    };
    void comparar(filtros);
  };

  const extra = cargando
    ? <span className="contador">Cargando último resultado…</span>
    : comparacion
      ? <span className="contador">Semilla {comparacion.semilla} · {comparacion.cantidadPedidos} pedidos</span>
      : null;

  return (
    <section className="panel">
      <EncabezadoPanel
        titulo="Comparador de políticas"
        descripcion="Ejecuta la misma carga fija con las tres políticas en una simulación aislada."
        extra={extra}
      />

      <form className="comparador-form" onSubmit={ejecutar}>
        <label className="campo">
          Semilla (opcional)
          <input
            type="number"
            min={0}
            step={1}
            placeholder="2026"
            value={semilla}
            onChange={(evento) => setSemilla(evento.target.value)}
          />
        </label>
        <label className="campo">
          Cantidad de pedidos (opcional)
          <input
            type="number"
            min={1}
            max={1000}
            step={1}
            placeholder="250"
            value={cantidad}
            onChange={(evento) => setCantidad(evento.target.value)}
          />
        </label>
        <button className="boton boton--primario comparador-form__accion" type="submit" disabled={!formularioValido || comparando}>
          {comparando ? 'Comparando…' : 'Comparar políticas'}
        </button>
      </form>
      <p className="comparador-nota">
        Deje los campos vacíos para usar semilla 2026 y 250 pedidos. La simulación no altera el estado en vivo.
      </p>

      {error && <p className="feedback__mensaje feedback__mensaje--error" role="alert">{error}</p>}

      {cargando && !comparacion && (
        <p className="estado-vacio">Consultando el último resultado del comparador…</p>
      )}

      {!cargando && !comparacion && (
        <p className="estado-vacio">Aún no hay una comparación. Ejecute “Comparar políticas” para generarla.</p>
      )}

      {comparacion && (
        <>
          <h4 className="subtitulo">Resultado global</h4>
          <TablaComparativa resultados={comparacion.resultados} />
          <h4 className="subtitulo">Espera promedio por nivel de servicio</h4>
          <TablaNivelesComparador resultados={comparacion.resultados} />
          <p className="comparador-nota">Generado a las {formatearHora(comparacion.generadoEn)}.</p>
        </>
      )}
    </section>
  );
}

export default ComparadorPanel;
