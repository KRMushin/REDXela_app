package com.redxela.dominio;

import com.redxela.procesamiento.PedidoFinalizadoEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reacción a la señal de terminación de un proceso.
 * 
 * Concepto de SO: Notificación de salida de proceso (SIGCHLD) y planificación a
 * corto plazo. Cuando un pedido termina, este componente intenta llenar el hueco
 * de CPU con el siguiente proceso de la cola de listos.
 */
@Component
public class DespachadorFinalizacion {

    private final PedidoService pedidoService;

    public DespachadorFinalizacion(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @EventListener
    public void alFinalizarPedido(PedidoFinalizadoEvent evento) {
        pedidoService.despacharSiCompletado(evento.pedidoId());
    }
}
