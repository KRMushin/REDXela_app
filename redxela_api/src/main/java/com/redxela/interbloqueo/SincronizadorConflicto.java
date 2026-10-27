package com.redxela.interbloqueo;

import com.redxela.recursos.GestorRecursos;
import com.redxela.recursos.ModoPrevencion;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Punto de sincronización para que el escenario de conflicto sea determinista.
 * 
 * Barrera de sincronización (CyclicBarrier). Obliga a que todos los
 * procesos del escenario hayan adquirido su primer recurso antes de que cualquiera
 * intente el segundo; así la espera circular se produce siempre, no por casualidad.
 * La barrera solo se activa en modo SIN_PREVENCION; con prevención se omite.
 */
@Component
public class SincronizadorConflicto {

    private static final long ESPERA_BARRERA_SEGUNDOS = 5;

    private final GestorRecursos gestorRecursos;
    private final Set<String> pedidosDelEscenario = ConcurrentHashMap.newKeySet();
    private final ReentrantLock cerrojo = new ReentrantLock();
    private volatile CyclicBarrier barrera;

    public SincronizadorConflicto(GestorRecursos gestorRecursos) {
        this.gestorRecursos = gestorRecursos;
    }

    /**
     * Arma la barrera para los pedidos que participarán del escenario.
     */
    public void preparar(Collection<String> pedidos, int participantes) {
        cerrojo.lock();
        try {
            pedidosDelEscenario.clear();
            pedidosDelEscenario.addAll(pedidos);
            barrera = new CyclicBarrier(participantes);
        } finally {
            cerrojo.unlock();
        }
    }

    /**
     * Bloquea al proceso justo después de adquirir su primer recurso hasta que todos
     * los participantes estén en la misma posición.
     */
    public void esperarTrasPrimerRecurso(String pedidoId, int recursosAdquiridos)
            throws InterruptedException, BrokenBarrierException, TimeoutException {
        CyclicBarrier barreraActual = barrera;
        if (barreraActual != null
                && recursosAdquiridos == 1
                && pedidosDelEscenario.contains(pedidoId)
                && gestorRecursos.getModo() == ModoPrevencion.SIN_PREVENCION) {
            barreraActual.await(ESPERA_BARRERA_SEGUNDOS, TimeUnit.SECONDS);
        }
    }

    public void limpiar() {
        cerrojo.lock();
        try {
            pedidosDelEscenario.clear();
            barrera = null;
        } finally {
            cerrojo.unlock();
        }
    }
}
