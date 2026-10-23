import type { SnapshotEstado } from '../types/snapshot';

/**
 * Snapshot de demostración basado en el JSON del contrato.
 * Permite revisar todos los paneles sin backend cuando VITE_MOCK_MODE=true.
 */

let ciclo = 0;

const recursos: SnapshotEstado['recursos'] = [
  {
    tipo: 'ENCARGADO_BODEGA',
    nombre: 'Encargado de Bodega',
    total: 10,
    disponibles: 6,
    enUso: 4,
    pedidosAsignados: ['PED-101', 'PED-102', 'PED-103', 'PED-104'],
    pedidosEnEspera: ['PED-105'],
  },
  {
    tipo: 'MONTACARGAS',
    nombre: 'Montacargas',
    total: 3,
    disponibles: 0,
    enUso: 3,
    pedidosAsignados: ['PED-101', 'PED-102', 'PED-106'],
    pedidosEnEspera: ['PED-107'],
  },
  {
    tipo: 'AREA_CARGA',
    nombre: 'Área de Carga',
    total: 2,
    disponibles: 0,
    enUso: 2,
    pedidosAsignados: ['PED-101', 'PED-102'],
    pedidosEnEspera: ['PED-108'],
  },
  {
    tipo: 'CONTROL_CALIDAD',
    nombre: 'Control de Calidad',
    total: 4,
    disponibles: 3,
    enUso: 1,
    pedidosAsignados: ['PED-109'],
    pedidosEnEspera: [],
  },
  {
    tipo: 'ESTACION_EMPAQUE',
    nombre: 'Estación de Empaque',
    total: 6,
    disponibles: 4,
    enUso: 2,
    pedidosAsignados: ['PED-101', 'PED-109'],
    pedidosEnEspera: [],
  },
  {
    tipo: 'ESCANER',
    nombre: 'Sistema de Escaneo',
    total: 1,
    disponibles: 0,
    enUso: 1,
    retenidoPor: 'RECEPCION',
    pedidosAsignados: [],
    pedidosEnEspera: ['PED-106'],
  },
];

const ubicaciones: SnapshotEstado['almacen']['ubicaciones'] = Array.from(
  { length: 20 },
  (_, indice) => {
    const numero = indice + 1;
    const producto = 1 + (indice % 6);
    const lote = 1 + Math.floor(indice / 4);
    return {
      id: `U-${String(numero).padStart(2, '0')}`,
      pasillo: Math.floor(indice / 10) + 1,
      nivel: Math.floor((indice % 10) / 5) + 1,
      posicion: (indice % 5) + 1,
      loteId: `LOTE-2026-${String(lote).padStart(2, '0')}`,
      productoId: `PROD-${String(producto).padStart(2, '0')}`,
      productoNombre: [
        'Cristalería Fina',
        'Cerámica Artesanal',
        'Prensa Hidráulica',
        'Bloques de Acero',
        'Sofá Modular',
        'Armario Metálico',
      ][producto - 1],
      unidades: 10 + ((indice * 7) % 41),
      capacidad: 50,
    };
  },
);

export function crearSnapshotSimulado(): SnapshotEstado {
  ciclo += 1;
  const progreso = 40 + (ciclo % 50);
  return {
    timestamp: Date.now(),
    tiempoSimuladoMinutos: 142.5 + ciclo * 0.1,
    modoPrevencion: 'SIN_PREVENCION',
    politicaPlanificacion: 'PRIORIDAD_CON_ENVEJECIMIENTO',
    generadorActivo: true,
    tasaGeneradorPorMinuto: 5,
    recursos,
    pedidosEnProceso: [
      {
        id: 'PED-101',
        clienteId: 'CLI-01',
        clienteNombre: 'Distribuidora Exprés Quetzal',
        nivelServicio: 'EXPRES',
        totalArticulos: 12,
        estado: 'EN_PROCESO',
        progreso,
        tiempoRestanteEstimadoSegundos: Math.max(2, 60 - ciclo),
        recursosRetenidos: ['MONTACARGAS', 'ENCARGADO_BODEGA'],
        recursoEsperado: 'AREA_CARGA',
      },
      {
        id: 'PED-102',
        clienteId: 'CLI-02',
        clienteNombre: 'Comercial San Marcos',
        nivelServicio: 'PRIORITARIO',
        totalArticulos: 8,
        estado: 'BLOQUEADO',
        progreso: 35,
        tiempoRestanteEstimadoSegundos: 18,
        recursosRetenidos: ['AREA_CARGA'],
        recursoEsperado: 'MONTACARGAS',
      },
      {
        id: 'PED-103',
        clienteId: 'CLI-03',
        clienteNombre: 'Importadora Los Altos',
        nivelServicio: 'ESTANDAR',
        totalArticulos: 5,
        estado: 'EN_PROCESO',
        progreso: 72,
        tiempoRestanteEstimadoSegundos: 9,
        recursosRetenidos: ['ENCARGADO_BODEGA', 'ESTACION_EMPAQUE'],
        recursoEsperado: null,
      },
    ],
    listaEspera: [
      {
        id: 'PED-105',
        clienteNombre: 'Ferretería Central Xela',
        nivelServicio: 'ECONOMICO',
        totalArticulos: 4,
        estado: 'EN_ESPERA_RECURSOS',
        tCreacion: Date.now() - 20 * 60 * 1000,
        tiempoEsperaMinutos: 20,
        prioridadCalculada: 3.8,
        motivoPosicion: 'Envejecimiento: 20 min en espera frente a SLA de 120 min',
      },
      {
        id: 'PED-106',
        clienteNombre: 'Bodega El Carmen',
        nivelServicio: 'PROGRAMADO',
        totalArticulos: 9,
        estado: 'EN_ESPERA_RECURSOS',
        tCreacion: Date.now() - 8 * 60 * 1000,
        tiempoEsperaMinutos: 8,
        prioridadCalculada: 2.6,
        motivoPosicion: 'Prioridad base del nivel Programado; desempate FIFO',
      },
      {
        id: 'PED-107',
        clienteNombre: 'Tienda El Sol',
        nivelServicio: 'PRIORITARIO',
        totalArticulos: 3,
        estado: 'EN_ESPERA_RECURSOS',
        tCreacion: Date.now() - 3 * 60 * 1000,
        tiempoEsperaMinutos: 3,
        prioridadCalculada: 1.9,
        motivoPosicion: 'Menor cantidad de artículos entre los procesos listos',
      },
    ],
    almacen: {
      capacidadTotal: 1000,
      unidadesOcupadas: 600,
      ubicaciones,
      colaRecepcion: { elementosEnCola: 2, capacidadMaxima: 50 },
    },
    interbloqueo: {
      hayInterbloqueo: true,
      pedidosInvolucrados: ['PED-101', 'PED-102'],
      recursosEnDisputa: ['MONTACARGAS', 'AREA_CARGA'],
      explicacion:
        'PED-101 retiene Montacargas y espera Área de Carga; PED-102 retiene Área de Carga y espera Montacargas. Se forma un ciclo de espera circular.',
    },
    estadisticas: {
      pedidosCompletados: 45,
      tiempoEsperaPromedioMinutos: 6.4,
      tiempoProcesamientoPromedioMinutos: 12.1,
      cumplimientoSlaPorcentaje: 93.3,
      tasaThroughputPorMinuto: 3.2,
    },
  };
}
