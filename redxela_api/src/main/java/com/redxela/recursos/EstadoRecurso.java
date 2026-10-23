package com.redxela.recursos;

import com.redxela.dominio.TipoRecurso;

import java.util.List;

/**
 * Snapshot inmutable del estado actual de un pool de recursos.
 * 
 * Concepto de SO: Vector de recursos disponibles y matriz de asignación/espera
 * del estado del sistema (similar a las estructuras del algoritmo del banquero de Dijkstra).
 */
public record EstadoRecurso(
        TipoRecurso tipo,
        String nombre,
        int total,
        int disponibles,
        int enUso,
        List<String> pedidosAsignados,
        List<String> pedidosEnEspera
) {
    public EstadoRecurso {
        pedidosAsignados = List.copyOf(pedidosAsignados);
        pedidosEnEspera = List.copyOf(pedidosEnEspera);
    }
}
