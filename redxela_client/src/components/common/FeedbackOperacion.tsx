interface FeedbackOperacionProps {
  mensaje: string | null;
  error: string | null;
}

/** Mensaje visible de éxito o de rechazo (400/409) de una operación REST. */
function FeedbackOperacion({ mensaje, error }: FeedbackOperacionProps) {
  if (!mensaje && !error) return null;
  return (
    <div className="feedback" aria-live="polite">
      {error && <p className="feedback__mensaje feedback__mensaje--error">{error}</p>}
      {mensaje && <p className="feedback__mensaje feedback__mensaje--ok">{mensaje}</p>}
    </div>
  );
}

export default FeedbackOperacion;
