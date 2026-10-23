package com.redxela.inventario;

/**
 * Evento publicado cuando Inventario termina de registrar un lote.
 * 
 * Concepto de SO: Señal/evento de sincronización entre procesos (signal / condition wake-up).
 * Despierta al planificador para reevaluar los pedidos que esperaban existencias de ese
 * producto, sin que Inventario conozca la estructura interna de la lista de espera.
 */
public record EventoLlegadaMercancia(
        String productoId,
        int cantidadDisponible,
        String loteId
) {
    public EventoLlegadaMercancia {
        if (productoId == null || productoId.isBlank()) {
            throw new IllegalArgumentException("El id del producto es obligatorio");
        }
        if (cantidadDisponible < 0) {
            throw new IllegalArgumentException("La cantidad disponible no puede ser negativa");
        }
    }
}
