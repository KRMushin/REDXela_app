import { useEffect, useState } from 'react';
import type { SnapshotEstado } from '../types/snapshot';
import { conectarEstado } from '../api/estadoStream';
import { crearSnapshotSimulado } from '../api/mockSnapshot';

interface UseSnapshotResultado {
  snapshot: SnapshotEstado | null;
  conectado: boolean;
  error: string | null;
}

const MODO_MOCK = import.meta.env.VITE_MOCK_MODE === 'true';

/**
 * Única fuente de estado en vivo del tablero.
 * Se conecta al SSE, reconecta si cae y expone el último snapshot conocido.
 */
export function useSnapshot(): UseSnapshotResultado {
  const [snapshot, setSnapshot] = useState<SnapshotEstado | null>(null);
  const [conectado, setConectado] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (MODO_MOCK) {
      setSnapshot(crearSnapshotSimulado());
      setConectado(true);
      setError(null);
      const intervalo = window.setInterval(() => {
        setSnapshot(crearSnapshotSimulado());
      }, 1200);
      return () => window.clearInterval(intervalo);
    }

    return conectarEstado({
      onSnapshot: (estado) => {
        setSnapshot(estado);
        setError(null);
      },
      onConexion: setConectado,
      onError: setError,
    });
  }, []);

  return { snapshot, conectado, error };
}
