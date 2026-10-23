package com.redxela.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Petición para registrar manualmente la llegada de mercadería al almacén.
 */
public record RegistrarLlegadaRequest(
        @NotBlank(message = "El id del producto es obligatorio")
        String productoId,

        @Positive(message = "La cantidad debe ser mayor a cero")
        int cantidad
) {}
