package com.redxela.recursos;

import com.redxela.dominio.Pedido;
import com.redxela.dominio.TipoRecurso;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Administrador concurrente de un pool de recursos finitos del centro de distribución.
 * 
 * Concepto de SO: Semáforo contador con política de atención justa (FIFO / Fair Semaphore).
 * Regula el acceso concurrente a múltiples unidades de un mismo tipo de recurso y mantiene
 * la contabilidad de procesos retenedores y procesos en cola de bloqueo.
 */
public class PoolRecursos {

    private static final long TIMEOUT_INTENTO_MS = 50;

    private final TipoRecurso tipo;
    private final int totalUnidades;
    private final Semaphore semaforo;

    // Colecciones seguras para subprocesos para auditar el grafo de asignación
    private final Set<String> retenedores = ConcurrentHashMap.newKeySet();
    private final Queue<String> enEspera = new ConcurrentLinkedQueue<>();

    public PoolRecursos(TipoRecurso tipo, int totalUnidades) {
        if (totalUnidades <= 0) {
            throw new IllegalArgumentException("El número de unidades debe ser mayor a 0");
        }
        this.tipo = tipo;
        this.totalUnidades = totalUnidades;
        // Semáforo con equidad (fair=true) para garantizar orden FIFO estricto y evitar inanición
        this.semaforo = new Semaphore(totalUnidades, true);
    }

    /**
     * Adquiere una unidad del recurso para el pedido.
     * Utiliza sondeo periódico con timeout corto para permitir la expropiación por interrupción.
     */
    public void adquirir(Pedido pedido) throws InterruptedException {
        String idPedido = pedido.getId();
        enEspera.add(idPedido);
        pedido.setRecursoEsperado(this.tipo);

        try {
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Hilo interrumpido mientras esperaba: " + tipo.getNombre());
                }

                // Intento no bloqueante indefinido: permite reaccionar ante señales de cancelación
                if (semaforo.tryAcquire(TIMEOUT_INTENTO_MS, TimeUnit.MILLISECONDS)) {
                    // Éxito: transferencia atómica de lista de espera a retenedores
                    enEspera.remove(idPedido);
                    retenedores.add(idPedido);
                    pedido.agregarRecursoRetenido(this.tipo);
                    pedido.setRecursoEsperado(null);
                    return;
                }
            }
        } catch (InterruptedException e) {
            enEspera.remove(idPedido);
            pedido.setRecursoEsperado(null);
            Thread.currentThread().interrupt(); // Preserva la bandera de interrupción
            throw e;
        }
    }

    /**
     * Libera una unidad del recurso previamente asignada al pedido.
     * Es idempotente: solo incrementa el semáforo si el pedido efectivamente retenía la unidad.
     */
    public void liberar(Pedido pedido) {
        String idPedido = pedido.getId();
        // Sincronización segura: solo se retorna el permiso si el id estaba registrado en retenedores
        if (retenedores.remove(idPedido)) {
            pedido.removerRecursoRetenido(this.tipo);
            semaforo.release();
        }
    }

    /**
     * Genera una fotografía instantánea del estado del recurso para monitoreo.
     */
    public EstadoRecurso estado() {
        int disponibles = semaforo.availablePermits();
        int enUso = totalUnidades - disponibles;
        return new EstadoRecurso(
                tipo,
                tipo.getNombre(),
                totalUnidades,
                disponibles,
                Math.max(0, enUso),
                new ArrayList<>(retenedores),
                new ArrayList<>(enEspera)
        );
    }

    public TipoRecurso getTipo() { return tipo; }
    public int getTotalUnidades() { return totalUnidades; }
    public int getDisponibles() { return semaforo.availablePermits(); }
}
