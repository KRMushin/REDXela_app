package com.redxela.dominio;

/**
 * Recursos compartidos finitos del centro de distribución REDXela.
 * 
 * Concepto de SO: Recursos del sistema gestionados por el kernel (unidades finitas de CPU,
 * dispositivos de E/S, canales DMA). Cada recurso tiene un rango global para prevención
 * de interbloqueos mediante jerarquía de recursos (condición de espera circular de Coffman/Havender).
 */
public enum TipoRecurso {
    ENCARGADO_BODEGA("Encargado de Bodega", 1, 10),
    MONTACARGAS("Montacargas", 2, 3),
    AREA_CARGA("Área de Carga", 3, 2),
    CONTROL_CALIDAD("Control de Calidad", 4, 4),
    ESTACION_EMPAQUE("Estación de Empaque", 5, 6),
    ESCANER("Sistema de Escaneo", 6, 1);

    private final String nombre;
    private final int ordenGlobal;
    private final int unidadesPorDefecto;

    TipoRecurso(String nombre, int ordenGlobal, int unidadesPorDefecto) {
        this.nombre = nombre;
        this.ordenGlobal = ordenGlobal;
        this.unidadesPorDefecto = unidadesPorDefecto;
    }

    public String getNombre() {
        return nombre;
    }

    public int getOrdenGlobal() {
        return ordenGlobal;
    }

    public int getUnidadesPorDefecto() {
        return unidadesPorDefecto;
    }
}
