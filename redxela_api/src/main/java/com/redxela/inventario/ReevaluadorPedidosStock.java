package com.redxela.inventario;

import com.redxela.dominio.PedidoService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Escucha la llegada de mercadería y reevalúa los pedidos detenidos por falta de stock.
 * 
 * Concepto de SO: Despertar de procesos bloqueados por una señal (wake-up / condition signal).
 * Inventario publica un evento y este componente, sin acoplarse a sus estructuras internas,
 * ordena al planificador reintentar los pedidos en estado EN_ESPERA_STOCK.
 */
@Component
public class ReevaluadorPedidosStock {

    private final PedidoService pedidoService;

    public ReevaluadorPedidosStock(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @EventListener
    public void alLlegarMercancia(EventoLlegadaMercancia evento) {
        pedidoService.reintentarPedidosEnEsperaStock();
    }
}
