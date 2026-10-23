package com.redxela.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Petición para resolver manualmente un interbloqueo indicando qué pedido continúa.
 */
public record ResolverInterbloqueoRequest(
        @NotBlank(message = "El id del pedido ganador es obligatorio")
        String pedidoGanadorId
) {}
