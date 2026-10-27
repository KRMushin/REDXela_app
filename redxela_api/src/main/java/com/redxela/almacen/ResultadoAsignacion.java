package com.redxela.almacen;

import java.util.List;

/**
 * Resultado de intentar almacenar un lote completo.
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
