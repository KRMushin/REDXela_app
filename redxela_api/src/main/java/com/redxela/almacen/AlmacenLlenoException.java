package com.redxela.almacen;

/**
 * Excepción lanzada cuando el almacén no dispone de celdas libres suficientes.
 * Fallo de Asignación de Memoria / Espacio en Disco Insuficiente (Out Of Memory / ENOSPC).
 */
public class AlmacenLlenoException extends RuntimeException {

    private final int unidadesPendientes;

    public AlmacenLlenoException(String mensaje, int unidadesPendientes) {
        super(mensaje);
        this.unidadesPendientes = unidadesPendientes;
    }

    public int getUnidadesPendientes() {
        return unidadesPendientes;
    }
}
