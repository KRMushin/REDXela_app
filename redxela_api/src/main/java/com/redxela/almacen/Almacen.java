package com.redxela.almacen;

import com.redxela.config.SimulacionProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Representación del espacio físico de almacenamiento del centro de distribución.
 * 
 * Concepto de SO: Espacio de Direccionamiento Físico de Memoria / Disco Físico (Physical Address Space).
 * Conjunto estructurado de celdas homogéneas donde se distribuyen los lotes entrantes.
 * 
 * Concurrencia: ReentrantReadWriteLock justo. Varios hilos pueden consultar el estado
 * en paralelo (lectores), pero la asignación de celdas es exclusiva (escritor), evitando
 * que dos lotes reciban la misma ubicación.
 */
@Component
public class Almacen {

    private final List<Ubicacion> ubicaciones = new ArrayList<>();
    private final int capacidadTotal;
    private final ReentrantReadWriteLock cerrojo = new ReentrantReadWriteLock(true);

    public Almacen(SimulacionProperties properties) {
        var ac = properties.almacen();
        int total = ac.totalUbicaciones() > 0 ? ac.totalUbicaciones() : 20;
        int capPorUbicacion = ac.capacidadPorUbicacion() > 0 ? ac.capacidadPorUbicacion() : 50;
        this.capacidadTotal = total * capPorUbicacion;

        // Distribución en pasillos (2), niveles (2) y posiciones variables
        int pasillo = 1;
        int nivel = 1;
        int posicion = 1;
        for (int i = 1; i <= total; i++) {
            String id = String.format("U-%02d", i);
            ubicaciones.add(new Ubicacion(id, pasillo, nivel, posicion, capPorUbicacion));
            posicion++;
            if (posicion > 5) {
                posicion = 1;
                nivel++;
                if (nivel > 2) {
                    nivel = 1;
                    pasillo++;
                }
            }
        }
    }

    /**
     * Ejecuta una consulta bajo el cerrojo de lectura (varios lectores simultáneos).
     */
    public <T> T enLectura(Supplier<T> accion) {
        cerrojo.readLock().lock();
        try {
            return accion.get();
        } finally {
            cerrojo.readLock().unlock();
        }
    }

    /**
     * Ejecuta una mutación bajo el cerrojo de escritura (un solo escritor exclusivo).
     */
    public <T> T enEscritura(Supplier<T> accion) {
        cerrojo.writeLock().lock();
        try {
            return accion.get();
        } finally {
            cerrojo.writeLock().unlock();
        }
    }

    /**
     * Acceso directo a las celdas vivas. Solo debe usarse dentro de {@link #enEscritura}.
     */
    List<Ubicacion> celdasInternas() {
        return ubicaciones;
    }

    /**
     * Espacio libre calculado sin volver a tomar el cerrojo (ya se posee el de escritura).
     */
    int espacioDisponibleInterno() {
        int ocupadas = ubicaciones.stream().mapToInt(Ubicacion::getUnidadesOcupadas).sum();
        return capacidadTotal - ocupadas;
    }

    public List<Ubicacion> obtenerUbicaciones() {
        return enLectura(() -> ubicaciones.stream().map(Ubicacion::copia).toList());
    }

    public Optional<Ubicacion> buscarPorId(String id) {
        return enLectura(() -> ubicaciones.stream()
                .filter(u -> u.getId().equals(id))
                .map(Ubicacion::copia)
                .findFirst());
    }

    public int getUnidadesOcupadas() {
        return enLectura(() -> capacidadTotal - espacioDisponibleInterno());
    }

    public int getEspacioDisponibleTotal() {
        return enLectura(this::espacioDisponibleInterno);
    }

    public int getCapacidadTotal() {
        return capacidadTotal;
    }

    public void limpiar() {
        enEscritura(() -> {
            ubicaciones.forEach(Ubicacion::vaciar);
            return null;
        });
    }
}
