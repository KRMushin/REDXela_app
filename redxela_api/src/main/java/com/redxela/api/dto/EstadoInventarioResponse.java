package com.redxela.api.dto;

import java.util.Map;

/**
 * Snapshot del estado del inventario y del buzón Recepción → Inventario.
 * 
 * Concepto de SO: Lectura consistente del estado de memoria compartida
 * (existencias) y de la ocupación del búfer de comunicación entre procesos.
 */
public record EstadoInventarioResponse(
        Map<String, Integer> existencias,
        int cantidadLotes,
        int unidadesPendientes,
        int elementosEnCola,
        int capacidadCola,
        String ultimoError
) {}
