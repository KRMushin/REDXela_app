package com.redxela.planificacion;

import com.redxela.api.dto.CambiarPoliticaRequest;
import com.redxela.dominio.*;
import com.redxela.planificacion.politicas.MenorCantidadPrimero;
import com.redxela.planificacion.politicas.PrioridadConEnvejecimiento;
import com.redxela.planificacion.politicas.PrioridadEstricta;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de aceptación para la FASE 2: Planificación y lista de espera.
 * 
 * Concepto de SO: Validación comparativa de algoritmos de planificación (Priority, Aging, SJF),
 * prevención de inanición, atomicidad del despachador y conmutación de algoritmo en caliente.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Fase2AceptacionTest {

    @Autowired
    private Despachador despachador;

    @Autowired
    private PrioridadEstricta politicaEstricta;

    @Autowired
    private PrioridadConEnvejecimiento politicaAging;

    @Autowired
    private MenorCantidadPrimero politicaSJF;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Pedido crearPedidoPersonalizado(String id, NivelServicio nivel, int articulos, long tCreacion) {
        Cliente cli = new Cliente("CLI-" + id, "Cliente " + id, nivel);
        Producto prod = new Producto("PROD-" + id, "Prod " + id, TipoMercancia.GENERAL, "Desc");
        Pedido p = new Pedido(id, cli, List.of(new ItemPedido(prod, articulos)));

        // Ajuste reflexivo de tCreacion para pruebas de envejecimiento deterministas
        try {
            Field campoTCreacion = Pedido.class.getDeclaredField("tCreacion");
            campoTCreacion.setAccessible(true);
            campoTCreacion.setLong(p, tCreacion);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return p;
    }

    @Test
    @DisplayName("La misma carga de pedidos debe generar órdenes completamente distintos según la política activa")
    void testMismaCargaGeneraOrdenesDistintos() {
        long ahora = 1000000000000L;

        // Pedido A: Nivel 5 (Económico, SLA 120m), 2 artículos, esperó 240 min (envejecido)
        Pedido pedidoA = crearPedidoPersonalizado("PED-A", NivelServicio.ECONOMICO, 2, ahora - (240L * 60 * 1000));

        // Pedido B: Nivel 2 (Prioritario, SLA 10m), 20 artículos, recién llegado (esperó 1 min)
        Pedido pedidoB = crearPedidoPersonalizado("PED-B", NivelServicio.PRIORITARIO, 20, ahora - (1L * 60 * 1000));

        // Pedido C: Nivel 3 (Estándar, SLA 30m), 1 artículo, recién llegado (esperó 2 min)
        Pedido pedidoC = crearPedidoPersonalizado("PED-C", NivelServicio.ESTANDAR, 1, ahora - (2L * 60 * 1000));

        List<Pedido> carga = List.of(pedidoA, pedidoB, pedidoC);

        // 1. PRIORIDAD ESTRICTA: B (Nivel 2) -> C (Nivel 3) -> A (Nivel 5)
        List<String> ordenEstricto = carga.stream()
                .sorted(politicaEstricta.obtenerComparador(ahora))
                .map(Pedido::getId)
                .toList();
        assertEquals(List.of("PED-B", "PED-C", "PED-A"), ordenEstricto, "Prioridad estricta debe ordenar: B, C, A");

        // 2. MENOR CANTIDAD PRIMERO (SJF): C (1 art) -> A (2 art) -> B (20 art)
        List<String> ordenSJF = carga.stream()
                .sorted(politicaSJF.obtenerComparador(ahora))
                .map(Pedido::getId)
                .toList();
        assertEquals(List.of("PED-C", "PED-A", "PED-B"), ordenSJF, "SJF debe ordenar: C, A, B");

        // 3. PRIORIDAD CON ENVEJECIMIENTO (Aging): A (prioridad efectiva 1.0) -> B (1.80) -> C (2.87)
        List<String> ordenAging = carga.stream()
                .sorted(politicaAging.obtenerComparador(ahora))
                .map(Pedido::getId)
                .toList();
        assertEquals(List.of("PED-A", "PED-B", "PED-C"), ordenAging, "Aging debe priorizar al envejecido: A, B, C");

        // Verificar que los 3 órdenes son estrictamente diferentes entre sí
        assertNotEquals(ordenEstricto, ordenSJF);
        assertNotEquals(ordenEstricto, ordenAging);
        assertNotEquals(ordenSJF, ordenAging);
    }

    @Test
    @DisplayName("El despachador es el único punto de salida y transiciona a EN_PROCESO atómicamente")
    void testDespachadorExtraccionAtomica() {
        ListaEspera lista = despachador.getListaEspera();
        lista.vaciar();

        Pedido p1 = crearPedidoPersonalizado("PED-DESP-1", NivelServicio.ESTANDAR, 5, System.currentTimeMillis());
        lista.agregar(p1);
        assertEquals(1, lista.tamano());
        assertEquals(EstadoPedido.EN_ESPERA_RECURSOS, p1.getEstado());

        Optional<Pedido> despachado = despachador.despacharSiguiente(System.currentTimeMillis());
        assertTrue(despachado.isPresent());
        assertEquals("PED-DESP-1", despachado.get().getId());
        assertEquals(EstadoPedido.EN_PROCESO, despachado.get().getEstado());
        assertNotNull(despachado.get().getTInicio());

        // La lista debe quedar vacía
        assertEquals(0, lista.tamano());
        assertTrue(despachador.despacharSiguiente(System.currentTimeMillis()).isEmpty());
    }

    @Test
    @DisplayName("Concurrencia en ListaEspera: inserción y extracción masiva simultánea sin pérdidas")
    void testConcurrenciaListaEspera() throws InterruptedException {
        ListaEspera lista = new ListaEspera();
        final int TOTAL_PEDIDOS = 100;
        CountDownLatch latchSalida = new CountDownLatch(1);
        CountDownLatch latchFin = new CountDownLatch(TOTAL_PEDIDOS * 2);

        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger pedidosExtraidos = new AtomicInteger(0);

        // Hilos productores de pedidos a la lista de espera
        for (int i = 0; i < TOTAL_PEDIDOS; i++) {
            final String id = "CONC-PED-" + i;
            executor.submit(() -> {
                try {
                    latchSalida.await();
                    Pedido p = crearPedidoPersonalizado(id, NivelServicio.ESTANDAR, 2, System.currentTimeMillis());
                    lista.agregar(p);
                } catch (InterruptedException ignored) {
                } finally {
                    latchFin.countDown();
                }
            });
        }

        // Hilos consumidores que extraen con el despachador
        for (int i = 0; i < TOTAL_PEDIDOS; i++) {
            executor.submit(() -> {
                try {
                    latchSalida.await();
                    // Intentar extraer con pequeños reintentos hasta que haya elementos
                    for (int intento = 0; intento < 100; intento++) {
                        Optional<Pedido> extraido = lista.extraerSiguiente(politicaEstricta, System.currentTimeMillis());
                        if (extraido.isPresent()) {
                            pedidosExtraidos.incrementAndGet();
                            break;
                        }
                        Thread.sleep(5);
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    latchFin.countDown();
                }
            });
        }

        latchSalida.countDown();
        assertTrue(latchFin.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // La suma de los que quedaron en cola más los extraídos debe ser exactamente el total producido
        assertEquals(TOTAL_PEDIDOS, pedidosExtraidos.get() + lista.tamano());
    }

    @Test
    @DisplayName("Endpoint REST permite cambiar política en caliente y consultar lista de espera")
    void testEndpointCambiarPoliticaEnCaliente() throws Exception {
        // Consultar política actual
        mockMvc.perform(get("/api/planificacion/politica"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.politica").exists());

        // Cambiar política a MENOR_CANTIDAD_PRIMERO
        CambiarPoliticaRequest req = new CambiarPoliticaRequest(TipoPolitica.MENOR_CANTIDAD_PRIMERO);
        mockMvc.perform(put("/api/planificacion/politica")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.politica").value("MENOR_CANTIDAD_PRIMERO"));

        assertEquals(TipoPolitica.MENOR_CANTIDAD_PRIMERO, despachador.getTipoPoliticaActiva());

        // Consultar lista de espera
        mockMvc.perform(get("/api/planificacion/espera"))
                .andExpect(status().isOk());
    }
}
