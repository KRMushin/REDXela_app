package com.redxela.almacen;

/**
 * Detalle de asignación de una porción de lote a una celda física.
 * 
 * Concepto de SO: Entrada en la Tabla de Páginas / Puntero de Bloque Directo (Page Table Entry / Block Pointer).
 */
public record AsignacionCelda(
        String ubicacionId,
        int pasillo,
        int nivel,
        int posicion,
        int unidades
) {}
