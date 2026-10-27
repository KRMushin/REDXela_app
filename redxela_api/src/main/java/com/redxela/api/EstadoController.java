package com.redxela.api;

import com.redxela.api.dto.SnapshotEstado;
import com.redxela.estado.PublicadorEstado;
import com.redxela.estado.ServicioSnapshot;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Expone el snapshot de estado por SSE y por polling de respaldo.
 * Canal de telemetría del sistema (procfs / dmesg). El stream SSE
 * empuja el estado a los observadores y el endpoint REST ofrece la misma foto
 * bajo demanda para clientes que no soportan eventos.
 */
@RestController
public class EstadoController {

    private final ServicioSnapshot servicioSnapshot;
    private final PublicadorEstado publicador;

    public EstadoController(ServicioSnapshot servicioSnapshot, PublicadorEstado publicador) {
        this.servicioSnapshot = servicioSnapshot;
        this.publicador = publicador;
    }

    @GetMapping(value = "/api/estado/snapshot", produces = MediaType.APPLICATION_JSON_VALUE)
    public SnapshotEstado snapshot() {
        return servicioSnapshot.construir();
    }

    @GetMapping(value = "/api/stream/estado", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return publicador.suscribir();
    }
}
