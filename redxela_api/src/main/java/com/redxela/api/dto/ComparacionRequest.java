package com.redxela.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Parámetros opcionales para lanzar la comparación de políticas.
 *
 * Semilla del generador de carga sintética (reproducibility seed)
 * y tamaño del lote de procesos a comparar.
 */
public record ComparacionRequest(
        @Min(value = 1, message = "Debe comparar al menos un pedido")
        @Max(value = 1000, message = "El máximo es 1000 pedidos por comparación")
        Integer cantidadPedidos,

        Long semilla
) {}
