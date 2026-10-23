package com.redxela.almacen;

import java.util.List;

/**
 * Resultado de intentar almacenar un lote completo.
 * 
 * Concepto de SO: Resultado de una llamada de asignación de memoria (allocation result):
 * indica cuántos bloques se entregaron y cuánto espacio quedó pendiente por falta
 * de marcos libres (Out Of Memory parcial).
 */
public record ResultadoAsignacion(
        List<AsignacionCelda> asignaciones,
        int unidadesPendientes
) {
    public ResultadoAsignacion {
        asignaciones = List.copyOf(asignaciones);
        if (unidadesPendientes < 0) {
            throw new IllegalArgumentException("Las unidades pendientes no pueden ser negativas");
        }
    }

    public int unidadesAsignadas() {
        return asignaciones.stream().mapToInt(AsignacionCelda::unidades).sum();
    }

    public boolean completo() {
        return unidadesPendientes == 0;
    }
}
