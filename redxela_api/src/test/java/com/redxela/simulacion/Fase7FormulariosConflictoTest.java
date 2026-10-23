package com.redxela.simulacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redxela.api.dto.CrearClienteRequest;
import com.redxela.api.dto.CrearPedidoRequest;
import com.redxela.api.dto.ItemPedidoRequest;
import com.redxela.api.dto.RegistrarLlegadaRequest;
import com.redxela.dominio.NivelServicio;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E de FASE 7A para los puntos 5 y 6: formularios manuales y conflicto.
 *
 * Concepto de SO: Llamadas al sistema de alta de procesos, ingreso de E/S y
 * recuperación manual de interbloqueos, verificadas contra la API real.
 */
@SpringBootTest(properties = {
        "simulacion.time-scale-ms=2000",
        "simulacion.publicacion-estado-ms=100",
        "simulacion.deteccion-interbloqueo-ms=100",
        "simulacion.tasa-generador-pedidos-minuto=1"
})
@AutoConfigureMockMvc
class Fase7FormulariosConflictoTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ServicioReinicio reinicio;

    @BeforeEach
    @AfterEach
    void limpiar() {
        reinicio.reiniciar();
    }

    @Test
    @DisplayName("Punto 5: alta de cliente, pedido manual y llegada de mercadería")
    void formulariosManuales() throws Exception {
        CrearClienteRequest clienteRequest = new CrearClienteRequest("Cliente E2E", NivelServicio.PRIORITARIO);
        String clienteJson = mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clienteRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Cliente E2E"))
                .andReturn().getResponse().getContentAsString();
        String clienteId = objectMapper.readTree(clienteJson).get("id").asText();

        CrearPedidoRequest pedidoRequest = new CrearPedidoRequest(
                clienteId,
                List.of(new ItemPedidoRequest("PROD-01", 3), new ItemPedidoRequest("PROD-07", 2))
        );
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pedidoRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cliente.id").value(clienteId))
                .andExpect(jsonPath("$.totalArticulos").value(5));

        RegistrarLlegadaRequest llegada = new RegistrarLlegadaRequest("PROD-05", 20);
        mockMvc.perform(post("/api/recepcion/llegada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(llegada)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.loteId").isNotEmpty())
                .andExpect(jsonPath("$.cantidad").value(20));

        mockMvc.perform(get("/api/recepcion/inventario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadLotes").isNumber())
                .andExpect(jsonPath("$.elementosEnCola").isNumber())
                .andExpect(jsonPath("$.capacidadCola").value(50));
    }

    @Test
    @DisplayName("Punto 6: visualizar el conflicto, elegir ganador y resolverlo")
    void conflictoSeVisualizaYResuelve() throws Exception {
        mockMvc.perform(put("/api/interbloqueo/modo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modo\":\"SIN_PREVENCION\"}"))
                .andExpect(status().isOk());

        JsonNode reporte = json(post("/api/interbloqueo/forzar"));
        assertTrue(reporte.get("hayInterbloqueo").asBoolean());
        assertFalse(reporte.get("pedidosInvolucrados").isEmpty());
        assertFalse(reporte.get("recursosEnDisputa").isEmpty());
        assertTrue(reporte.get("explicacion").isTextual());
        String ganador = reporte.get("pedidosInvolucrados").get(0).asText();

        mockMvc.perform(post("/api/interbloqueo/resolver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoGanadorId\":\"" + ganador + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reporte.hayInterbloqueo").value(false));

        mockMvc.perform(get("/api/interbloqueo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hayInterbloqueo").value(false));

        JsonNode espera = json(get("/api/planificacion/espera"));
        assertFalse(espera.isEmpty(), "Los procesos perdedores deben volver a la cola de listos");
        assertEquals(1, espera.get(0).get("posicion").asInt());
        assertTrue(espera.get(0).get("motivoPosicion").isTextual());
    }

    private JsonNode json(MockHttpServletRequestBuilder solicitud) throws Exception {
        MvcResult resultado = mockMvc.perform(solicitud)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }
}
