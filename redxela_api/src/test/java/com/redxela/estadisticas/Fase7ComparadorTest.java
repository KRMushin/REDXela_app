package com.redxela.estadisticas;

import com.redxela.api.dto.ComparacionPoliticas;
import com.redxela.api.dto.ComparacionRequest;
import com.redxela.dominio.PedidoService;
import com.redxela.planificacion.Despachador;
import com.redxela.planificacion.TipoPolitica;
import com.redxela.simulacion.ServicioReinicio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Aceptación de FASE 7A: comparación reproducible y aislada de las tres políticas.
 *
 * Concepto de SO: Benchmark de planificadores con la misma carga y semilla, sin
 * modificar la cola de listos, la política activa ni los procesos de la simulación viva.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Fase7ComparadorTest {

    @Autowired private ComparadorPoliticas comparador;
    @Autowired private PedidoService pedidos;
    @Autowired private Despachador despachador;
    @Autowired private ServicioReinicio reinicio;
    @Autowired private MockMvc mockMvc;

    @BeforeEach
    @AfterEach
    void limpiar() {
        reinicio.reiniciar();
    }

    @Test
    @DisplayName("La misma semilla produce la misma tabla bajo las tres políticas")
    void comparacionEsDeterministaYAislada() {
        TipoPolitica politicaAntes = despachador.getTipoPoliticaActiva();
        int pedidosAntes = pedidos.listarPedidos().size();

        ComparacionPoliticas primera = comparador.comparar(new ComparacionRequest(40, 98765L));
        ComparacionPoliticas segunda = comparador.comparar(new ComparacionRequest(40, 98765L));

        assertEquals(3, primera.resultados().size());
        assertEquals(ListaPoliticas.ESPERADAS, primera.resultados().stream()
                .map(ComparacionPoliticas.ResultadoPolitica::politica)
                .toList());
        assertEquals(primera.resultados(), segunda.resultados());
        assertEquals(politicaAntes, despachador.getTipoPoliticaActiva());
        assertEquals(pedidosAntes, pedidos.listarPedidos().size());

        for (ComparacionPoliticas.ResultadoPolitica resultado : primera.resultados()) {
            assertEquals(40, resultado.pedidosCompletados());
            assertEquals(5, resultado.porNivel().size());
            assertEquals(40, resultado.porNivel().stream()
                    .mapToInt(ComparacionPoliticas.MetricaNivel::pedidos)
                    .sum());
            assertTrue(resultado.tiempoEsperaPromedioMinutos() >= 0);
            assertTrue(resultado.esperaMaximaMinutos() >= resultado.tiempoEsperaPromedioMinutos());
            assertTrue(resultado.tasaThroughputPorMinuto() > 0);
        }
    }

    @Test
    @DisplayName("Los endpoints lanzan la comparación y consultan el último resultado")
    void endpointsDeComparacion() throws Exception {
        mockMvc.perform(post("/api/comparador/politicas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"semilla\":1234,\"cantidadPedidos\":18}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semilla").value(1234))
                .andExpect(jsonPath("$.cantidadPedidos").value(18))
                .andExpect(jsonPath("$.resultados.length()").value(3))
                .andExpect(jsonPath("$.resultados[0].politica").value("PRIORIDAD_ESTRICTA"))
                .andExpect(jsonPath("$.resultados[0].porNivel.length()").value(5))
                .andExpect(jsonPath("$.resultados[0].cumplimientoSlaPorcentaje").isNumber())
                .andExpect(jsonPath("$.resultados[0].tasaThroughputPorMinuto").isNumber());

        mockMvc.perform(get("/api/comparador/politicas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semilla").value(1234))
                .andExpect(jsonPath("$.cantidadPedidos").value(18))
                .andExpect(jsonPath("$.resultados.length()").value(3));
    }

    /** Orden estable de presentación de la tabla comparativa. */
    private static final class ListaPoliticas {
        private static final java.util.List<TipoPolitica> ESPERADAS = java.util.List.of(
                TipoPolitica.PRIORIDAD_ESTRICTA,
                TipoPolitica.PRIORIDAD_CON_ENVEJECIMIENTO,
                TipoPolitica.MENOR_CANTIDAD_PRIMERO
        );

        private ListaPoliticas() {}
    }
}
