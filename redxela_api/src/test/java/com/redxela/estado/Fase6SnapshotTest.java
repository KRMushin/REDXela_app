package com.redxela.estado;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redxela.simulacion.ServicioReinicio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Aceptación de FASE 6A: el snapshot JSON debe coincidir con CONTRATO_API.md.
 *
 * Concepto de SO: Verificación del contrato de la interfaz de telemetría
 * (procfs schema validation) y de la política CORS entre orígenes.
 */
@SpringBootTest(properties = {
        "simulacion.time-scale-ms=20",
        "simulacion.publicacion-estado-ms=100",
        "simulacion.tasa-generador-pedidos-minuto=1"
})
@AutoConfigureMockMvc
class Fase6SnapshotTest {

    private static final Set<String> CAMPOS_SNAPSHOT = Set.of(
            "timestamp", "tiempoSimuladoMinutos", "modoPrevencion",
            "politicaPlanificacion", "generadorActivo", "tasaGeneradorPorMinuto",
            "recursos", "pedidosEnProceso", "listaEspera", "almacen",
            "interbloqueo", "estadisticas"
    );

    private static final Set<String> CAMPOS_RECURSO = Set.of(
            "tipo", "nombre", "total", "disponibles", "enUso",
            "pedidosAsignados", "pedidosEnEspera"
    );

    private static final Set<String> CAMPOS_UBICACION = Set.of(
            "id", "pasillo", "nivel", "posicion", "loteId",
            "productoId", "productoNombre", "unidades", "capacidad"
    );

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ServicioReinicio reinicio;

    @BeforeEach
    void preparar() {
        reinicio.reiniciar();
    }

    @AfterEach
    void limpiar() {
        reinicio.reiniciar();
    }

    @Test
    @DisplayName("El snapshot contiene exactamente los campos y tipos del contrato")
    void snapshotCoincideConContrato() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/estado/snapshot"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(resultado.getResponse().getContentAsString());
        assertEquals(CAMPOS_SNAPSHOT, nombres(root));

        assertEquals(6, root.get("recursos").size());
        for (JsonNode recurso : root.get("recursos")) {
            Set<String> esperados = new HashSet<>(CAMPOS_RECURSO);
            if ("ESCANER".equals(recurso.get("tipo").asText())) {
                esperados.add("retenidoPor");
            }
            assertEquals(esperados, nombres(recurso));
            assertEquals(0, recurso.get("enUso").asInt());
            assertEquals(recurso.get("total").asInt(), recurso.get("disponibles").asInt());
        }

        assertEquals(0, root.get("pedidosEnProceso").size());
        assertEquals(0, root.get("listaEspera").size());
        assertEquals("PREVENCION_ACTIVA", root.get("modoPrevencion").asText());
        assertEquals("PRIORIDAD_CON_ENVEJECIMIENTO", root.get("politicaPlanificacion").asText());
        assertFalse(root.get("generadorActivo").asBoolean());

        JsonNode almacen = root.get("almacen");
        assertEquals(Set.of("capacidadTotal", "unidadesOcupadas", "ubicaciones", "colaRecepcion"),
                nombres(almacen));
        assertEquals(20, almacen.get("ubicaciones").size());
        assertEquals(600, almacen.get("unidadesOcupadas").asInt());
        almacen.get("ubicaciones").forEach(u -> assertEquals(CAMPOS_UBICACION, nombres(u)));
        assertEquals(Set.of("elementosEnCola", "capacidadMaxima"),
                nombres(almacen.get("colaRecepcion")));

        assertEquals(Set.of("hayInterbloqueo", "pedidosInvolucrados",
                        "recursosEnDisputa", "explicacion"),
                nombres(root.get("interbloqueo")));
        assertEquals(Set.of("pedidosCompletados", "tiempoEsperaPromedioMinutos",
                        "tiempoProcesamientoPromedioMinutos", "cumplimientoSlaPorcentaje",
                        "tasaThroughputPorMinuto"),
                nombres(root.get("estadisticas")));
        assertEquals(0, root.get("estadisticas").get("pedidosCompletados").asInt());
    }

    @Test
    @DisplayName("CORS permite el origen de Vite en desarrollo")
    void corsPermiteFrontendDeDesarrollo() throws Exception {
        mockMvc.perform(options("/api/estado/snapshot")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:5173"));
    }

    private Set<String> nombres(JsonNode nodo) {
        Set<String> nombres = new HashSet<>();
        nodo.fieldNames().forEachRemaining(nombres::add);
        return nombres;
    }
}
