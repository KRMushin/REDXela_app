package com.redxela.estado;

import com.redxela.api.dto.SnapshotEstado;
import com.redxela.simulacion.ServicioReinicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Aceptación del canal SSE de FASE 6A.
 *
 * Concepto de SO: Comunicación asíncrona servidor-cliente (push de telemetría);
 * el cliente queda suscrito y recibe snapshots sin realizar polling.
 */
@SpringBootTest(properties = {
        "simulacion.time-scale-ms=20",
        "simulacion.publicacion-estado-ms=100",
        "simulacion.tasa-generador-pedidos-minuto=1"
})
@AutoConfigureMockMvc
class Fase6StreamingTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private PublicadorEstado publicador;
    @Autowired private ServicioReinicio reinicio;

    @BeforeEach
    void preparar() {
        reinicio.reiniciar();
        publicador.completarTodos();
    }

    @Test
    @DisplayName("El publicador difunde el snapshot a cada suscriptor")
    void publicadorDifundeSnapshot() {
        EmisorCaptura emisor = new EmisorCaptura();
        publicador.registrar(emisor);

        publicador.publicarAhora();

        assertInstanceOf(SnapshotEstado.class, emisor.dato.get());
        assertEquals(1, publicador.contarSuscriptores());

        publicador.completarTodos();
        assertEquals(0, publicador.contarSuscriptores());
    }

    @Test
    @DisplayName("GET /api/stream/estado abre un canal text/event-stream")
    void endpointSseAbreCanal() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/stream/estado"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        String tipo = resultado.getResponse().getContentType();
        assertNotNull(tipo);
        assertTrue(tipo.startsWith(MediaType.TEXT_EVENT_STREAM_VALUE));
        assertTrue(publicador.contarSuscriptores() >= 1);

        publicador.completarTodos();
    }

    /** Emisor de prueba que captura el objeto enviado sin abrir una conexión HTTP. */
    private static final class EmisorCaptura extends SseEmitter {
        private final AtomicReference<Object> dato = new AtomicReference<>();

        @Override
        public void send(Object object) throws IOException {
            dato.set(object);
        }
    }
}
