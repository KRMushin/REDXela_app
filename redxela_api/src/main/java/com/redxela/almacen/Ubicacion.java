package com.redxela.almacen;

/**
 * Celda física elemental de almacenamiento en el centro de distribución.
 * 
 * Concepto de SO: Marco de Página / Bloque de Almacenamiento Secundario (Page Frame / Disk Block).
 * Unidad de asignación de tamaño fijo identificada por coordenadas discretas (pasillo, nivel, posición).
 * 
 * Concurrencia: esta clase no usa cerrojos propios. Su estado se modifica únicamente
 * mientras se retiene el ReentrantReadWriteLock de {@link Almacen}, de modo que la
 * exclusión mutua queda centralizada en un solo punto (menos cerrojos, menos deadlocks).
 */
public class Ubicacion {

    private final String id;
    private final int pasillo;
    private final int nivel;
    private final int posicion;
    private final int capacidad;

    private int unidadesOcupadas = 0;
    private String productoId = null;
    private String loteId = null;

    public Ubicacion(String id, int pasillo, int nivel, int posicion, int capacidad) {
        this.id = id;
        this.pasillo = pasillo;
        this.nivel = nivel;
        this.posicion = posicion;
        this.capacidad = capacidad;
    }

    /**
     * Indica si la celda puede recibir unidades de un lote.
     * Una celda aloja un solo lote (evita mezclar mercancía y conserva la trazabilidad).
     */
    public boolean puedeAlmacenar(String loteId) {
        return unidadesOcupadas == 0 || loteId.equals(this.loteId);
    }

    public int getEspacioDisponible() {
        return capacidad - unidadesOcupadas;
    }

    public boolean estaVacia() {
        return unidadesOcupadas == 0;
    }

    /**
     * Guarda la mayor cantidad posible del lote y devuelve cuántas unidades aceptó.
     * Debe invocarse con el cerrojo de escritura de {@link Almacen} retenido.
     */
    public int almacenar(String loteId, String productoId, int cantidad) {
        int aGuardar = Math.min(getEspacioDisponible(), cantidad);
        if (aGuardar > 0) {
            this.loteId = loteId;
            this.productoId = productoId;
            this.unidadesOcupadas += aGuardar;
        }
        return aGuardar;
    }

    public void vaciar() {
        this.unidadesOcupadas = 0;
        this.productoId = null;
        this.loteId = null;
    }

    /**
     * Copia inmutable para publicar una foto del almacén sin exponer el estado vivo.
     */
    public Ubicacion copia() {
        Ubicacion copia = new Ubicacion(id, pasillo, nivel, posicion, capacidad);
        copia.unidadesOcupadas = unidadesOcupadas;
        copia.productoId = productoId;
        copia.loteId = loteId;
        return copia;
    }

    public String getId() { return id; }
    public int getPasillo() { return pasillo; }
    public int getNivel() { return nivel; }
    public int getPosicion() { return posicion; }
    public int getCapacidad() { return capacidad; }
    public int getUnidadesOcupadas() { return unidadesOcupadas; }
    public String getProductoId() { return productoId; }
    public String getLoteId() { return loteId; }
}
