import { useCallback, useEffect, useState } from 'react';
import type { Cliente, Producto } from '../types/api';
import { listarClientes, obtenerCatalogo } from '../api/redxelaApi';
import { clientesMock, productosMock } from '../api/mockDatosReferencia';
import { mensajeError } from '../utils/errores';

const MODO_MOCK = import.meta.env.VITE_MOCK_MODE === 'true';

interface DatosReferencia {
  productos: Producto[];
  clientes: Cliente[];
  cargando: boolean;
  error: string | null;
  recargar: () => Promise<void>;
  agregarCliente: (cliente: Cliente) => void;
}

/** Catálogo y clientes que alimentan los formularios; no forman parte del snapshot. */
export function useDatosReferencia(): DatosReferencia {
  const [productos, setProductos] = useState<Producto[]>([]);
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const recargar = useCallback(async () => {
    setCargando(true);
    setError(null);
    try {
      if (MODO_MOCK) {
        setProductos(productosMock);
        setClientes(clientesMock);
        return;
      }
      const [catalogo, listado] = await Promise.all([obtenerCatalogo(), listarClientes()]);
      setProductos(catalogo as Producto[]);
      setClientes(listado as Cliente[]);
    } catch (fallo) {
      setError(mensajeError(fallo));
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    void recargar();
  }, [recargar]);

  const agregarCliente = useCallback((cliente: Cliente) => {
    setClientes((actuales) => [
      ...actuales.filter((item) => item.id !== cliente.id),
      cliente,
    ]);
  }, []);

  return { productos, clientes, cargando, error, recargar, agregarCliente };
}
