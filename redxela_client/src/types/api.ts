import type { EstadoPedido, NivelServicio, TipoPolitica, TipoRecurso } from './snapshot';

/** Tipos de las peticiones y respuestas REST del contrato. */

export interface Producto {
  id: string;
  nombre: string;
  tipoMercancia: string;
  descripcion: string;
}

export interface Cliente {
  id: string;
  nombre: string;
  nivelServicio: NivelServicio;
}

export interface CrearClienteRequest {
  nombre: string;
  nivelServicio: NivelServicio;
}

export interface ItemPedidoRequest {
  productoId: string;
  cantidad: number;
}

export interface CrearPedidoRequest {
  clienteId: string;
  items: ItemPedidoRequest[];
}

export interface PedidoCreado {
  id: string;
  clienteId: string;
  nivelServicio: NivelServicio;
  totalArticulos: number;
  estado: EstadoPedido;
}

export interface RegistrarLlegadaRequest {
  productoId: string;
  cantidad: number;
}

export interface RespuestaLlegada {
  mensaje: string;
  loteId: string;
  productoId: string;
  cantidad: number;
}

export interface RespuestaMensaje {
  mensaje: string;
  [clave: string]: unknown;
}

export interface RespuestaModo {
  mensaje: string;
  modo: string;
}

export interface RespuestaGenerador {
  mensaje: string;
  activo: boolean;
}

export interface RespuestaTasa {
  mensaje: string;
  tasaPorMinuto: number;
}

export interface RespuestaPolitica {
  politica: TipoPolitica;
  nombreLegible: string;
  mensaje: string;
}

export interface ErrorApi {
  error: string;
  mensaje: string;
  [clave: string]: unknown;
}

/** Petición opcional para comparar las tres políticas de planificación. */
export interface ComparacionRequest {
  semilla?: number;
  cantidadPedidos?: number;
}

export interface MetricaNivelComparacion {
  nivel: NivelServicio;
  nombre: string;
  pedidos: number;
  pedidosDentroSla: number;
  tiempoObjetivoMinutos: number;
  tiempoEsperaPromedioMinutos: number;
  esperaMaximaMinutos: number;
  cumplimientoSlaPorcentaje: number;
}

export interface ResultadoPolitica {
  politica: TipoPolitica;
  nombreLegible: string;
  pedidosCompletados: number;
  tiempoEsperaPromedioMinutos: number;
  esperaMaximaMinutos: number;
  pedidosDentroSla: number;
  cumplimientoSlaPorcentaje: number;
  tasaThroughputPorMinuto: number;
  porNivel: MetricaNivelComparacion[];
}

export interface ComparacionPoliticas {
  semilla: number;
  cantidadPedidos: number;
  generadoEn: number;
  resultados: ResultadoPolitica[];
}

export interface MetricaNivelEstadisticas {
  nivel: NivelServicio;
  nombre: string;
  pedidosCompletados: number;
  tiempoEsperaPromedioMinutos: number;
  tiempoProcesamientoPromedioMinutos: number;
  tiempoObjetivoMinutos: number;
  pedidosDentroSla: number;
  cumplimientoSlaPorcentaje: number;
}

export interface OcupacionRecurso {
  tipo: TipoRecurso;
  nombre: string;
  total: number;
  disponibles: number;
  enUso: number;
  ocupacionPorcentaje: number;
}

export interface EstadisticasDetalladas {
  pedidosCompletados: number;
  pedidosEnEsperaStock: number;
  pedidosEnEsperaRecursos: number;
  tiempoEsperaPromedioMinutos: number;
  tiempoProcesamientoPromedioMinutos: number;
  pedidosDentroSla: number;
  cumplimientoSlaPorcentaje: number;
  tasaThroughputPorMinuto: number;
  porNivel: MetricaNivelEstadisticas[];
  ocupacionRecursos: OcupacionRecurso[];
}
