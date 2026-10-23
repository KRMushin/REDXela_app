import type { RecursoEstado } from '../../types/snapshot';
import EncabezadoPanel from '../common/EncabezadoPanel';
import BarraProgreso from '../common/BarraProgreso';
import { formatearNumero, porcentaje } from '../../utils/formato';

interface RecursosPanelProps {
  recursos: RecursoEstado[];
}

/** Panel 1: uso, disponibilidad y procesos asociados a cada recurso. */
function RecursosPanel({ recursos }: RecursosPanelProps) {
  return (
    <section className="panel">
      <EncabezadoPanel
        titulo="Recursos del sistema"
        descripcion="Semáforos y unidades compartidas; quién las retiene y quién espera."
        extra={<span className="contador">{recursos.length} recursos</span>}
      />
      <div className="recursos-grid">
        {recursos.map((recurso) => {
          const uso = porcentaje(recurso.enUso, recurso.total);
          return (
            <article className="recurso-card" key={recurso.tipo}>
              <div className="recurso-card__cabecera">
                <div>
                  <h4>{recurso.nombre}</h4>
                  <span className="texto-muted">{recurso.tipo}</span>
                </div>
                <strong className={uso >= 100 ? 'texto-peligro' : 'texto-ok'}>
                  {formatearNumero(uso, 0)}%
                </strong>
              </div>
              <BarraProgreso valor={uso} tono={uso >= 100 ? 'peligro' : 'normal'} />
              <p className="recurso-card__resumen">
                {recurso.disponibles} disponibles de {recurso.total} · {recurso.enUso} en uso
              </p>
              {recurso.retenidoPor !== undefined && (
                <p className="recurso-card__retenido">
                  Retenido por: <strong>{recurso.retenidoPor ?? 'libre'}</strong>
                </p>
              )}
              <div className="recurso-card__listas">
                <div>
                  <span className="mini-etiqueta">Retienen</span>
                  <div className="chips">
                    {recurso.pedidosAsignados.length === 0 && <span className="texto-muted">—</span>}
                    {recurso.pedidosAsignados.map((id) => (
                      <span className="chip chip--uso" key={id}>{id}</span>
                    ))}
                  </div>
                </div>
                <div>
                  <span className="mini-etiqueta">Esperan</span>
                  <div className="chips">
                    {recurso.pedidosEnEspera.length === 0 && <span className="texto-muted">—</span>}
                    {recurso.pedidosEnEspera.map((id) => (
                      <span className="chip chip--espera" key={id}>{id}</span>
                    ))}
                  </div>
                </div>
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}

export default RecursosPanel;
