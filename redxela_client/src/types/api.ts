import type { EstadoPedido, NivelServicio, TipoPolitica } from './snapshot';

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
