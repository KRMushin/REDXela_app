package com.redxela.recursos;

import com.redxela.dominio.Pedido;
import com.redxela.dominio.TipoRecurso;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Sistema de escaneo compartido entre el área de Recepción y la línea de Despacho.
 * 
 * Concepto de SO: Sección Crítica y Exclusión Mutua con Cerrojo Justo (Fair Mutex Lock).
 * Un único dispositivo compartido donde solo un hilo (pedido en empaque o proceso de recepción)
 * puede acceder simultáneamente, garantizando orden FIFO y ausencia de inversión de prioridades.
 */
@Component
public class Escaner {

    public static final String IDENTIFICADOR_RECEPCION = "RECEPCION";
    private static final long TIMEOUT_INTENTO_MS = 50;

    // Mutex justo (fairness=true) para evitar inanición entre recepción y pedidos
    private final ReentrantLock cerrojo = new ReentrantLock(true);
    private final Queue<String> colaEspera = new ConcurrentLinkedQueue<>();
    private volatile String retenedorActual = null;

    /**
     * Adquiere el escáner para un pedido específico.
     */
    public void adquirir(Pedido pedido) throws InterruptedException {
        adquirirGenerico(pedido.getId());
        pedido.agregarRecursoRetenido(TipoRecurso.ESCANER);
        pedido.setRecursoEsperado(null);
    }

    /**
     * Libera el escáner retenido por un pedido.
     */
    public void liberar(Pedido pedido) {
        if (liberarGenerico(pedido.getId())) {
            pedido.removerRecursoRetenido(TipoRecurso.ESCANER);
        }
    }

    /**
     * Adquiere el escáner para la descarga de lotes en Recepción.
     */
    public void adquirirPorRecepcion() throws InterruptedException {
        adquirirGenerico(IDENTIFICADOR_RECEPCION);
    }

    /**
     * Libera el escáner utilizado por Recepción.
     */
    public void liberarPorRecepcion() {
        liberarGenerico(IDENTIFICADOR_RECEPCION);
    }

    private void adquirirGenerico(String solicitanteId) throws InterruptedException {
        colaEspera.add(solicitanteId);
        try {
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Interrumpido mientras esperaba el escáner");
                }
                if (cerrojo.tryLock(TIMEOUT_INTENTO_MS, TimeUnit.MILLISECONDS)) {
                    colaEspera.remove(solicitanteId);
                    retenedorActual = solicitanteId;
                    return;
                }
            }
        } catch (InterruptedException e) {
            colaEspera.remove(solicitanteId);
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    private boolean liberarGenerico(String solicitanteId) {
        // Exclusión mutua garantizada: solo quien retiene el cerrojo puede liberarlo
        if (cerrojo.isHeldByCurrentThread() && solicitanteId.equals(retenedorActual)) {
            retenedorActual = null;
            cerrojo.unlock();
            return true;
        }
        return false;
    }

    public EstadoRecurso estado() {
        boolean ocupado = cerrojo.isLocked();
        List<String> asignados = (ocupado && retenedorActual != null)
                ? List.of(retenedorActual)
                : Collections.emptyList();

        return new EstadoRecurso(
                TipoRecurso.ESCANER,
                TipoRecurso.ESCANER.getNombre(),
                1,
                ocupado ? 0 : 1,
                ocupado ? 1 : 0,
                asignados,
                new ArrayList<>(colaEspera)
        );
    }

    public boolean estaEnUso() { return cerrojo.isLocked(); }
    public String getRetenedorActual() { return retenedorActual; }
}
