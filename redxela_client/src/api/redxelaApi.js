/**
 * Cliente HTTP único del frontend.
 * Cada función corresponde a un endpoint REST del contrato de REDXela.
 */

const API_BASE_URL = (import.meta.env.VITE_API_URL ?? '/api').replace(/\/+$/, '');

async function solicitar(ruta, opciones = {}) {
  const respuesta = await fetch(`${API_BASE_URL}${ruta}`, {
    ...opciones,
    headers: {
      Accept: 'application/json',
      ...(opciones.body ? { 'Content-Type': 'application/json' } : {}),
      ...(opciones.headers ?? {}),
    },
  });

  const texto = await respuesta.text();
  let datos = null;
  if (texto) {
    try {
      datos = JSON.parse(texto);
    } catch {
      datos = texto;
    }
  }

  if (!respuesta.ok) {
    const mensajeServidor =
      datos && typeof datos === 'object' && 'mensaje' in datos
        ? datos.mensaje
        : `Error HTTP ${respuesta.status}`;
    const error = new Error(mensajeServidor);
    error.status = respuesta.status;
    error.detalle = datos;
    throw error;
  }

  return datos;
}

const enviarJson = (ruta, metodo, cuerpo) =>
  solicitar(ruta, {
    method: metodo,
    body: JSON.stringify(cuerpo),
  });

export const obtenerCatalogo = () => solicitar('/catalogo');

export const listarClientes = () => solicitar('/clientes');

export const crearCliente = (datos) => enviarJson('/clientes', 'POST', datos);

export const crearPedido = (datos) => enviarJson('/pedidos', 'POST', datos);

export const registrarMercancia = (datos) =>
  enviarJson('/recepcion/llegada', 'POST', datos);

export const cambiarPolitica = (politica) =>
  enviarJson('/planificacion/politica', 'PUT', { politica });

export const iniciarGenerador = () => solicitar('/simulacion/generador/iniciar', { method: 'POST' });

export const detenerGenerador = () => solicitar('/simulacion/generador/detener', { method: 'POST' });

export const ajustarTasa = (pedidosPorMinuto) =>
  enviarJson('/simulacion/generador/tasa', 'PUT', { pedidosPorMinuto });

export const cambiarModoPrevencion = (modo) =>
  enviarJson('/interbloqueo/modo', 'PUT', { modo });

export const forzarConflicto = () => solicitar('/interbloqueo/forzar', { method: 'POST' });

export const resolverConflicto = (pedidoGanadorId) =>
  enviarJson('/interbloqueo/resolver', 'POST', { pedidoGanadorId });

export const reiniciarSimulacion = () => solicitar('/simulacion/reiniciar', { method: 'POST' });

/** Respaldo del stream cuando el navegador no soporta EventSource. */
export const obtenerSnapshot = () => solicitar('/estado/snapshot');

/** Métricas detalladas por nivel y por recurso. */
export const obtenerEstadisticas = () => solicitar('/estadisticas');

/** Lanza la comparación aislada de las tres políticas. */
export const compararPoliticas = (datos = {}) =>
  enviarJson('/comparador/politicas', 'POST', datos);

/** Recupera el último resultado de comparación disponible. */
export const obtenerUltimaComparacion = () => solicitar('/comparador/politicas');
