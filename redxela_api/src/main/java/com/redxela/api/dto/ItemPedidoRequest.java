package com.redxela.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Detalle de artículo en la solicitud de creación de un pedido.
 */
public record ItemPedidoRequest(
        @NotBlank(message = "El id del producto es obligatorio")
        String productoId,

        @Positive(message = "La cantidad debe ser mayor a cero")
        int cantidad
) {}
