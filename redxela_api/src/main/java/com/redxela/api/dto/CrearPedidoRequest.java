package com.redxela.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Petición de alta manual de un pedido en el sistema.
 */
public record CrearPedidoRequest(
        @NotBlank(message = "El clienteId es obligatorio")
        String clienteId,

        @NotEmpty(message = "El pedido debe contener al menos un artículo")
        List<@Valid ItemPedidoRequest> items
) {}
