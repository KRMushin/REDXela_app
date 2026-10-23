package com.redxela.interbloqueo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redxela.api.dto.CambiarModoRequest;
import com.redxela.api.dto.ResolverInterbloqueoRequest;
import com.redxela.dominio.EstadoPedido;
import com.redxela.dominio.PedidoService;
import com.redxela.dominio.TipoRecurso;
import com.redxela.procesamiento.GestorProcesamiento;
import com.redxela.recursos.GestorRecursos;
import com.redxela.recursos.ModoPrevencion;
import com.redxela.recursos.PoolRecursos;
import com.redxela.simulacion.GeneradorPedidos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de aceptación de los endpoints REST de interbloqueo (FASE 5).
 * 
 * Concepto de SO: Interfaz de administración para detección, diagnóstico y
 * recuperación de interbloqueos (deadlock management syscalls).
 */
@SpringBootTest(properties = {
        "simulacion.time-scale-ms=5",
        "simulacion.deteccion-interbloqueo-ms=25",
        "simulacion.tasa-generador-pedidos-minuto=1"
})
@AutoConfigureMockMvc
class Fase5ApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DetectorInterbloqueo detector;
    @Autowired private ResolutorInterbloqueo resolutor;
    @Autowired private GestorRecursos gestorRecursos;
    @Autowired private GestorProcesamiento gestorProcesamiento;
    @Autowired private PedidoService pedidoService;
    @Autowired private GeneradorPedidos generador;

    @BeforeEach
    void prepararSistema() throws InterruptedException {
        generador.detener();
        gestorRecursos.cambiarModo(ModoPrevencion.PREVENCION_ACTIVA);
        ReporteInterbloqueo reporte = detector.detectarAhora();
        if (reporte.hayInterbloqueo()) {
            resolutor.resolver(reporte.pedidosInvolucrados().get(0));
        }
        esperarHasta(() -> gestorProcesamiento.totalTareasActivas() == 0
                && !detector.detectarAhora().hayInterbloqueo()
                && recursoLibre(TipoRecurso.MONTACARGAS)
                && recursoLibre(TipoRecurso.AREA_CARGA)
                && conflictosCompletados(), 15000);
    }

    @Test
    @DisplayName("La API permite cambiar el modo, forzar el conflicto y resolverlo")
    void flujoCompletoPorApi() throws Exception {
        mockMvc.perform(put("/api/interbloqueo/modo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CambiarModoRequest(ModoPrevencion.SIN_PREVENCION))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modo").value("SIN_PREVENCION"));

        String json = mockMvc.perform(post("/api/interbloqueo/forzar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hayInterbloqueo").value(true))
                .andExpect(jsonPath("$.pedidosInvolucrados").isNotEmpty())
                .andExpect(jsonPath("$.explicacion").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        ReporteInterbloqueo reporte = objectMapper.readValue(json, ReporteInterbloqueo.class);
        String ganador = reporte.pedidosInvolucrados().get(0);

        mockMvc.perform(post("/api/interbloqueo/resolver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResolverInterbloqueoRequest(ganador))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reporte.hayInterbloqueo").value(false));

        mockMvc.perform(get("/api/interbloqueo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hayInterbloqueo").value(false));

        esperarHasta(() -> gestorProcesamiento.totalTareasActivas() == 0
                && recursoLibre(TipoRecurso.MONTACARGAS)
                && recursoLibre(TipoRecurso.AREA_CARGA)
                && conflictosCompletados(), 15000);
    }

    private boolean recursoLibre(TipoRecurso tipo) {
        PoolRecursos pool = gestorRecursos.getPool(tipo);
        return pool.getDisponibles() == pool.getTotalUnidades();
    }

    /** Todos los pedidos del escenario deben haber terminado (no solo el ganador). */
    private boolean conflictosCompletados() {
        return pedidoService.listarPedidos().stream()
                .filter(pedido -> pedido.getId().startsWith("CONF-"))
                .allMatch(pedido -> pedido.getEstado() == EstadoPedido.COMPLETADO);
    }

    private void esperarHasta(BooleanSupplier condicion, long timeoutMs) throws InterruptedException {
        long limite = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < limite) {
            if (condicion.getAsBoolean()) {
                return;
            }
            Thread.sleep(10);
        }
        fail("Tiempo de espera agotado esperando la condición del escenario");
    }
}
