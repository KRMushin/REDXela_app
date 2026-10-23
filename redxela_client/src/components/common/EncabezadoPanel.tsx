import type { ReactNode } from 'react';

interface EncabezadoPanelProps {
  titulo: string;
  descripcion?: string;
  extra?: ReactNode;
}

/** Encabezado homogéneo para todos los paneles del tablero. */
function EncabezadoPanel({ titulo, descripcion, extra }: EncabezadoPanelProps) {
  return (
    <header className="panel__encabezado">
      <div>
        <h3 className="panel__titulo">{titulo}</h3>
        {descripcion && <p className="panel__descripcion">{descripcion}</p>}
      </div>
      {extra && <div className="panel__extra">{extra}</div>}
    </header>
  );
}

export default EncabezadoPanel;
