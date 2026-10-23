package com.redxela.inventario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redxela.api.dto.RegistrarLlegadaRequest;
import com.redxela.dominio.*;
import com.redxela.recepcion.AreaRecepcion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de aceptación de la FASE 4: Recepción, Inventario y Almacén.
 * 
 * Concepto de SO: Productor-Consumidor con búfer acotado y contrapresión,
 * reserva atómica de recursos bajo lectores-escritores, y despertar de procesos
 * bloqueados por falta de existencias (EN_ESPERA_STOCK).
 */
@SpringBootTest(properties = {
        "simulacion.almacen.total-ubicaciones=500",
        "simulacion.inventario.stock-inicial-por-producto=0",
        "simulacion.capacidad-cola-recepcion=10",
        "simulacion.time-scale-ms=1",
        "simulacion.tasa-generador-pedidos-minuto=1"
})
@AutoConfigureMockMvc
class Fase4AceptacionTest {

    @Autowired private AreaRecepcion areaRecepcion;
    @Autowired private ServicioInventario servicioInventario;
    @Autowired private PedidoService pedidoService;
    @Autowired private ClienteService clienteService;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private void esperarHasta(BooleanSupplier condicion, long timeoutMs) throws InterruptedException {
        long limite = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < limite) {
            if (condicion.getAsBoolean()) {
                return;
            }
            Thread.sleep(10);
        }
        fail("Tiempo de espera agotado esperando la condición de la simulación");
    }

    @Test
    @DisplayName("Ráfaga de recepciones sin pérdida de mensajes pese a la cola acotada")
    void rafagaNoPierdeMensajes() throws Exception {
        String productoId = "PROD-01";
        int inicial = servicioInventario.existenciaDe(productoId);
        int lotesIniciales = servicioInventario.lotes().size();
        int mensajes = 120;
        int unidadesPorMensaje = 2;

        ExecutorService productores = Executors.newFixedThreadPool(6);
        List<Future<?>> futuros = new ArrayList<>();
        for (int hilo = 0; hilo < 6; hilo++) {
            futuros.add(productores.submit(() -> {
                for (int i = 0; i < mensajes / 6; i++) {
                    try {
                        areaRecepcion.recibirLlegada(productoId, unidadesPorMensaje);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
                return null;
            }));
        }
        for (Future<?> futuro : futuros) {
            futuro.get(20, TimeUnit.SECONDS);
        }
        productores.shutdown();

        int esperado = inicial + mensajes * unidadesPorMensaje;
        esperarHasta(() -> areaRecepcion.colaVacia()
                && servicioInventario.existenciaDe(productoId) == esperado
                && servicioInventario.lotes().size() - lotesIniciales == mensajes, 15000);

        assertEquals(esperado, servicioInventario.existenciaDe(productoId),
                "Ningún mensaje de la ráfaga debe perderse");
        assertEquals(mensajes, servicioInventario.lotes().size() - lotesIniciales,
                "Cada mensaje debe registrarse como un lote");
    }

    @Test
    @DisplayName("Un pedido en EN_ESPERA_STOCK avanza solo cuando llega mercadería")
    void pedidoEnEsperaStockAvanzaAlLlegarMercancia() throws Exception {
        String productoId = "PROD-12";
        int disponible = servicioInventario.existenciaDe(productoId);
        Cliente cliente = clienteService.buscarPorId("CLI-01").orElseThrow();
        Producto producto = new Producto(productoId, "Producto de prueba", TipoMercancia.GENERAL, "Prueba");

        Pedido pedido = pedidoService.registrarNuevoPedido(cliente,
                List.of(new ItemPedido(producto, disponible + 5)));

        assertEquals(EstadoPedido.EN_ESPERA_STOCK, pedido.getEstado(), "Sin stock el pedido debe bloquearse");
        assertEquals(1, pedidoService.pedidosEnEsperaStock());

        areaRecepcion.recibirLlegada(productoId, 5); // abastece exactamente lo que faltaba

        esperarHasta(() -> pedido.getEstado() != EstadoPedido.EN_ESPERA_STOCK, 10000);
        assertEquals(0, pedidoService.pedidosEnEsperaStock());
        assertEquals(0, servicioInventario.existenciaDe(productoId), "La reserva debe descontar el stock");
    }

    @Test
    @DisplayName("Endpoint REST de recepción acepta el lote y el inventario lo refleja")
    void endpointRecepcionAceptaCarga() throws Exception {
        String productoId = "PROD-05";
        int inicial = servicioInventario.existenciaDe(productoId);

        mockMvc.perform(post("/api/recepcion/llegada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegistrarLlegadaRequest(productoId, 7))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.loteId").exists())
                .andExpect(jsonPath("$.productoId").value(productoId))
                .andExpect(jsonPath("$.cantidad").value(7));

        esperarHasta(() -> servicioInventario.existenciaDe(productoId) == inicial + 7, 10000);

        mockMvc.perform(get("/api/recepcion/inventario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existencias['" + productoId + "']").value(inicial + 7))
                .andExpect(jsonPath("$.capacidadCola").value(10));
    }
}
