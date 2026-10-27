/** Tipos del snapshot de estado definido en docs/CONTRATO_API.md. */

export type NivelServicio =
  | 'EXPRES'
  | 'PRIORITARIO'
  | 'ESTANDAR'
  | 'PROGRAMADO'
  | 'ECONOMICO';

export type EstadoPedido =
  | 'EN_ESPERA_STOCK'
  | 'EN_ESPERA_RECURSOS'
  | 'EN_PROCESO'
  | 'BLOQUEADO'
  | 'COMPLETADO';

export type TipoRecurso =
  | 'ENCARGADO_BODEGA'
  | 'MONTACARGAS'
  | 'AREA_CARGA'
  | 'CONTROL_CALIDAD'
  | 'ESTACION_EMPAQUE'
  | 'ESCANER';

export type ModoPrevencion = 'PREVENCION_ACTIVA' | 'SIN_PREVENCION';

export type TipoPolitica =
  | 'PRIORIDAD_ESTRICTA'
  | 'PRIORIDAD_CON_ENVEJECIMIENTO'
  | 'MENOR_CANTIDAD_PRIMERO';

export interface RecursoEstado {
  tipo: TipoRecurso;
  nombre: string;
  total: number;
  disponibles: number;
  enUso: number;
  /** Solo lo envía el backend para el recurso ESCANER. */
  retenidoPor?: string | null;
  pedidosAsignados: string[];
  pedidosEnEspera: string[];
}

export interface PedidoProceso {
  id: string;
  clienteId: string;
  clienteNombre: string;
  nivelServicio: NivelServicio;
  totalArticulos: number;
  estado: EstadoPedido;
  progreso: number;
  tiempoRestanteEstimadoSegundos: number;
  recursosRetenidos: TipoRecurso[];
  recursoEsperado: TipoRecurso | null;
}

export interface ElementoEspera {
  id: string;
  clienteNombre: string;
  nivelServicio: NivelServicio;
  totalArticulos: number;
  estado: EstadoPedido;
  tCreacion: number;
  tiempoEsperaMinutos: number;
  prioridadCalculada: number;
  motivoPosicion: string;
}

export interface UbicacionEstado {
  id: string;
  pasillo: number;
  nivel: number;
  posicion: number;
  loteId: string;
  productoId: string;
  productoNombre: string;
  unidades: number;
  capacidad: number;
}

export interface ColaRecepcion {
  elementosEnCola: number;
  capacidadMaxima: number;
}

export interface AlmacenEstado {
  capacidadTotal: number;
  unidadesOcupadas: number;
  ubicaciones: UbicacionEstado[];
  colaRecepcion: ColaRecepcion;
}

export interface ReporteInterbloqueo {
  hayInterbloqueo: boolean;
  pedidosInvolucrados: string[];
  recursosEnDisputa: TipoRecurso[];
  explicacion: string | null;
}

export interface Estadisticas {
  pedidosCompletados: number;
  tiempoEsperaPromedioMinutos: number;
  tiempoProcesamientoPromedioMinutos: number;
  cumplimientoSlaPorcentaje: number;
  tasaThroughputPorMinuto: number;
  pedidosEnEsperaStock: number;
  pedidosEnEsperaRecursos: number;
}

export interface SnapshotEstado {
  timestamp: number;
  tiempoSimuladoMinutos: number;
  modoPrevencion: ModoPrevencion;
  politicaPlanificacion: TipoPolitica;
  generadorActivo: boolean;
  tasaGeneradorPorMinuto: number;
  recursos: RecursoEstado[];
  pedidosEnProceso: PedidoProceso[];
  listaEspera: ElementoEspera[];
  almacen: AlmacenEstado;
  interbloqueo: ReporteInterbloqueo;
  estadisticas: Estadisticas;
}
