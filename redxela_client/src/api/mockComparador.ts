import type {
  ComparacionPoliticas,
  MetricaNivelComparacion,
  ResultadoPolitica,
} from '../types/api';
import type { NivelServicio, TipoPolitica } from '../types/snapshot';

/** Resultados de demostración con la misma forma que /api/comparador/politicas. */

const NIVELES: NivelServicio[] = [
  'EXPRES',
  'PRIORITARIO',
  'ESTANDAR',
  'PROGRAMADO',
  'ECONOMICO',
];

const NOMBRES: Record<NivelServicio, string> = {
  EXPRES: 'Exprés',
  PRIORITARIO: 'Prioritario',
  ESTANDAR: 'Estándar',
  PROGRAMADO: 'Programado',
  ECONOMICO: 'Económico',
};

const OBJETIVOS: Record<NivelServicio, number> = {
  EXPRES: 1,
  PRIORITARIO: 10,
  ESTANDAR: 30,
  PROGRAMADO: 60,
  ECONOMICO: 120,
};

interface PerfilPolitica {
  politica: TipoPolitica;
  nombreLegible: string;
  espera: Record<NivelServicio, number>;
  esperaMaxima: Record<NivelServicio, number>;
  sla: Record<NivelServicio, number>;
  throughput: number;
}

const PERFILES: PerfilPolitica[] = [
  {
    politica: 'PRIORIDAD_ESTRICTA',
    nombreLegible: 'Prioridad Estricta',
    espera: { EXPRES: 0.3, PRIORITARIO: 0.7, ESTANDAR: 4.7, PROGRAMADO: 54.8, ECONOMICO: 97.9 },
    esperaMaxima: { EXPRES: 1.0, PRIORITARIO: 2.4, ESTANDAR: 12.9, PROGRAMADO: 89.4, ECONOMICO: 132.0 },
    sla: { EXPRES: 0, PRIORITARIO: 100, ESTANDAR: 100, PROGRAMADO: 56, ECONOMICO: 80 },
    throughput: 1.5,
  },
  {
    politica: 'PRIORIDAD_CON_ENVEJECIMIENTO',
    nombreLegible: 'Prioridad con Envejecimiento',
    espera: { EXPRES: 0.3, PRIORITARIO: 0.9, ESTANDAR: 9.1, PROGRAMADO: 48.2, ECONOMICO: 97.9 },
    esperaMaxima: { EXPRES: 1.1, PRIORITARIO: 3.5, ESTANDAR: 20.7, PROGRAMADO: 70.3, ECONOMICO: 130.8 },
    sla: { EXPRES: 0, PRIORITARIO: 100, ESTANDAR: 100, PROGRAMADO: 72, ECONOMICO: 80 },
    throughput: 1.5,
  },
  {
    politica: 'MENOR_CANTIDAD_PRIMERO',
    nombreLegible: 'Menor Cantidad Primero',
    espera: { EXPRES: 16.3, PRIORITARIO: 16.6, ESTANDAR: 14.1, PROGRAMADO: 24.9, ECONOMICO: 17.1 },
    esperaMaxima: { EXPRES: 136.7, PRIORITARIO: 126.7, ESTANDAR: 125.8, PROGRAMADO: 125.9, ECONOMICO: 131.9 },
    sla: { EXPRES: 0, PRIORITARIO: 78, ESTANDAR: 84, PROGRAMADO: 74, ECONOMICO: 98 },
    throughput: 1.5,
  },
];

let ultimaComparacion: ComparacionPoliticas | null = null;

const redondear = (valor: number) => Math.round(valor * 10) / 10;

function distribuir(cantidad: number): Map<NivelServicio, number> {
  const base = Math.floor(cantidad / NIVELES.length);
  const resto = cantidad % NIVELES.length;
  return new Map(NIVELES.map((nivel, indice) => [nivel, base + (indice < resto ? 1 : 0)]));
}

function crearResultado(perfil: PerfilPolitica, distribucion: Map<NivelServicio, number>): ResultadoPolitica {
  const porNivel: MetricaNivelComparacion[] = NIVELES.map((nivel) => {
    const pedidos = distribucion.get(nivel) ?? 0;
    const dentro = Math.round((pedidos * perfil.sla[nivel]) / 100);
    return {
      nivel,
      nombre: NOMBRES[nivel],
      pedidos,
      pedidosDentroSla: dentro,
      tiempoObjetivoMinutos: OBJETIVOS[nivel],
      tiempoEsperaPromedioMinutos: perfil.espera[nivel],
      esperaMaximaMinutos: perfil.esperaMaxima[nivel],
      cumplimientoSlaPorcentaje: pedidos === 0 ? 0 : redondear((dentro * 100) / pedidos),
    };
  });

  const total = porNivel.reduce((suma, metrica) => suma + metrica.pedidos, 0);
  const dentro = porNivel.reduce((suma, metrica) => suma + metrica.pedidosDentroSla, 0);
  const espera = porNivel.reduce(
    (suma, metrica) => suma + metrica.tiempoEsperaPromedioMinutos * metrica.pedidos,
    0,
  );

  return {
    politica: perfil.politica,
    nombreLegible: perfil.nombreLegible,
    pedidosCompletados: total,
    tiempoEsperaPromedioMinutos: total === 0 ? 0 : redondear(espera / total),
    esperaMaximaMinutos: Math.max(...porNivel.map((metrica) => metrica.esperaMaximaMinutos)),
    pedidosDentroSla: dentro,
    cumplimientoSlaPorcentaje: total === 0 ? 0 : redondear((dentro * 100) / total),
    tasaThroughputPorMinuto: perfil.throughput,
    porNivel,
  };
}

/** Genera y conserva un resultado mock para que GET y POST funcionen sin backend. */
export function crearComparacionMock(semilla: number, cantidadPedidos: number): ComparacionPoliticas {
  const cantidad = Math.min(1000, Math.max(1, Math.trunc(cantidadPedidos)));
  const distribucion = distribuir(cantidad);
  ultimaComparacion = {
    semilla,
    cantidadPedidos: cantidad,
    generadoEn: Date.now(),
    resultados: PERFILES.map((perfil) => crearResultado(perfil, distribucion)),
  };
  return ultimaComparacion;
}

export function obtenerUltimaComparacionMock(): ComparacionPoliticas | null {
  if (!ultimaComparacion) {
    return crearComparacionMock(2026, 250);
  }
  return ultimaComparacion;
}
