package com.redxela.dominio;

/**
 * Estados del ciclo de vida de un pedido en la simulación.
 * 
 * Modelo de estados de un proceso (New, Ready, Running, Waiting/Blocked, Terminated).
 * Representa la transición de un proceso a través de los planificadores del sistema operativo.
 */
public enum EstadoPedido {
    /** El pedido espera a que exista stock disponible en el almacén (bloqueo lógico/E/S). */
    EN_ESPERA_STOCK,

    /** El pedido tiene stock asignado pero espera turnos o recursos físicos (cola de listos). */
    EN_ESPERA_RECURSOS,

    /** El pedido está en ejecución activa siendo procesado por los recursos asignados (running). */
    EN_PROCESO,

    /** El pedido retiene al menos un recurso pero se encuentra suspendido esperando otro (bloqueado/espera circular). */
    BLOQUEADO,

    /** El pedido completó todas sus etapas de empaque y despacho exitosamente (terminado). */
    COMPLETADO
}
