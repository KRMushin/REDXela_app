package com.redxela.api.dto;

import com.redxela.planificacion.TipoPolitica;

/**
 * Respuesta con la política de planificación actualmente activa.
 */
public record PoliticaResponse(
        TipoPolitica politica,
        String nombreLegible,
        String mensaje
) {}
