package com.redxela.recepcion;

/**
 * Mensaje inmutable que viaja del área de Recepción al área de Inventario.
 * 
 * Concepto de SO: Mensaje de comunicación entre procesos (IPC Message).
 * Recepción y Empaque son áreas independientes; no comparten estructuras internas,
 * solo intercambian este mensaje a través de un buzón acotado (bounded mailbox).
 */
public record MensajeLlegada(
        String loteId,
        String productoId,
        int cantidad,
        long tLlegada
) {
    public MensajeLlegada {
        if (loteId == null || loteId.isBlank()) {
            throw new IllegalArgumentException("El id del lote es obligatorio");
        }
        if (productoId == null || productoId.isBlank()) {
            throw new IllegalArgumentException("El id del producto es obligatorio");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad recibida debe ser mayor a cero");
        }
    }
}
