package com.redxela.dominio;

/**
 * Representa al usuario o entidad que solicita pedidos al centro de distribución.
 * 
 * Identificador de usuario/propietario del proceso (UID), que define
 * la clase de prioridad y el privilegio de atención asignado por el planificador del sistema.
 */
public record Cliente(
        String id,
        String nombre,
        NivelServicio nivelServicio
) {
    public Cliente {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del cliente no puede estar vacío");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del cliente no puede estar vacío");
        }
        if (nivelServicio == null) {
            throw new IllegalArgumentException("El nivel de servicio es obligatorio");
        }
    }
}
