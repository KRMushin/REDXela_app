import { useState, type FormEvent } from 'react';
import type { Cliente, CrearClienteRequest } from '../../types/api';
import type { NivelServicio } from '../../types/snapshot';
import { crearCliente } from '../../api/redxelaApi';
import { useAccion } from '../../hooks/useAccion';
import { ETIQUETAS_NIVEL } from '../../utils/formato';
import FeedbackOperacion from '../common/FeedbackOperacion';

interface FormularioClienteProps {
  onCreado?: (cliente: Cliente) => void;
}

const NIVELES = Object.keys(ETIQUETAS_NIVEL) as NivelServicio[];

/** Alta de un cliente con su nivel de prioridad. */
function FormularioCliente({ onCreado }: FormularioClienteProps) {
  const [nombre, setNombre] = useState('');
  const [nivelServicio, setNivelServicio] = useState<NivelServicio>('ESTANDAR');
  const [errorValidacion, setErrorValidacion] = useState<string | null>(null);
  const { enviando, mensaje, error, ejecutar } = useAccion(
    crearCliente,
    'Cliente registrado correctamente.',
  );

  const enviar = async (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    if (!nombre.trim()) {
      setErrorValidacion('El nombre del cliente es obligatorio.');
      return;
    }
    setErrorValidacion(null);
    const cuerpo: CrearClienteRequest = { nombre: nombre.trim(), nivelServicio };
    const creado = (await ejecutar(cuerpo)) as Cliente | undefined;
    if (creado) {
      onCreado?.(creado);
      setNombre('');
    }
  };

  return (
    <form className="form-card" onSubmit={(evento) => void enviar(evento)}>
      <h4>Nuevo cliente</h4>
      <label className="campo">
        <span>Nombre</span>
        <input
          value={nombre}
          onChange={(evento) => {
            setNombre(evento.target.value);
            setErrorValidacion(null);
          }}
          placeholder="Tienda El Sol"
          maxLength={80}
        />
      </label>
      <label className="campo">
        <span>Nivel de servicio</span>
        <select
          value={nivelServicio}
          onChange={(evento) => setNivelServicio(evento.target.value as NivelServicio)}
        >
          {NIVELES.map((nivel) => (
            <option value={nivel} key={nivel}>{ETIQUETAS_NIVEL[nivel]}</option>
          ))}
        </select>
      </label>
      <button className="boton boton--primario" type="submit" disabled={enviando}>
        {enviando ? 'Registrando…' : 'Registrar cliente'}
      </button>
      <FeedbackOperacion mensaje={mensaje} error={errorValidacion ?? error} />
    </form>
  );
}

export default FormularioCliente;
