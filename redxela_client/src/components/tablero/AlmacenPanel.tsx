import type { AlmacenEstado } from '../../types/snapshot';
import EncabezadoPanel from '../common/EncabezadoPanel';
import BarraProgreso from '../common/BarraProgreso';
import { colorLote } from '../../utils/colores';
import { formatearNumero, porcentaje } from '../../utils/formato';

interface AlmacenPanelProps {
  almacen: AlmacenEstado;
}

/** Panel 6: mapa de ubicaciones y estado del buzón Recepción → Inventario. */
function AlmacenPanel({ almacen }: AlmacenPanelProps) {
  const ubicaciones = [...almacen.ubicaciones].sort(
    (a, b) => a.pasillo - b.pasillo || a.nivel - b.nivel || a.posicion - b.posicion,
  );
  const lotes = [...new Set(ubicaciones.map((ubicacion) => ubicacion.loteId))];
  const ocupacion = porcentaje(almacen.unidadesOcupadas, almacen.capacidadTotal);

  return (
    <section className="panel">
      <EncabezadoPanel
        titulo="Almacén"
        descripcion="Cada color representa un lote; las celdas muestran producto y unidades."
        extra={<span className="contador">{ubicaciones.length} ubicaciones</span>}
      />
      <div className="almacen-resumen">
        <div>
          <span className="mini-etiqueta">Ocupación general</span>
          <BarraProgreso valor={ocupacion} />
          <span className="texto-muted">
            {almacen.unidadesOcupadas} / {almacen.capacidadTotal} unidades ({formatearNumero(ocupacion, 0)}%)
          </span>
        </div>
        <div>
          <span className="mini-etiqueta">Cola Recepción → Inventario</span>
          <BarraProgreso
            valor={porcentaje(almacen.colaRecepcion.elementosEnCola, almacen.colaRecepcion.capacidadMaxima)}
            tono={almacen.colaRecepcion.elementosEnCola >= almacen.colaRecepcion.capacidadMaxima ? 'peligro' : 'alerta'}
          />
          <span className="texto-muted">
            {almacen.colaRecepcion.elementosEnCola} en cola · capacidad {almacen.colaRecepcion.capacidadMaxima}
          </span>
        </div>
      </div>
      <div className="almacen-grid">
        {ubicaciones.map((ubicacion) => {
          const color = colorLote(ubicacion.loteId);
          return (
            <article
              className="ubicacion"
              key={ubicacion.id}
              style={{
                backgroundColor: `${color}24`,
                borderColor: `${color}99`,
              }}
              title={`${ubicacion.loteId} · ${ubicacion.productoNombre}`}
            >
              <div className="ubicacion__cabecera">
                <strong>{ubicacion.id}</strong>
                <span>{ubicacion.unidades}/{ubicacion.capacidad}</span>
              </div>
              <span className="ubicacion__producto">{ubicacion.productoNombre}</span>
              <span className="ubicacion__lote" style={{ color }}>{ubicacion.loteId}</span>
            </article>
          );
        })}
      </div>
      <div className="leyenda">
        {lotes.map((lote) => (
          <span className="leyenda__item" key={lote}>
            <span className="leyenda__color" style={{ backgroundColor: colorLote(lote) }} />
            {lote}
          </span>
        ))}
      </div>
    </section>
  );
}

export default AlmacenPanel;
