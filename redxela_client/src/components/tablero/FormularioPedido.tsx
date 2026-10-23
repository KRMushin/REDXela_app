import { useState, type FormEvent } from 'react';
import type { Cliente, CrearPedidoRequest, PedidoCreado, Producto } from '../../types/api';
import { crearPedido } from '../../api/redxelaApi';
import { useAccion } from '../../hooks/useAccion';
import { ETIQUETAS_NIVEL } from '../../utils/formato';
import FeedbackOperacion from '../common/FeedbackOperacion';
import NivelBadge from '../common/NivelBadge';

interface FormularioPedidoProps {
  clientes: Cliente[];
  productos: Producto[];
  onCreado?: (pedido: PedidoCreado) => void;
}

interface LineaPedido {
  productoId: string;
  cantidad: number;
}

/** Alta manual de un pedido con uno o más productos del catálogo. */
function FormularioPedido({ clientes, productos, onCreado }: FormularioPedidoProps) {
  const [clienteId, setClienteId] = useState('');
  const [lineas, setLineas] = useState<LineaPedido[]>([{ productoId: '', cantidad: 1 }]);
  const [errorValidacion, setErrorValidacion] = useState<string | null>(null);
  const { enviando, mensaje, error, ejecutar } = useAccion(
    crearPedido,
    'Pedido registrado correctamente.',
  );

  const cliente = clientes.find((item) => item.id === clienteId);

  const actualizarLinea = (indice: number, cambio: Partial<LineaPedido>) => {
    setLineas((actuales) =>
      actuales.map((linea, posicion) => (posicion === indice ? { ...linea, ...cambio } : linea)),
    );
    setErrorValidacion(null);
  };

  const enviar = async (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    if (!clienteId) {
      setErrorValidacion('Seleccione un cliente.');
      return;
    }
    if (lineas.some((linea) => !linea.productoId || linea.cantidad <= 0)) {
      setErrorValidacion('Cada línea necesita un producto y una cantidad mayor a cero.');
      return;
    }
    const cuerpo: CrearPedidoRequest = {
      clienteId,
      items: lineas.map((linea) => ({ productoId: linea.productoId, cantidad: linea.cantidad })),
    };
    const creado = (await ejecutar(cuerpo)) as PedidoCreado | undefined;
    if (creado) {
      onCreado?.(creado);
      setLineas([{ productoId: '', cantidad: 1 }]);
    }
  };

  return (
    <form className="form-card" onSubmit={(evento) => void enviar(evento)}>
      <h4>Nuevo pedido</h4>
      <label className="campo">
        <span>Cliente</span>
        <select value={clienteId} onChange={(evento) => setClienteId(evento.target.value)}>
          <option value="">Seleccione un cliente…</option>
          {clientes.map((item) => (
            <option value={item.id} key={item.id}>{item.id} · {item.nombre}</option>
          ))}
        </select>
      </label>
      {cliente && (
        <p className="form-card__detalle">
          Prioridad: <NivelBadge nivel={cliente.nivelServicio} /> {ETIQUETAS_NIVEL[cliente.nivelServicio]}
        </p>
      )}
      <div className="lineas-pedido">
        <span className="mini-etiqueta">Productos</span>
        {lineas.map((linea, indice) => (
          <div className="linea-pedido" key={indice}>
            <select
              aria-label={`Producto de la línea ${indice + 1}`}
              value={linea.productoId}
              onChange={(evento) => actualizarLinea(indice, { productoId: evento.target.value })}
            >
              <option value="">Producto…</option>
              {productos.map((producto) => (
                <option value={producto.id} key={producto.id}>{producto.nombre}</option>
              ))}
            </select>
            <input
              aria-label={`Cantidad de la línea ${indice + 1}`}
              type="number"
              min={1}
              value={linea.cantidad}
              onChange={(evento) => actualizarLinea(indice, { cantidad: Number(evento.target.value) })}
            />
            <button
              className="boton boton--icono"
              type="button"
              title="Quitar línea"
              onClick={() => setLineas((actuales) => actuales.filter((_, posicion) => posicion !== indice))}
              disabled={lineas.length === 1}
            >
              ×
            </button>
          </div>
        ))}
        <button
          className="boton boton--secundario boton--pequeno"
          type="button"
          onClick={() => setLineas((actuales) => [...actuales, { productoId: '', cantidad: 1 }])}
        >
          + Agregar producto
        </button>
      </div>
      <button className="boton boton--primario" type="submit" disabled={enviando || clientes.length === 0}>
        {enviando ? 'Registrando…' : 'Registrar pedido'}
      </button>
      <FeedbackOperacion mensaje={mensaje} error={errorValidacion ?? error} />
    </form>
  );
}

export default FormularioPedido;
