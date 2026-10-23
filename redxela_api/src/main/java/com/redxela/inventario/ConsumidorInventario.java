package com.redxela.inventario;

import com.redxela.almacen.AlmacenLlenoException;
import com.redxela.recepcion.AreaRecepcion;
import com.redxela.recepcion.MensajeLlegada;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hilo consumidor del área de Inventario.
 * 
 * Concepto de SO: Consumidor del patrón Productor-Consumidor sobre un buzón acotado.
 * Se bloquea en {@code take()} cuando no hay mensajes (estado Waiting) y se reactiva
 * cuando Recepción publica uno. Inventario procesa a su propio ritmo, desacoplado de
 * la velocidad de Recepción, sin pérdida de datos por la contrapresión del buzón.
 */
@Component
public class ConsumidorInventario {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorInventario.class);

    private final AreaRecepcion areaRecepcion;
    private final ServicioInventario servicioInventario;
    private final ExecutorService ejecutor = Executors.newSingleThreadExecutor(tarea -> {
        Thread hilo = new Thread(tarea, "consumidor-inventario");
        hilo.setDaemon(true); // no impide el apagado del proceso
        return hilo;
    });
    private final AtomicBoolean activo = new AtomicBoolean(false);

    public ConsumidorInventario(AreaRecepcion areaRecepcion, ServicioInventario servicioInventario) {
        this.areaRecepcion = areaRecepcion;
        this.servicioInventario = servicioInventario;
    }

    @PostConstruct
    public void iniciar() {
        if (activo.compareAndSet(false, true)) {
            ejecutor.submit(this::bucleConsumo);
        }
    }

    /**
     * Ciclo principal del consumidor: toma un mensaje y lo procesa.
     * Un lote fallido no detiene al consumidor; el error queda visible.
     */
    private void bucleConsumo() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                MensajeLlegada mensaje = areaRecepcion.tomar(); // take() bloqueante
                servicioInventario.procesarMensaje(mensaje);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // restaura la bandera de interrupción
                return;
            } catch (AlmacenLlenoException e) {
                servicioInventario.registrarError(e.getMessage()); // espacio insuficiente: lote pendiente
                log.warn("Lote no almacenado: {}", e.getMessage());
            } catch (RuntimeException e) {
                servicioInventario.registrarError(e.getMessage());
                log.error("Error procesando mensaje de recepción", e);
            }
        }
    }

    @PreDestroy
    public void detener() {
        activo.set(false);
        ejecutor.shutdownNow();
        try {
            if (!ejecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                log.warn("El consumidor de inventario no terminó a tiempo");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restaura la bandera de interrupción
        }
    }

    public boolean estaActivo() {
        return activo.get();
    }
}
