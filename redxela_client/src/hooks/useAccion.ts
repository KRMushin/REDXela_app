import { useCallback, useState } from 'react';
import { mensajeError } from '../utils/errores';

interface Accion<T extends unknown[]> {
  enviando: boolean;
  mensaje: string | null;
  error: string | null;
  ejecutar: (...args: T) => Promise<unknown | undefined>;
  limpiar: () => void;
}

/** Envuelve una acción REST con estado de envío y mensajes para el usuario. */
export function useAccion<T extends unknown[]>(
  accion: (...args: T) => Promise<unknown>,
  mensajeExito: string,
): Accion<T> {
  const [enviando, setEnviando] = useState(false);
  const [mensaje, setMensaje] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const ejecutar = useCallback(
    async (...args: T) => {
      setEnviando(true);
      setMensaje(null);
      setError(null);
      try {
        const resultado = await accion(...args);
        setMensaje(mensajeExito);
        return resultado;
      } catch (fallo) {
        setError(mensajeError(fallo));
        return undefined;
      } finally {
        setEnviando(false);
      }
    },
    [accion, mensajeExito],
  );

  const limpiar = useCallback(() => {
    setMensaje(null);
    setError(null);
  }, []);

  return { enviando, mensaje, error, ejecutar, limpiar };
}
