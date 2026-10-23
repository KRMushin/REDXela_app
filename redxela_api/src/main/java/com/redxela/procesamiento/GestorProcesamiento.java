package com.redxela.procesamiento;

import com.redxela.dominio.Pedido;
import com.redxela.recursos.GestorRecursos;
import com.redxela.simulacion.RelojSimulacion;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Gestor del pool de hilos de procesamiento de pedidos.
 * 
 * Concepto de SO: Planificador de Hilos y Manejador de Procesos Activos (Process Manager).
 * Despacha hilos de ejecución en el pool y mantiene los manejadores (descriptores de tarea)
 * que permiten enviar señales de interrupción (SIGINT / expropiación) para resolución de bloqueos.
 */
@Service
public class GestorProcesamiento {

    private final GestorRecursos gestorRecursos;
    private final RelojSimulacion reloj;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final Map<String, Future<?>> tareasActivas = new ConcurrentHashMap<>();

    public GestorProcesamiento(GestorRecursos gestorRecursos, RelojSimulacion reloj) {
        this.gestorRecursos = gestorRecursos;
        this.reloj = reloj;
    }

    public Future<?> ejecutar(Pedido pedido) {
        ProcesadorPedido procesador = new ProcesadorPedido(pedido, gestorRecursos, reloj);
        Future<?> future = threadPool.submit(() -> {
            try {
                procesador.run();
            } finally {
                tareasActivas.remove(pedido.getId());
            }
        });
        tareasActivas.put(pedido.getId(), future);
        return future;
    }

    public boolean interrumpirPedido(String pedidoId) {
        Future<?> future = tareasActivas.get(pedidoId);
        if (future != null && !future.isDone()) {
            return future.cancel(true); // Envía interrupción al hilo en ejecución
        }
        return false;
    }

    public int totalTareasActivas() {
        return tareasActivas.size();
    }

    @PreDestroy
    public void cerrar() {
        threadPool.shutdownNow();
    }
}
