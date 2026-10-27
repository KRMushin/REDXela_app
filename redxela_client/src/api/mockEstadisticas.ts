import type { EstadisticasDetalladas } from '../types/api';

/** Detalle de estadísticas de demostración con la forma de GET /api/estadisticas. */

const POR_NIVEL: EstadisticasDetalladas['porNivel'] = [
  {
    nivel: 'EXPRES',
    nombre: 'Exprés',
    pedidosCompletados: 9,
    tiempoEsperaPromedioMinutos: 1.2,
    tiempoProcesamientoPromedioMinutos: 4.5,
    tiempoObjetivoMinutos: 1,
    pedidosDentroSla: 2,
    cumplimientoSlaPorcentaje: 22.2,
  },
  {
    nivel: 'PRIORITARIO',
    nombre: 'Prioritario',
    pedidosCompletados: 9,
    tiempoEsperaPromedioMinutos: 2.8,
    tiempoProcesamientoPromedioMinutos: 7.4,
    tiempoObjetivoMinutos: 10,
    pedidosDentroSla: 9,
    cumplimientoSlaPorcentaje: 100,
  },
  {
    nivel: 'ESTANDAR',
    nombre: 'Estándar',
    pedidosCompletados: 9,
    tiempoEsperaPromedioMinutos: 5.9,
    tiempoProcesamientoPromedioMinutos: 11.2,
    tiempoObjetivoMinutos: 30,
    pedidosDentroSla: 9,
    cumplimientoSlaPorcentaje: 100,
  },
  {
    nivel: 'PROGRAMADO',
    nombre: 'Programado',
    pedidosCompletados: 9,
    tiempoEsperaPromedioMinutos: 8.4,
    tiempoProcesamientoPromedioMinutos: 14.8,
    tiempoObjetivoMinutos: 60,
    pedidosDentroSla: 9,
    cumplimientoSlaPorcentaje: 100,
  },
  {
    nivel: 'ECONOMICO',
    nombre: 'Económico',
    pedidosCompletados: 9,
    tiempoEsperaPromedioMinutos: 13.7,
    tiempoProcesamientoPromedioMinutos: 22.6,
    tiempoObjetivoMinutos: 120,
    pedidosDentroSla: 8,
    cumplimientoSlaPorcentaje: 88.9,
  },
];

const OCUPACION: EstadisticasDetalladas['ocupacionRecursos'] = [
  { tipo: 'ENCARGADO_BODEGA', nombre: 'Encargado de Bodega', total: 10, disponibles: 6, enUso: 4, ocupacionPorcentaje: 40 },
  { tipo: 'MONTACARGAS', nombre: 'Montacargas', total: 3, disponibles: 0, enUso: 3, ocupacionPorcentaje: 100 },
  { tipo: 'AREA_CARGA', nombre: 'Área de Carga', total: 2, disponibles: 0, enUso: 2, ocupacionPorcentaje: 100 },
  { tipo: 'CONTROL_CALIDAD', nombre: 'Control de Calidad', total: 4, disponibles: 3, enUso: 1, ocupacionPorcentaje: 25 },
  { tipo: 'ESTACION_EMPAQUE', nombre: 'Estación de Empaque', total: 6, disponibles: 4, enUso: 2, ocupacionPorcentaje: 33.3 },
  { tipo: 'ESCANER', nombre: 'Sistema de Escaneo', total: 1, disponibles: 0, enUso: 1, ocupacionPorcentaje: 100 },
];

export function crearEstadisticasSimuladas(): EstadisticasDetalladas {
  return {
    pedidosEnEsperaStock: 1,
    pedidosEnEsperaRecursos: 3,
    pedidosCompletados: 45,
    tiempoEsperaPromedioMinutos: 6.4,
    tiempoProcesamientoPromedioMinutos: 12.1,
    pedidosDentroSla: 37,
    cumplimientoSlaPorcentaje: 82.2,
    tasaThroughputPorMinuto: 3.2,
    porNivel: POR_NIVEL,
    ocupacionRecursos: OCUPACION,
  };
}
