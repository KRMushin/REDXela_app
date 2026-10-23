import type { Cliente, Producto } from '../../types/api';
import EncabezadoPanel from '../common/EncabezadoPanel';
import FormularioCliente from './FormularioCliente';
import FormularioPedido from './FormularioPedido';
import FormularioMercancia from './FormularioMercancia';

interface FormulariosPanelProps {
  clientes: Cliente[];
  productos: Producto[];
  cargando: boolean;
  error: string | null;
  onClienteCreado: (cliente: Cliente) => void;
}

/** Panel 5: alta de clientes, pedidos manuales y llegada de mercadería. */
function FormulariosPanel({
  clientes,
  productos,
  cargando,
  error,
  onClienteCreado,
}: FormulariosPanelProps) {
  return (
    <section className="panel">
      <EncabezadoPanel
        titulo="Operaciones manuales"
        descripcion="Inyección de trabajo y abastecimiento sin esperar al generador automático."
        extra={cargando ? <span className="contador">Cargando catálogo…</span> : undefined}
      />
      {error && <p className="feedback__mensaje feedback__mensaje--error">{error}</p>}
      <div className="formularios-grid">
        <FormularioCliente onCreado={onClienteCreado} />
        <FormularioPedido clientes={clientes} productos={productos} />
        <FormularioMercancia productos={productos} />
      </div>
    </section>
  );
}

export default FormulariosPanel;
