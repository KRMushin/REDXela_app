package com.redxela.api.dto;

import com.redxela.dominio.NivelServicio;
import com.redxela.planificacion.TipoPolitica;

import java.util.List;

/**
 * Tabla comparativa de las políticas de planificación bajo una carga idéntica.
 *
 * Concepto de SO: Comparación de algoritmos de planificación (Scheduler Benchmark)
 * ejecutada sobre una réplica aislada del sistema, sin alterar la simulación viva.
 */
public record ComparacionPoliticas(
        long semilla,
        int cantidadPedidos,
        long generadoEn,
        List<ResultadoPolitica> resultados
) {

    public record ResultadoPolitica(
            TipoPolitica politica,
            String nombreLegible,
            int pedidosCompletados,
            double tiempoEsperaPromedioMinutos,
            double esperaMaximaMinutos,
            int pedidosDentroSla,
            double cumplimientoSlaPorcentaje,
            double tasaThroughputPorMinuto,
            List<MetricaNivel> porNivel
    ) {}

    public record MetricaNivel(
            NivelServicio nivel,
            String nombre,
            int pedidos,
            int pedidosDentroSla,
            long tiempoObjetivoMinutos,
            double tiempoEsperaPromedioMinutos,
            double esperaMaximaMinutos,
            double cumplimientoSlaPorcentaje
    ) {}
}
