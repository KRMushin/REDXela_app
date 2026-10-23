package com.redxela.interbloqueo;

import com.redxela.dominio.TipoRecurso;

import java.util.List;

/**
 * Reporte del estado de interbloqueo del sistema.
 * 
 * Concepto de SO: Diagnóstico de interbloqueo (deadlock report): identifica los
 * procesos involucrados, los recursos en disputa y la explicación de la espera circular.
 */
public record ReporteInterbloqueo(
        boolean hayInterbloqueo,
        List<String> pedidosInvolucrados,
        List<TipoRecurso> recursosEnDisputa,
        String explicacion
) {
    public ReporteInterbloqueo {
        pedidosInvolucrados = List.copyOf(pedidosInvolucrados);
        recursosEnDisputa = List.copyOf(recursosEnDisputa);
    }

    public static ReporteInterbloqueo sinInterbloqueo() {
        return new ReporteInterbloqueo(false, List.of(), List.of(), null);
    }
}
