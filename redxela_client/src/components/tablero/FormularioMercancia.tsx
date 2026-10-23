import { useState, type FormEvent } from 'react';
import type { Producto, RegistrarLlegadaRequest, RespuestaLlegada } from '../../types/api';
import { registrarMercancia } from '../../api/redxelaApi';
import { useAccion } from '../../hooks/useAccion';
import FeedbackOperacion from '../common/FeedbackOperacion';

interface FormularioMercanciaProps {
  productos: Producto[];
}

/** Registro de la llegada de un lote al área de Recepción. */
function FormularioMercancia({ productos }: FormularioMercanciaProps) {
  const [productoId, setProductoId] = useState('');
  const [cantidad, setCantidad] = useState(10);
  const [loteRegistrado, setLoteRegistrado] = useState<string | null>(null);
  const [errorValidacion, setErrorValidacion] = useState<string | null>(null);
  const { enviando, mensaje, error, ejecutar } = useAccion(
    registrarMercancia,
    'Mercadería recibida y encolada para escaneo.',
  );

  const enviar = async (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    if (!productoId) {
      setErrorValidacion('Seleccione un producto del catálogo.');
      return;
    }
    if (!Number.isInteger(cantidad) || cantidad <= 0) {
      setErrorValidacion('La cantidad debe ser un entero mayor a cero.');
      return;
    }
    setErrorValidacion(null);
    const cuerpo: RegistrarLlegadaRequest = { productoId, cantidad };
    const respuesta = (await ejecutar(cuerpo)) as RespuestaLlegada | undefined;
    if (respuesta) setLoteRegistrado(respuesta.loteId);
  };

  return (
    <form className="form-card" onSubmit={(evento) => void enviar(evento)}>
      <h4>Llegada de mercadería</h4>
      <label className="campo">
        <span>Producto</span>
        <select
          value={productoId}
          onChange={(evento) => {
            setProductoId(evento.target.value);
            setErrorValidacion(null);
          }}
        >
          <option value="">Seleccione un producto…</option>
          {productos.map((producto) => (
            <option value={producto.id} key={producto.id}>
              {producto.id} · {producto.nombre} ({producto.tipoMercancia})
            </option>
          ))}
        </select>
      </label>
      <label className="campo">
        <span>Cantidad de unidades</span>
        <input
          type="number"
          min={1}
          step={1}
          value={cantidad}
          onChange={(evento) => {
            setCantidad(Number(evento.target.value));
            setErrorValidacion(null);
          }}
        />
      </label>
      <button className="boton boton--primario" type="submit" disabled={enviando || productos.length === 0}>
        {enviando ? 'Enviando…' : 'Registrar llegada'}
      </button>
      {loteRegistrado && <p className="form-card__detalle">Lote generado: <strong>{loteRegistrado}</strong></p>}
      <FeedbackOperacion mensaje={mensaje} error={errorValidacion ?? error} />
    </form>
  );
}

export default FormularioMercancia;
