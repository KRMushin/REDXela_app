package com.redxela.dominio;

import java.util.List;

/**
 * Clasificación de la mercancía con la secuencia de recursos requeridos para su procesamiento.
 * 
 * Concepto de SO: Grafo de solicitud y asignación de recursos (RAG).
 * Define el orden cronológico natural de solicitud de recursos. La divergencia de orden
 * entre 'Pesado' (Montacargas -> Área de carga) y 'Voluminoso' (Área de carga -> Montacargas)
 * introduce deliberadamente la condición de espera circular para interbloqueos.
 */
public enum TipoMercancia {
    FRAGIL(
            "Frágil",
            "Artículos delicados que requieren inspección minuciosa y empaque protector",
            List.of(TipoRecurso.ENCARGADO_BODEGA, TipoRecurso.CONTROL_CALIDAD, TipoRecurso.ESTACION_EMPAQUE)
    ),
    PESADO(
            "Pesado",
            "Carga pesada que requiere maquinaria para su traslado y estiba",
            List.of(TipoRecurso.MONTACARGAS, TipoRecurso.ENCARGADO_BODEGA, TipoRecurso.AREA_CARGA)
    ),
    VOLUMINOSO(
            "Voluminoso",
            "Mercancía de gran tamaño que ocupa bahía de carga antes de su montaje",
            List.of(TipoRecurso.AREA_CARGA, TipoRecurso.MONTACARGAS, TipoRecurso.ESTACION_EMPAQUE)
    ),
    ELECTRONICO(
            "Electrónico",
            "Dispositivos sensibles que requieren verificación técnica y escaneo",
            List.of(TipoRecurso.CONTROL_CALIDAD, TipoRecurso.ESCANER, TipoRecurso.ESTACION_EMPAQUE)
    ),
    PERECEDERO(
            "Perecedero",
            "Bienes con vida útil corta que requieren despacho rápido hacia transporte",
            List.of(TipoRecurso.ENCARGADO_BODEGA, TipoRecurso.ESTACION_EMPAQUE, TipoRecurso.AREA_CARGA)
    ),
    GENERAL(
            "General",
            "Mercancía estándar de fácil manipulación y registro general",
            List.of(TipoRecurso.ENCARGADO_BODEGA, TipoRecurso.ESTACION_EMPAQUE, TipoRecurso.ESCANER)
    );

    private final String nombre;
    private final String descripcion;
    private final List<TipoRecurso> recursosRequeridos;

    TipoMercancia(String nombre, String descripcion, List<TipoRecurso> recursosRequeridos) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.recursosRequeridos = recursosRequeridos;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public List<TipoRecurso> getRecursosRequeridos() {
        return recursosRequeridos;
    }
}
