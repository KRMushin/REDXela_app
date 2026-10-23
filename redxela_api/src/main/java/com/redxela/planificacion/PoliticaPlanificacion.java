package com.redxela.planificacion;

import com.redxela.dominio.Pedido;

import java.util.Comparator;

/**
 * Estrategia de planificación para ordenar la cola de pedidos listos.
 * 
 * Concepto de SO: Patrón Strategy para Algoritmos de Planificación de Procesos (CPU Scheduler).
 * Cada implementación materializa una función de decisión de prioridad para seleccionar
 * el próximo proceso a despachar desde la cola de preparados/listos.
 */
public interface PoliticaPlanificacion {

    /**
     * Retorna el tipo de política representado.
     */
    TipoPolitica getTipo();

    /**
     * Genera el comparador ordenado para los pedidos considerando el tiempo actual.
     */
    Comparator<Pedido> obtenerComparador(long tiempoActualMs);

    /**
     * Calcula la métrica numérica de prioridad (menor valor = mayor prioridad de atención).
     */
    double calcularPrioridadEfectiva(Pedido pedido, long tiempoActualMs);

    /**
     * Retorna una explicación legible del porqué de la posición del pedido en la cola.
     */
    String explicarPrioridad(Pedido pedido, long tiempoActualMs);
}
