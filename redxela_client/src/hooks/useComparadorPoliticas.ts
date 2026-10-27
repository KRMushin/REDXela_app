import { useCallback, useEffect, useState } from 'react';
import type { ComparacionPoliticas } from '../types/api';
import { compararPoliticas, obtenerUltimaComparacion } from '../api/redxelaApi';
import { crearComparacionMock, obtenerUltimaComparacionMock } from '../api/mockComparador';
import { esErrorNoEncontrado, mensajeError } from '../utils/errores';

const MODO_MOCK = import.meta.env.VITE_MOCK_MODE === 'true';

export interface FiltrosComparacion {
  semilla?: number;
  cantidadPedidos?: number;
}

interface ResultadoComparador {
  comparacion: ComparacionPoliticas | null;
  cargando: boolean;
  comparando: boolean;
  error: string | null;
  comparar: (filtros: FiltrosComparacion) => Promise<void>;
}

/** Carga el último resultado y lanza comparaciones aisladas de políticas. */
export function useComparadorPoliticas(): ResultadoComparador {
  const [comparacion, setComparacion] = useState<ComparacionPoliticas | null>(null);
  const [cargando, setCargando] = useState(true);
  const [comparando, setComparando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let vigente = true;
    const cargar = async () => {
      setCargando(true);
      try {
        const datos = MODO_MOCK
          ? obtenerUltimaComparacionMock()
          : ((await obtenerUltimaComparacion()) as ComparacionPoliticas);
        if (vigente) {
          setComparacion(datos);
          setError(null);
        }
      } catch (fallo) {
        if (vigente && !esErrorNoEncontrado(fallo)) {
          setError(mensajeError(fallo));
        }
      } finally {
        if (vigente) setCargando(false);
      }
    };
    void cargar();
    return () => {
      vigente = false;
    };
  }, []);

  const comparar = useCallback(async (filtros: FiltrosComparacion) => {
    setComparando(true);
    setError(null);
    try {
      const datos = MODO_MOCK
        ? crearComparacionMock(filtros.semilla ?? 2026, filtros.cantidadPedidos ?? 250)
        : ((await compararPoliticas(filtros)) as ComparacionPoliticas);
      setComparacion(datos);
    } catch (fallo) {
      setError(mensajeError(fallo));
    } finally {
      setComparando(false);
    }
  }, []);

  return { comparacion, cargando, comparando, error, comparar };
}
