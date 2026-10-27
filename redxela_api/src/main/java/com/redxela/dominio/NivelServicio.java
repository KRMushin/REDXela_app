package com.redxela.dominio;

/**
 * Niveles de servicio y prioridad asociados al cliente y a sus pedidos.
 * 
 * Prioridad de procesos y contratos de tiempo límite (SLA / Real-Time Soft Deadline).
 * Base para los algoritmos de planificación preventiva y cálculo de envejecimiento (aging) contra inanición.
 */
public enum NivelServicio {
    EXPRES(1, "Exprés", 1),
    PRIORITARIO(2, "Prioritario", 10),
    ESTANDAR(3, "Estándar", 30),
    PROGRAMADO(4, "Programado", 60),
    ECONOMICO(5, "Económico", 120);

    private final int nivel;
    private final String nombre;
    private final long tiempoObjetivoMinutos;

    NivelServicio(int nivel, String nombre, long tiempoObjetivoMinutos) {
        this.nivel = nivel;
        this.nombre = nombre;
        this.tiempoObjetivoMinutos = tiempoObjetivoMinutos;
    }

    public int getNivel() {
        return nivel;
    }

    public String getNombre() {
        return nombre;
    }

    public long getTiempoObjetivoMinutos() {
        return tiempoObjetivoMinutos;
    }
}
