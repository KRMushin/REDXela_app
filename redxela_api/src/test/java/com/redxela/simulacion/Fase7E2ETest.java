package com.redxela.simulacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redxela.api.dto.CrearPedidoRequest;
import com.redxela.api.dto.ItemPedidoRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de extremo a extremo de FASE 7A sobre los seis puntos de interfaz.
 *
 * Concepto de SO: Verificación integral de las llamadas al sistema y de la
 * telemetría del kernel simulado: procesos, cola de listos, recursos, E/S,
 * contabilidad y recuperación de interbloqueos.
 */
@SpringBootTest(properties = {
        "simulacion.time-scale-ms=2000",
        "simulacion.publicacion-estado-ms=100",
        "simulacion.deteccion-interbloqueo-ms=100",
        "simulacion.tasa-generador-pedidos-minuto=1"
})
@AutoConfigureMockMvc
class Fase7E2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ServicioReinicio reinicio;

    @BeforeEach
    @AfterEach
    void limpiar() {
        reinicio.reiniciar();
    }

    @Test
    @DisplayName("Puntos 1-4: recursos, pedidos en proceso, lista de espera y estadísticas")
    void recursosPedidosEsperaYEstadisticas() throws Exception {
        JsonNode inicial = snapshot();
        assertEquals(6, inicial.get("recursos").size());
        assertEquals(20, inicial.get("almacen").get("ubicaciones").size());

        crearPedido("CLI-01", "PROD-03", 5);
        JsonNode conCarga = esperarSnapshot(
                nodo -> nodo.get("pedidosEnProceso").size() > 0,
                5000,
                "Debe observarse un pedido en proceso"
        );
        assertTrue(conCarga.get("pedidosEnProceso").get(0).get("progreso").isNumber());
        for (int i = 1; i < 10; i++) {
            crearPedido("CLI-01", "PROD-03", 5);
        }

        JsonNode finalizado = esperarSnapshot(
                nodo -> nodo.get("pedidosEnProceso").isEmpty()
                        && nodo.get("listaEspera").isEmpty()
                        && nodo.get("estadisticas").get("pedidosCompletados").asInt() == 10,
                20000,
                "Los diez pedidos deben completarse"
        );
        assertEquals(10, finalizado.get("estadisticas").get("pedidosCompletados").asInt());

        JsonNode estadisticas = json(get("/api/estadisticas"));
        assertEquals(10, estadisticas.get("pedidosCompletados").asInt());
        assertEquals(5, estadisticas.get("porNivel").size());
        assertEquals(6, estadisticas.get("ocupacionRecursos").size());
        assertTrue(estadisticas.get("cumplimientoSlaPorcentaje").isNumber());
        assertTrue(estadisticas.get("tasaThroughputPorMinuto").isNumber());
    }

    private void crearPedido(String clienteId, String productoId, int cantidad) throws Exception {
        CrearPedidoRequest request = new CrearPedidoRequest(
                clienteId,
                List.of(new ItemPedidoRequest(productoId, cantidad))
        );
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private JsonNode snapshot() throws Exception {
        return json(get("/api/estado/snapshot"));
    }

    private JsonNode json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder solicitud)
            throws Exception {
        MvcResult resultado = mockMvc.perform(solicitud)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }

    private JsonNode esperarSnapshot(
            Predicate<JsonNode> condicion,
            long timeoutMs,
            String mensaje
    ) throws Exception {
        long limite = System.currentTimeMillis() + timeoutMs;
        JsonNode ultimo = snapshot();
        while (System.currentTimeMillis() < limite) {
            ultimo = snapshot();
            if (condicion.test(ultimo)) {
                return ultimo;
            }
            Thread.sleep(25);
        }
        fail(mensaje + ". Último snapshot: " + ultimo);
        return ultimo;
    }
}
