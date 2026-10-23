package com.redxela.simulacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Aceptación REST de FASE 6A: endpoints de operación y reinicio de la simulación.
 *
 * Concepto de SO: Llamadas administrativas al sistema (syscalls) para crear
 * procesos, inyectar E/S, cambiar el planificador y reiniciar el estado global.
 */
@SpringBootTest(properties = {
        "simulacion.time-scale-ms=20",
        "simulacion.publicacion-estado-ms=100",
        "simulacion.tasa-generador-pedidos-minuto=1"
})
@AutoConfigureMockMvc
class Fase6RestTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ServicioReinicio reinicio;

    @BeforeEach
    void preparar() {
        reinicio.reiniciar();
    }

    @Test
    @DisplayName("Catálogo, clientes, pedidos, recepción, política y generador responden")
    void endpointsOperativos() throws Exception {
        mockMvc.perform(get("/api/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[0].id").value("PROD-01"));

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Tienda Prueba\",\"nivelServicio\":\"PRIORITARIO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty());

        String pedidoJson = mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":\"CLI-01\",\"items\":[{\"productoId\":\"PROD-01\",\"cantidad\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.totalArticulos").value(2))
                .andReturn().getResponse().getContentAsString();

        String pedidoId = objectMapper.readTree(pedidoJson).get("id").asText();
        mockMvc.perform(get("/api/pedidos")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/pedidos/{id}", pedidoId)).andExpect(status().isOk());

        mockMvc.perform(post("/api/recepcion/llegada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":\"PROD-03\",\"cantidad\":10}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.loteId").isNotEmpty());
        mockMvc.perform(get("/api/recepcion/inventario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existencias").isNotEmpty());

        mockMvc.perform(get("/api/planificacion/politica"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.politica").value("PRIORIDAD_CON_ENVEJECIMIENTO"));
        mockMvc.perform(put("/api/planificacion/politica")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"politica\":\"MENOR_CANTIDAD_PRIMERO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.politica").value("MENOR_CANTIDAD_PRIMERO"));

        mockMvc.perform(post("/api/simulacion/generador/iniciar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
        mockMvc.perform(get("/api/simulacion/generador"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
        mockMvc.perform(put("/api/simulacion/generador/tasa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidosPorMinuto\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasaPorMinuto").value(8));
        mockMvc.perform(post("/api/simulacion/generador/detener"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        mockMvc.perform(put("/api/interbloqueo/modo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modo\":\"SIN_PREVENCION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modo").value("SIN_PREVENCION"));
        mockMvc.perform(get("/api/interbloqueo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hayInterbloqueo").value(false));
    }

    @Test
    @DisplayName("El reinicio detiene el generador, vacía pedidos y restaura recursos e inventario")
    void reinicioRestauraEstadoInicial() throws Exception {
        mockMvc.perform(post("/api/simulacion/generador/iniciar")).andExpect(status().isOk());
        Thread.sleep(50); // el generador crea al menos un pedido al arrancar

        mockMvc.perform(post("/api/simulacion/reiniciar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").isNotEmpty());

        mockMvc.perform(get("/api/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/simulacion/generador"))
                .andExpect(jsonPath("$.activo").value(false));

        String snapshot = mockMvc.perform(get("/api/estado/snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidosEnProceso").isEmpty())
                .andExpect(jsonPath("$.listaEspera").isEmpty())
                .andExpect(jsonPath("$.generadorActivo").value(false))
                .andExpect(jsonPath("$.modoPrevencion").value("PREVENCION_ACTIVA"))
                .andExpect(jsonPath("$.politicaPlanificacion").value("PRIORIDAD_CON_ENVEJECIMIENTO"))
                .andExpect(jsonPath("$.almacen.unidadesOcupadas").value(600))
                .andExpect(jsonPath("$.estadisticas.pedidosCompletados").value(0))
                .andReturn().getResponse().getContentAsString();

        JsonNode recursos = objectMapper.readTree(snapshot).get("recursos");
        for (JsonNode recurso : recursos) {
            org.junit.jupiter.api.Assertions.assertEquals(0, recurso.get("enUso").asInt());
        }
        mockMvc.perform(get("/api/recepcion/inventario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadLotes").value(12))
                .andExpect(jsonPath("$.unidadesPendientes").value(0));
    }
}
