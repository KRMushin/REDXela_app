package com.redxela.procesamiento;

import com.redxela.dominio.Pedido;
import com.redxela.interbloqueo.SincronizadorConflicto;
import com.redxela.recursos.GestorRecursos;
import com.redxela.simulacion.RelojSimulacion;
import jakarta.annotation.PreDestroy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Gestor del pool de hilos de procesamiento de pedidos.
 * 
 * Concepto de SO: Planificador de Hilos y Manejador de Procesos Activos (Process Manager).
 * Despacha hilos de ejecución y conserva un descriptor por tarea con un CountDownLatch
 * que permite confirmar cuándo el proceso expropiado terminó y liberó sus recursos.
 */
@Service
public class GestorProcesamiento {

    /** Descriptor de una tarea: su Future y la señal de finalización efectiva. */
    private record TareaActiva(Future<?> futuro, CountDownLatch finalizada) {}

    private final GestorRecursos gestorRecursos;
    private final RelojSimulacion reloj;
    private final SincronizadorConflicto sincronizador;
    private final ApplicationEventPublisher eventos;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final Map<String, TareaActiva> tareasActivas = new ConcurrentHashMap<>();

    public GestorProcesamiento(
            GestorRecursos gestorRecursos,
            RelojSimulacion reloj,
            SincronizadorConflicto sincronizador,
            ApplicationEventPublisher eventos
    ) {
        this.gestorRecursos = gestorRecursos;
        this.reloj = reloj;
        this.sincronizador = sincronizador;
        this.eventos = eventos;
    }

    public Future<?> ejecutar(Pedido pedido) {
        ProcesadorPedido procesador = new ProcesadorPedido(pedido, gestorRecursos, reloj, sincronizador);
        CountDownLatch finalizada = new CountDownLatch(1);

        FutureTask<Void> tarea = new FutureTask<>(() -> {
            try {
                procesador.run();
            } finally {
                tareasActivas.remove(pedido.getId());
                finalizada.countDown(); // el finally de ProcesadorPedido ya liberó recursos
                eventos.publishEvent(new PedidoFinalizadoEvent(pedido.getId()));
            }
            return null;
        });

        // Se registra antes de arrancar para evitar la carrera de terminación inmediata
        tareasActivas.put(pedido.getId(), new TareaActiva(tarea, finalizada));
        threadPool.execute(tarea);
        return tarea;
    }

    /**
     * Envía una interrupción (expropiación) al hilo que ejecuta el pedido.
     */
    public boolean interrumpirPedido(String pedidoId) {
        TareaActiva tarea = tareasActivas.get(pedidoId);
        return tarea != null && !tarea.futuro().isDone() && tarea.futuro().cancel(true);
    }

    /**
     * Espera a que la tarea haya salido de su bloque finally y liberado sus recursos.
     */
    public boolean esperarFinalizacion(String pedidoId, long timeoutMs) {
        TareaActiva tarea = tareasActivas.get(pedidoId);
        if (tarea == null) {
            return true;
        }
        try {
            return tarea.finalizada().await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restaura la bandera de interrupción
            return false;
        }
    }

    public int totalTareasActivas() {
        return tareasActivas.size();
    }

    /**
     * Interrumpe todas las tareas vivas y espera a que cada una libere sus recursos
     * en el bloque finally antes de reiniciar la simulación.
     */
    public void cancelarTodas() {
        List<String> ids = new ArrayList<>(tareasActivas.keySet());
        ids.forEach(this::interrumpirPedido);
        ids.forEach(id -> esperarFinalizacion(id, 1000));
    }

    @PreDestroy
    public void cerrar() {
        threadPool.shutdownNow();
    }
}
