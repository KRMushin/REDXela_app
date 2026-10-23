package com.redxela.api.dto;

import com.redxela.dominio.NivelServicio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Petición de alta manual de un cliente.
 */
public record CrearClienteRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotNull(message = "El nivel de servicio es obligatorio")
        NivelServicio nivelServicio
) {}
