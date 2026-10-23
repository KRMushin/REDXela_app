package com.redxela.almacen;

import com.redxela.config.SimulacionProperties;
import com.redxela.dominio.Producto;
import com.redxela.dominio.TipoMercancia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de aceptación de la FASE 4 (parte Almacén).
 * 
 * Concepto de SO: Asignación no contigua de memoria (paginación), fragmentación
 * externa y fallo de asignación cuando no existen marcos libres.
 */
class Fase4AlmacenTest {

    private static final Producto PRODUCTO = new Producto("PROD-X", "Producto X", TipoMercancia.GENERAL, "Prueba");

    private SimulacionProperties propiedades(int ubicaciones, int capacidad) {
        return new SimulacionProperties(1000L, 10, 5, null,
                new SimulacionProperties.AlmacenConfig(ubicaciones, capacidad),
                new SimulacionProperties.InventarioConfig(0));
    }

    private AsignadorUbicaciones crearAsignador(int ubicaciones, int capacidad) {
        return new AsignadorUbicaciones(new Almacen(propiedades(ubicaciones, capacidad)));
    }

    @Test
    @DisplayName("Un lote grande ocupa celdas físicamente no contiguas y se puede consultar su ubicación")
    void loteSeReparteEnCeldasNoContiguas() {
        AsignadorUbicaciones asignador = crearAsignador(20, 50);

        ResultadoAsignacion resultado = asignador.asignarLote("LOTE-NC", PRODUCTO, 150);

        assertEquals(3, resultado.asignaciones().size(), "150 unidades de capacidad 50 deben ocupar 3 celdas");
        assertEquals(150, resultado.unidadesAsignadas());
        assertTrue(resultado.completo());
        assertEquals(3, asignador.consultarUbicacionesLote("LOTE-NC").size());

        List<Integer> indices = resultado.asignaciones().stream()
                .map(AsignacionCelda::ubicacionId)
                .map(id -> Integer.parseInt(id.substring(2)))
                .sorted(Comparator.naturalOrder())
                .toList();
        for (int i = 1; i < indices.size(); i++) {
            assertTrue(indices.get(i) - indices.get(i - 1) > 1,
                    "Las celdas de un mismo lote no deben ser adyacentes: " + indices);
        }
    }

    @Test
    @DisplayName("Si no cabe todo el lote, la asignación parcial reporta las unidades pendientes")
    void asignacionParcialReportaPendiente() {
        AsignadorUbicaciones asignador = crearAsignador(3, 10);
        asignador.asignarLote("LOTE-A", PRODUCTO, 10);
        asignador.asignarLote("LOTE-B", PRODUCTO, 10);

        ResultadoAsignacion parcial = asignador.asignarLote("LOTE-C", PRODUCTO, 15);

        assertEquals(10, parcial.unidadesAsignadas(), "Solo queda una celda libre de 10 unidades");
        assertEquals(5, parcial.unidadesPendientes(), "El pendiente debe quedar visible");
        assertFalse(parcial.completo());
    }

    @Test
    @DisplayName("Sin celdas libres se lanza AlmacenLlenoException con las unidades pendientes")
    void almacenSinEspacioLanzaExcepcion() {
        AsignadorUbicaciones asignador = crearAsignador(2, 10);
        asignador.asignarLote("LOTE-A", PRODUCTO, 10);
        asignador.asignarLote("LOTE-B", PRODUCTO, 10);

        AlmacenLlenoException excepcion = assertThrows(AlmacenLlenoException.class,
                () -> asignador.asignarLote("LOTE-C", PRODUCTO, 5));

        assertEquals(5, excepcion.getUnidadesPendientes());
    }
}
