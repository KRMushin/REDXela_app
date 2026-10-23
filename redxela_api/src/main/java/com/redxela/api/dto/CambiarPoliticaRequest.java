package com.redxela.api.dto;

import com.redxela.planificacion.TipoPolitica;
import jakarta.validation.constraints.NotNull;

/**
 * Petición para modificar la política de planificación activa en caliente.
 */
public record CambiarPoliticaRequest(
        @NotNull(message = "El tipo de política es obligatorio")
        TipoPolitica politica
) {}
