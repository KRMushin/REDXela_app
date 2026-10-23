package com.redxela.api.dto;

import com.redxela.dominio.NivelServicio;
import com.redxela.dominio.TipoRecurso;

import java.util.List;

/**
 * Métricas detalladas del centro de distribución expuestas para análisis.
 *
 * Concepto de SO: Contabilidad del sistema (Accounting) desagregada por clase de
 * prioridad y por recurso, sin modificar el snapshot de telemetría en vivo.
 */
public record EstadisticasSistema(
        int pedidosCompletados,
        double tiempoEsperaPromedioMinutos,
        double tiempoProcesamientoPromedioMinutos,
        int pedidosDentroSla,
        double cumplimientoSlaPorcentaje,
        double tasaThroughputPorMinuto,
        List<MetricaNivel> porNivel,
        List<OcupacionRecurso> ocupacionRecursos
) {

    public record MetricaNivel(
            NivelServicio nivel,
            String nombre,
            int pedidosCompletados,
            double tiempoEsperaPromedioMinutos,
            double tiempoProcesamientoPromedioMinutos,
            long tiempoObjetivoMinutos,
            int pedidosDentroSla,
            double cumplimientoSlaPorcentaje
    ) {}

    public record OcupacionRecurso(
            TipoRecurso tipo,
            String nombre,
            int total,
            int disponibles,
            int enUso,
            double ocupacionPorcentaje
    ) {}
}
