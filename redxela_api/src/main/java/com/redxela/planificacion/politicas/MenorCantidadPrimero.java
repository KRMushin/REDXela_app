package com.redxela.planificacion.politicas;

import com.redxela.dominio.Pedido;
import com.redxela.planificacion.PoliticaPlanificacion;
import com.redxela.planificacion.TipoPolitica;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * Política de planificación por menor cantidad de artículos solicitados.
 * 
 * Concepto de SO: Planificación por Trabajo Más Corto Primero (SJF / Shortest Job First).
 * Minimiza el tiempo de espera promedio global del sistema al despachar primero
 * aquellas tareas que consumirán la menor ráfaga de CPU/procesamiento.
 */
@Component
public class MenorCantidadPrimero implements PoliticaPlanificacion {

    @Override
    public TipoPolitica getTipo() {
        return TipoPolitica.MENOR_CANTIDAD_PRIMERO;
    }

    @Override
    public Comparator<Pedido> obtenerComparador(long tiempoActualMs) {
        return Comparator
                .comparingInt(Pedido::getTotalArticulos)
                .thenComparingInt((Pedido p) -> p.getNivel().getNivel())
                .thenComparingLong(Pedido::getTCreacion)
                .thenComparing(Pedido::getId);
    }

    @Override
    public double calcularPrioridadEfectiva(Pedido pedido, long tiempoActualMs) {
        // En SJF, el tiempo estimado de ráfaga es directamente el número de artículos
        return pedido.getTotalArticulos();
    }

    @Override
    public String explicarPrioridad(Pedido pedido, long tiempoActualMs) {
        return String.format(
                "SJF: %d artículos solicitados (desempate: nivel %s, llegada %d ms)",
                pedido.getTotalArticulos(),
                pedido.getNivel().getNombre(),
                pedido.getTCreacion()
        );
    }
}
