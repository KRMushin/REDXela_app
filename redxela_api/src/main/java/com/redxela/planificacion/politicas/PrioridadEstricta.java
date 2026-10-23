package com.redxela.planificacion.politicas;

import com.redxela.dominio.Pedido;
import com.redxela.planificacion.PoliticaPlanificacion;
import com.redxela.planificacion.TipoPolitica;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * Política de planificación por prioridad estricta sin envejecimiento.
 * 
 * Concepto de SO: Planificación por Prioridad Estricta (Priority Scheduling).
 * Los procesos con mayor nivel de prioridad se despachan invariablemente antes
 * que los de menor prioridad. Los empates se resuelven bajo política FIFO (tCreacion).
 * Puede provocar inanición (starvation) indefinida en niveles bajos ante alta carga.
 */
@Component
public class PrioridadEstricta implements PoliticaPlanificacion {

    @Override
    public TipoPolitica getTipo() {
        return TipoPolitica.PRIORIDAD_ESTRICTA;
    }

    @Override
    public Comparator<Pedido> obtenerComparador(long tiempoActualMs) {
        return Comparator
                .comparingInt((Pedido p) -> p.getNivel().getNivel())
                .thenComparingLong(Pedido::getTCreacion)
                .thenComparing(Pedido::getId);
    }

    @Override
    public double calcularPrioridadEfectiva(Pedido pedido, long tiempoActualMs) {
        return pedido.getNivel().getNivel();
    }

    @Override
    public String explicarPrioridad(Pedido pedido, long tiempoActualMs) {
        return String.format(
                "Nivel %s (prioridad estricta %d). Orden de llegada: %d ms",
                pedido.getNivel().getNombre(),
                pedido.getNivel().getNivel(),
                pedido.getTCreacion()
        );
    }
}
