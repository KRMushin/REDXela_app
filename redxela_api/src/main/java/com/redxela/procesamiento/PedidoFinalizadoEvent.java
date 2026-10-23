package com.redxela.procesamiento;

/**
 * Evento publicado cuando un pedido termina su ejecución (exitosa o expropiada).
 * 
 * Concepto de SO: Señal de terminación de proceso (SIGCHLD / process exit notification).
 * Permite que el planificador vuelva a llenar la CPU con el siguiente proceso de la
 * cola de listos sin esperar a que llegue una solicitud nueva.
 */
public record PedidoFinalizadoEvent(String pedidoId) {}
