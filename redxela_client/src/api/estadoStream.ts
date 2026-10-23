import type { SnapshotEstado } from '../types/snapshot';
import { obtenerSnapshot } from './redxelaApi';

const API_BASE_URL = (import.meta.env.VITE_API_URL ?? '/api').replace(/\/+$/, '');

interface CallbacksEstado {
  onSnapshot: (snapshot: SnapshotEstado) => void;
  onConexion: (conectado: boolean) => void;
  onError: (mensaje: string | null) => void;
}

const ESPERA_BASE_MS = 1500;
const ESPERA_MAXIMA_MS = 10000;
const INTERVALO_RESPALDO_MS = 1000;

/**
 * Abre el stream SSE y programa una reconexión con espera exponencial.
 * Si EventSource no existe, consulta el endpoint de snapshot periódicamente.
 */
export function conectarEstado(callbacks: CallbacksEstado): () => void {
  let activo = true;
  let emisor: EventSource | null = null;
  let temporizadorReconexion: number | null = null;
  let temporizadorRespaldo: number | null = null;
  let intento = 0;

  const limpiarTemporizador = () => {
    if (temporizadorReconexion !== null) {
      window.clearTimeout(temporizadorReconexion);
      temporizadorReconexion = null;
    }
  };

  const programarReconexion = () => {
    if (!activo) return;
    limpiarTemporizador();
    const espera = Math.min(ESPERA_MAXIMA_MS, ESPERA_BASE_MS * 2 ** intento);
    intento += 1;
    temporizadorReconexion = window.setTimeout(conectar, espera);
  };

  const recibirSnapshot = (evento: MessageEvent<string>) => {
    try {
      const snapshot = JSON.parse(evento.data) as SnapshotEstado;
      intento = 0;
      callbacks.onSnapshot(snapshot);
      callbacks.onConexion(true);
      callbacks.onError(null);
    } catch {
      callbacks.onError('El snapshot recibido no tiene un formato válido');
    }
  };

  const iniciarRespaldo = () => {
    const consultar = async () => {
      if (!activo) return;
      try {
        const snapshot = (await obtenerSnapshot()) as SnapshotEstado;
        callbacks.onSnapshot(snapshot);
        callbacks.onConexion(true);
        callbacks.onError(null);
      } catch {
        callbacks.onConexion(false);
        callbacks.onError('No se pudo obtener el estado del backend');
      }
    };
    void consultar();
    temporizadorRespaldo = window.setInterval(() => void consultar(), INTERVALO_RESPALDO_MS);
  };

  function conectar() {
    if (!activo || typeof EventSource === 'undefined') return;
    limpiarTemporizador();
    emisor = new EventSource(`${API_BASE_URL}/stream/estado`);

    emisor.onopen = () => {
      intento = 0;
      callbacks.onConexion(true);
      callbacks.onError(null);
    };

    emisor.onmessage = recibirSnapshot;

    emisor.onerror = () => {
      if (!activo) return;
      callbacks.onConexion(false);
      callbacks.onError('Conexión en tiempo real perdida. Reconectando…');
      emisor?.close();
      emisor = null;
      programarReconexion();
    };
  }

  if (typeof EventSource === 'undefined') {
    iniciarRespaldo();
  } else {
    conectar();
  }

  return () => {
    activo = false;
    limpiarTemporizador();
    if (temporizadorRespaldo !== null) {
      window.clearInterval(temporizadorRespaldo);
    }
    emisor?.close();
    emisor = null;
  };
}
