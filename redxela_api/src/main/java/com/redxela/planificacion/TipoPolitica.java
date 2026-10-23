package com.redxela.planificacion;

/**
 * Políticas de planificación admitidas por el centro de distribución REDXela.
 * 
 * Concepto de SO: Algoritmos de planificación de la CPU (Scheduling Algorithms).
 * Permiten contrastar el comportamiento del sistema bajo distintos criterios
 * de equidad, prioridad y rendimiento.
 */
public enum TipoPolitica {
    /** Planifica estrictamente por el nivel de servicio del cliente (1 a 5), desempate FIFO. */
    PRIORIDAD_ESTRICTA("Prioridad Estricta"),

    /** Ajusta dinámicamente la prioridad según el tiempo en espera para evitar inanición (Aging). */
    PRIORIDAD_CON_ENVEJECIMIENTO("Prioridad con Envejecimiento"),

    /** Atiende primero los pedidos con menor cantidad total de artículos (SJF / Shortest Job First). */
    MENOR_CANTIDAD_PRIMERO("Menor Cantidad Primero");

    private final String nombreLegible;

    TipoPolitica(String nombreLegible) {
        this.nombreLegible = nombreLegible;
    }

    public String getNombreLegible() {
        return nombreLegible;
    }
}
