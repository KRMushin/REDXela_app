package com.redxela.planificacion;

import com.redxela.dominio.NivelServicio;

/**
 * Representación inmutable de la posición y justificación de un proceso en la cola de listos.
 * 
 * Concepto de SO: Entrada en la Cola de Procesos Listos (Ready Queue Entry) con telemetría
 * de planificación y auditoría de la decisión del despachador.
 */
public record ElementoListaEspera(
        int posicion,
        String pedidoId,
        String clienteNombre,
        NivelServicio nivelServicio,
        int totalArticulos,
        long tCreacion,
        double tiempoEsperaMinutos,
        double prioridadCalculada,
        String motivoPosicion
) {}
