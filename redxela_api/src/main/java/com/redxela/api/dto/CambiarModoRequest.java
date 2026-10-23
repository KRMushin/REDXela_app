package com.redxela.api.dto;

import com.redxela.recursos.ModoPrevencion;
import jakarta.validation.constraints.NotNull;

/**
 * Petición para cambiar el modo de prevención de interbloqueos en caliente.
 */
public record CambiarModoRequest(
        @NotNull(message = "El modo de prevención es obligatorio")
        ModoPrevencion modo
) {}
