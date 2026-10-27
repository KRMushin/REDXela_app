import { useCallback, useEffect, useState } from 'react';
import type { EstadisticasDetalladas } from '../types/api';
import { obtenerEstadisticas } from '../api/redxelaApi';
import { crearEstadisticasSimuladas } from '../api/mockEstadisticas';
import { mensajeError } from '../utils/errores';

const MODO_MOCK = import.meta.env.VITE_MOCK_MODE === 'true';
const INTERVALO_ACTUALIZACION_MS = 3000;

interface ResultadoEstadisticas {
  detalle: EstadisticasDetalladas | null;
  cargando: boolean;
  error: string | null;
}

/** Mantiene actualizado el detalle por nivel desde GET /api/estadisticas. */
export function useEstadisticasDetalladas(): ResultadoEstadisticas {
  const [detalle, setDetalle] = useState<EstadisticasDetalladas | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const cargar = useCallback(async (mostrarCarga = false) => {
    if (mostrarCarga) setCargando(true);
    try {
      const datos = MODO_MOCK
        ? crearEstadisticasSimuladas()
        : ((await obtenerEstadisticas()) as EstadisticasDetalladas);
      setDetalle(datos);
      setError(null);
    } catch (fallo) {
      setError(mensajeError(fallo));
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    void cargar(true);
    const intervalo = window.setInterval(() => {
      void cargar();
    }, INTERVALO_ACTUALIZACION_MS);
    return () => window.clearInterval(intervalo);
  }, [cargar]);

  return { detalle, cargando, error };
}
