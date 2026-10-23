import type {
  EstadoPedido,
  ModoPrevencion,
  NivelServicio,
  TipoPolitica,
  TipoRecurso,
} from '../types/snapshot';

/** Etiquetas y formateadores de presentación, sin lógica de negocio. */

export const ETIQUETAS_NIVEL: Record<NivelServicio, string> = {
  EXPRES: 'Exprés',
  PRIORITARIO: 'Prioritario',
  ESTANDAR: 'Estándar',
  PROGRAMADO: 'Programado',
  ECONOMICO: 'Económico',
};

export const ETIQUETAS_ESTADO: Record<EstadoPedido, string> = {
  EN_ESPERA_STOCK: 'Espera stock',
  EN_ESPERA_RECURSOS: 'Espera recursos',
  EN_PROCESO: 'En proceso',
  BLOQUEADO: 'Bloqueado',
  COMPLETADO: 'Completado',
};

export const ETIQUETAS_RECURSO: Record<TipoRecurso, string> = {
  ENCARGADO_BODEGA: 'Encargado de Bodega',
  MONTACARGAS: 'Montacargas',
  AREA_CARGA: 'Área de Carga',
  CONTROL_CALIDAD: 'Control de Calidad',
  ESTACION_EMPAQUE: 'Estación de Empaque',
  ESCANER: 'Sistema de Escaneo',
};

export const ETIQUETAS_POLITICA: Record<TipoPolitica, string> = {
  PRIORIDAD_ESTRICTA: 'Prioridad Estricta',
  PRIORIDAD_CON_ENVEJECIMIENTO: 'Prioridad con Envejecimiento',
  MENOR_CANTIDAD_PRIMERO: 'Menor Cantidad Primero',
};

export const ETIQUETAS_MODO: Record<ModoPrevencion, string> = {
  PREVENCION_ACTIVA: 'Prevención activa',
  SIN_PREVENCION: 'Sin prevención',
};

export function porcentaje(parte: number, total: number): number {
  if (!Number.isFinite(parte) || !Number.isFinite(total) || total <= 0) return 0;
  return Math.max(0, Math.min(100, (parte / total) * 100));
}

export function formatearNumero(valor: number, decimales = 1): string {
  if (!Number.isFinite(valor)) return '—';
  return valor.toLocaleString('es-GT', {
    minimumFractionDigits: decimales,
    maximumFractionDigits: decimales,
  });
}

export function formatearMinutos(minutos: number): string {
  if (!Number.isFinite(minutos)) return '—';
  if (minutos < 1) return `${Math.round(minutos * 60)} s`;
  if (minutos < 60) return `${formatearNumero(minutos)} min`;
  const horas = Math.floor(minutos / 60);
  const resto = Math.round(minutos % 60);
  return `${horas} h ${resto} min`;
}

export function formatearSegundos(segundos: number): string {
  if (!Number.isFinite(segundos) || segundos <= 0) return '—';
  if (segundos < 60) return `${Math.round(segundos)} s`;
  return formatearMinutos(segundos / 60);
}

export function formatearHora(timestamp: number): string {
  if (!Number.isFinite(timestamp)) return '—';
  return new Date(timestamp).toLocaleTimeString('es-GT', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

export function claseNivel(nivel: NivelServicio): string {
  return `nivel nivel--${nivel.toLowerCase()}`;
}

export function claseEstado(estado: EstadoPedido): string {
  return `estado estado--${estado.toLowerCase()}`;
}
