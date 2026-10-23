package com.redxela.api.dto;

import jakarta.validation.constraints.Positive;

/**
 * Petición para modificar la tasa de pedidos por minuto del generador.
 */
public record AjustarTasaRequest(
        @Positive(message = "La tasa debe ser mayor a cero")
        int pedidosPorMinuto
) {}
