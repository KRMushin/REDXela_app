import type { ErrorApi } from '../types/api';

interface ErrorHttp extends Error {
  status?: number;
  detalle?: ErrorApi | string;
}

/** Convierte errores del API en mensajes visibles para el operador. */
export function mensajeError(error: unknown): string {
  if (error instanceof Error) {
    const errorHttp = error as ErrorHttp;
    const detalle = errorHttp.detalle;
    const mensajeServidor =
      typeof detalle === 'object' && detalle !== null && 'mensaje' in detalle
        ? detalle.mensaje
        : null;

    if (mensajeServidor) {
      return errorHttp.status
        ? `${mensajeServidor} (HTTP ${errorHttp.status})`
        : mensajeServidor;
    }
    if (errorHttp.status) {
      return `${error.message} (HTTP ${errorHttp.status})`;
    }
    return error.message || 'No fue posible completar la operación';
  }
  return 'No fue posible completar la operación';
}

/** Permite distinguir un recurso aún no generado de un fallo real del backend. */
export function esErrorNoEncontrado(error: unknown): boolean {
  if (typeof error !== 'object' || error === null || !('status' in error)) {
    return false;
  }
  return (error as { status?: number }).status === 404;
}
