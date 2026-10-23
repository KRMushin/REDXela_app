package com.redxela.dominio;

/**
 * Representación de un producto en el catálogo del centro de distribución.
 * 
 * Concepto de SO: Identificador de tipo de tarea o dato dentro del sistema de archivos o memoria.
 * Cada producto está invariablemente ligado a un tipo de mercancía y su perfil de consumo de recursos.
 */
public record Producto(
        String id,
        String nombre,
        TipoMercancia tipoMercancia,
        String descripcion
) {
    public Producto {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del producto no puede estar vacío");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del producto no puede estar vacío");
        }
        if (tipoMercancia == null) {
            throw new IllegalArgumentException("El tipo de mercancía es obligatorio");
        }
    }
}
