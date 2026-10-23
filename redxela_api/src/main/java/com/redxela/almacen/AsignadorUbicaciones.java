package com.redxela.almacen;

import com.redxela.dominio.Producto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de asignación no contigua de almacenamiento para lotes entrantes.
 * 
 * Concepto de SO: Asignación No Contigua de Memoria / Paginación e Indexación (Paging & Non-contiguous Allocation).
 * Divide un lote de tamaño arbitrario en bloques de tamaño celda (marcos de página) y los dispersa
 * en ubicaciones físicas libres, manteniendo la tabla de traducción lote → celdas (Page Table).
 * 
 * La búsqueda de celdas es "next-fit circular con paso coprimo": recorre todas las celdas una sola
 * vez desde un inicio pseudoaleatorio, pero salta de forma dispersa, por lo que un lote grande
 * termina repartido en pasillos o niveles no adyacentes (fragmentación externa real del almacén).
 * Toda la asignación corre bajo el cerrojo de escritura de {@link Almacen}.
 */
@Component
public class AsignadorUbicaciones {

    private final Almacen almacen;
    // Tabla de asignación (análoga a la Tabla de Páginas): loteId -> lista de celdas físicas
    private final Map<String, List<AsignacionCelda>> tablaAsignacion = new ConcurrentHashMap<>();

    public AsignadorUbicaciones(Almacen almacen) {
        this.almacen = almacen;
    }

    /**
     * Almacena un lote en celdas libres y reporta lo que no cupo.
     *
     * @throws AlmacenLlenoException si ninguna celda puede recibir el lote.
     */
    public ResultadoAsignacion asignarLote(String loteId, Producto producto, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a almacenar debe ser mayor a cero");
        }
        // Sincronización segura: un único escritor recorre y muta las celdas.
        return almacen.enEscritura(() -> asignarBajoCerrojo(loteId, producto, cantidad));
    }

    private ResultadoAsignacion asignarBajoCerrojo(String loteId, Producto producto, int cantidad) {
        List<Ubicacion> celdas = almacen.celdasInternas();
        List<Ubicacion> utilizables = celdas.stream()
                .filter(u -> u.getEspacioDisponible() > 0 && u.puedeAlmacenar(loteId))
                .toList();

        if (utilizables.isEmpty()) {
            // Ningún marco libre: fallo de asignación (ENOSPC / Out Of Memory)
            throw new AlmacenLlenoException(
                    String.format("Almacén sin celdas disponibles. Quedan %d unidades pendientes", cantidad),
                    cantidad
            );
        }

        int total = celdas.size();
        int inicio = Math.floorMod(loteId.hashCode(), total);
        int paso = calcularPaso(total);

        List<AsignacionCelda> asignaciones = new ArrayList<>();
        int restante = cantidad;
        for (int i = 0; i < total && restante > 0; i++) {
            Ubicacion celda = celdas.get((inicio + i * paso) % total);
            if (!celda.puedeAlmacenar(loteId) || celda.getEspacioDisponible() <= 0) {
                continue; // celda ocupada por otro lote o llena: se salta
            }
            int guardadas = celda.almacenar(loteId, producto.id(), restante);
            if (guardadas > 0) {
                asignaciones.add(new AsignacionCelda(
                        celda.getId(),
                        celda.getPasillo(),
                        celda.getNivel(),
                        celda.getPosicion(),
                        guardadas
                ));
                restante -= guardadas;
            }
        }

        tablaAsignacion.put(loteId, List.copyOf(asignaciones));
        return new ResultadoAsignacion(asignaciones, restante);
    }

    /**
     * Busca un paso coprimo con el total de celdas para garantizar que el recorrido
     * circular visite todas las ubicaciones exactamente una vez.
     */
    private int calcularPaso(int total) {
        int paso = 7;
        while (maximoComunDivisor(paso, total) != 1) {
            paso++;
        }
        return paso;
    }

    private int maximoComunDivisor(int a, int b) {
        return b == 0 ? a : maximoComunDivisor(b, a % b);
    }

    public List<AsignacionCelda> consultarUbicacionesLote(String loteId) {
        return tablaAsignacion.getOrDefault(loteId, Collections.emptyList());
    }

    public Map<String, List<AsignacionCelda>> getTablaAsignacion() {
        return Collections.unmodifiableMap(tablaAsignacion);
    }

    public void limpiar() {
        tablaAsignacion.clear();
        almacen.limpiar();
    }
}
