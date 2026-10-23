package com.redxela.estado;

import com.redxela.api.dto.SnapshotEstado;
import com.redxela.config.SimulacionProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Publica el snapshot completo cada 500 ms mediante Server-Sent Events.
 *
 * Concepto de SO: Difusión periódica de estado a observadores (Publish/Subscribe).
 * Un hilo publicador desacoplado envía telemetría a todos los clientes suscritos,
 * sin que los procesos del centro de distribución conozcan a los consumidores.
 */
@Component
public class PublicadorEstado {

    private static final Logger log = LoggerFactory.getLogger(PublicadorEstado.class);

    private final ServicioSnapshot servicioSnapshot;
    private final long intervaloMs;
    private final List<SseEmitter> emisores = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService publicador = Executors.newSingleThreadScheduledExecutor(tarea -> {
        Thread hilo = new Thread(tarea, "publicador-estado");
        hilo.setDaemon(true);
        return hilo;
    });

    public PublicadorEstado(ServicioSnapshot servicioSnapshot, SimulacionProperties properties) {
        this.servicioSnapshot = servicioSnapshot;
        this.intervaloMs = properties.publicacionEstadoMs() > 0
                ? properties.publicacionEstadoMs()
                : 500L;
    }

    @PostConstruct
    public void iniciar() {
        publicador.scheduleWithFixedDelay(this::publicarSeguro, intervaloMs, intervaloMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Abre un canal SSE y entrega inmediatamente el estado actual.
     */
    public SseEmitter suscribir() {
        SseEmitter emisor = new SseEmitter(0L);
        registrar(emisor);
        enviar(emisor, servicioSnapshot.construir());
        return emisor;
    }

    /**
     * Registra un emisor y limpia la suscripción cuando el cliente se desconecta.
     */
    public void registrar(SseEmitter emisor) {
        emisor.onCompletion(() -> emisores.remove(emisor));
        emisor.onTimeout(() -> emisores.remove(emisor));
        emisor.onError(error -> emisores.remove(emisor));
        emisores.add(emisor);
    }

    public void publicarAhora() {
        SnapshotEstado estado = servicioSnapshot.construir();
        emisores.forEach(emisor -> enviar(emisor, estado));
    }

    private void publicarSeguro() {
        try {
            publicarAhora();
        } catch (RuntimeException e) {
            // Un fallo de telemetría no debe detener la simulación ni el publicador
            log.error("Error al publicar el snapshot de estado", e);
        }
    }

    private void enviar(SseEmitter emisor, SnapshotEstado estado) {
        try {
            emisor.send(estado);
        } catch (IOException | IllegalStateException e) {
            emisores.remove(emisor);
            emisor.complete();
        }
    }

    public int contarSuscriptores() {
        return emisores.size();
    }

    public void completarTodos() {
        emisores.forEach(SseEmitter::complete);
        emisores.clear();
    }

    @PreDestroy
    public void cerrar() {
        publicador.shutdownNow();
        completarTodos();
    }
}
