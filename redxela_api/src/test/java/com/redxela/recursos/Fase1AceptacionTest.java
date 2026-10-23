package com.redxela.recursos;

import com.redxela.dominio.Cliente;
import com.redxela.dominio.ItemPedido;
import com.redxela.dominio.NivelServicio;
import com.redxela.dominio.Pedido;
import com.redxela.dominio.Producto;
import com.redxela.dominio.TipoMercancia;
import com.redxela.dominio.TipoRecurso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de aceptación para la FASE 1: Recursos y sincronización.
 * 
 * Concepto de SO: Evaluación de sincronización concurrente, semáforos contadores,
 * exclusión mutua, invariantes de estado y recuperación/expropiación mediante interrupción.
 */
@SpringBootTest
class Fase1AceptacionTest {

    @Autowired
    private GestorRecursos gestorRecursos;

    private Pedido crearPedidoFicticio(String id) {
        Cliente cli = new Cliente("CLI-TEST", "Cliente Prueba", NivelServicio.ESTANDAR);
        Producto prod = new Producto("PROD-T", "Producto Test", TipoMercancia.GENERAL, "Desc");
        return new Pedido(id, cli, List.of(new ItemPedido(prod, 1)));
    }

    @Test
    @DisplayName("50 hilos concurrentes compitiendo por un Pool no deben violar la capacidad máxima")
    void test50HilosConcurrenciaInvarianteCapacidad() throws InterruptedException {
        final int CAPACIDAD = 3;
        final int NUM_HILOS = 50;
        PoolRecursos pool = new PoolRecursos(TipoRecurso.MONTACARGAS, CAPACIDAD);

        CountDownLatch salidaComun = new CountDownLatch(1);
        CountDownLatch hilosCompletados = new CountDownLatch(NUM_HILOS);

        AtomicInteger enSeccionCritica = new AtomicInteger(0);
        AtomicInteger maxConcurrenteObservado = new AtomicInteger(0);
        AtomicBoolean huboInfraccion = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(NUM_HILOS);

        for (int i = 0; i < NUM_HILOS; i++) {
            final String idPedido = "PED-CONC-" + i;
            executor.submit(() -> {
                Pedido pedido = crearPedidoFicticio(idPedido);
                try {
                    salidaComun.await(); // Barrera para iniciar todos al mismo instante
                    pool.adquirir(pedido);
                    try {
                        int actuales = enSeccionCritica.incrementAndGet();
                        maxConcurrenteObservado.accumulateAndGet(actuales, Math::max);

                        if (actuales > CAPACIDAD) {
                            huboInfraccion.set(true);
                        }

                        // Simula breve uso del recurso
                        Thread.sleep(10);
                    } finally {
                        enSeccionCritica.decrementAndGet();
                        pool.liberar(pedido);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    hilosCompletados.countDown();
                }
            });
        }

        salidaComun.countDown(); // Despierta a los 50 hilos en paralelo
        assertTrue(hilosCompletados.await(10, TimeUnit.SECONDS), "Los 50 hilos debieron terminar en el tiempo previsto");
        executor.shutdown();

        assertFalse(huboInfraccion.get(), "Nunca debe superarse la capacidad máxima del semáforo");
        assertTrue(maxConcurrenteObservado.get() <= CAPACIDAD, "El pico concurrente observado debe ser <= " + CAPACIDAD);

        // Invariante de estado al finalizar
        EstadoRecurso estadoFinal = pool.estado();
        assertEquals(CAPACIDAD, estadoFinal.total());
        assertEquals(CAPACIDAD, estadoFinal.disponibles());
        assertEquals(0, estadoFinal.enUso());
        assertTrue(estadoFinal.pedidosAsignados().isEmpty(), "No deben quedar pedidos asignados");
        assertTrue(estadoFinal.pedidosEnEspera().isEmpty(), "No deben quedar pedidos en espera");
    }

    @Test
    @DisplayName("Un pedido bloqueado en espera puede ser expropiado por interrupción sin corromper el pool")
    void testCancelacionPorInterrupcionSinFugaDeRecursos() throws Exception {
        PoolRecursos pool = new PoolRecursos(TipoRecurso.AREA_CARGA, 1);
        Pedido pedido1 = crearPedidoFicticio("PED-BLOQ-1");
        Pedido pedido2 = crearPedidoFicticio("PED-BLOQ-2");

        // Saturar el pool
        pool.adquirir(pedido1);
        assertEquals(0, pool.getDisponibles());

        CountDownLatch esperaIniciada = new CountDownLatch(1);
        AtomicBoolean interrupcionDetectada = new AtomicBoolean(false);

        Thread hiloBloqueado = new Thread(() -> {
            try {
                esperaIniciada.countDown();
                pool.adquirir(pedido2); // Bloqueado esperando la unidad
            } catch (InterruptedException e) {
                interrupcionDetectada.set(true);
            }
        });

        hiloBloqueado.start();
        assertTrue(esperaIniciada.await(1, TimeUnit.SECONDS));

        // Esperar brevemente a que el hilo quede en espera en el pool
        Thread.sleep(100);
        assertTrue(pool.estado().pedidosEnEspera().contains("PED-BLOQ-2"), "El pedido 2 debe figurar en espera");

        // Simular expropiación por el gestor de deadlock
        hiloBloqueado.interrupt();
        hiloBloqueado.join(1000);

        assertTrue(interrupcionDetectada.get(), "La interrupción debió ser capturada y manejada");
        assertFalse(pool.estado().pedidosEnEspera().contains("PED-BLOQ-2"), "La lista de espera debió limpiarse");

        // Liberar el pedido 1 y comprobar consistencia
        pool.liberar(pedido1);
        assertEquals(1, pool.getDisponibles());
        assertEquals(0, pool.estado().enUso());
    }

    @Test
    @DisplayName("El escáner compartido debe garantizar exclusión mutua estricta")
    void testEscanerExclusionMutua() throws Exception {
        Escaner escaner = new Escaner();
        Pedido pedido = crearPedidoFicticio("PED-ESC-1");

        assertFalse(escaner.estaEnUso());
        escaner.adquirir(pedido);
        assertTrue(escaner.estaEnUso());
        assertEquals("PED-ESC-1", escaner.getRetenedorActual());

        // Intentar adquirir desde recepción en otro hilo: debe fallar con timeout si no está libre
        CountDownLatch lockOcupado = new CountDownLatch(1);
        Thread hiloRecepcion = new Thread(() -> {
            try {
                escaner.adquirirPorRecepcion();
                lockOcupado.countDown();
                escaner.liberarPorRecepcion();
            } catch (InterruptedException ignored) {}
        });

        hiloRecepcion.start();
        assertFalse(lockOcupado.await(100, TimeUnit.MILLISECONDS), "Recepción no puede entrar mientras pedido lo retiene");

        escaner.liberar(pedido);
        assertTrue(lockOcupado.await(1, TimeUnit.SECONDS), "Recepción debe ingresar una vez liberado");
        hiloRecepcion.join();
        assertFalse(escaner.estaEnUso());
    }

    @Test
    @DisplayName("GestorRecursos ordena según ModoPrevencion en caliente")
    void testModoPrevencionHavenderVsNatural() {
        List<TipoRecurso> ordenVoluminoso = TipoMercancia.VOLUMINOSO.getRecursosRequeridos();
        // Voluminoso natural: AREA_CARGA (rango 3) -> MONTACARGAS (rango 2) -> ESTACION_EMPAQUE (rango 5)

        // 1. Modo PREVENCION_ACTIVA: ordena por ordenGlobal ascendente (Havender)
        gestorRecursos.cambiarModo(ModoPrevencion.PREVENCION_ACTIVA);
        assertEquals(ModoPrevencion.PREVENCION_ACTIVA, gestorRecursos.getModo());

        List<TipoRecurso> ordenHavender = gestorRecursos.determinarOrdenAdquisicion(ordenVoluminoso);
        assertEquals(
                List.of(TipoRecurso.MONTACARGAS, TipoRecurso.AREA_CARGA, TipoRecurso.ESTACION_EMPAQUE),
                ordenHavender,
                "En PREVENCION_ACTIVA Montacargas (2) debe preceder a Área de Carga (3)"
        );

        // 2. Modo SIN_PREVENCION: respeta orden natural arbitrario
        gestorRecursos.cambiarModo(ModoPrevencion.SIN_PREVENCION);
        assertEquals(ModoPrevencion.SIN_PREVENCION, gestorRecursos.getModo());

        List<TipoRecurso> ordenNatural = gestorRecursos.determinarOrdenAdquisicion(ordenVoluminoso);
        assertEquals(ordenVoluminoso, ordenNatural, "En SIN_PREVENCION debe preservarse el orden natural de la mercancía");

        // Restaurar modo preventivo por defecto
        gestorRecursos.cambiarModo(ModoPrevencion.PREVENCION_ACTIVA);
    }
}
