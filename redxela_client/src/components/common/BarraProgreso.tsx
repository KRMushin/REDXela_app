interface BarraProgresoProps {
  valor: number;
  tono?: 'normal' | 'alerta' | 'peligro';
}

/** Barra de uso o avance con accesibilidad básica. */
function BarraProgreso({ valor, tono = 'normal' }: BarraProgresoProps) {
  const porcentaje = Math.max(0, Math.min(100, valor));
  return (
    <div
      className="barra-progreso"
      role="progressbar"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(porcentaje)}
    >
      <span
        className={`barra-progreso__relleno barra-progreso__relleno--${tono}`}
        style={{ width: `${porcentaje}%` }}
      />
    </div>
  );
}

export default BarraProgreso;
