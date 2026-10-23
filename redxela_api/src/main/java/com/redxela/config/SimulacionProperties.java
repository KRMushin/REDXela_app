package com.redxela.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuración de la simulación del centro de distribución REDXela.
 * 
 * Concepto de SO: Parámetros del sistema operativo (tabla de límites del kernel)
 * que definen la cantidad finita de recursos del sistema, el tamaño de los búferes
 * de comunicación y la tasa de llegada de procesos.
 */
@ConfigurationProperties(prefix = "simulacion")
public record SimulacionProperties(
        long timeScaleMs,
        int capacidadColaRecepcion,
        int tasaGeneradorPedidosMinuto,
        RecursosConfig recursos,
        AlmacenConfig almacen,
        InventarioConfig inventario,
        long deteccionInterbloqueoMs,
        long publicacionEstadoMs
) {

    public record RecursosConfig(
            int estacionesEmpaque,
            int areasCarga,
            int encargadosBodega,
            int montacargas,
            int controlCalidad,
            int escaner
    ) {}

    public record AlmacenConfig(
            int totalUbicaciones,
            int capacidadPorUbicacion
    ) {}

    /**
     * Existencias con las que arranca el inventario al inicializar el sistema.
     * Concepto de SO: contenido inicial del área de intercambio (swap) o del
     * almacenamiento secundario antes de recibir nueva carga.
     */
    public record InventarioConfig(
            int stockInicialPorProducto
    ) {}
}
