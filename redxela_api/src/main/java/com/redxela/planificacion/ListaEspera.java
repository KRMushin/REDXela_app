package com.redxela.planificacion;

import com.redxela.dominio.EstadoPedido;
import com.redxela.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Estructura de almacenamiento concurrente y sin límite para los pedidos en espera de recursos.
 * 
 * Concepto de SO: Cola de Listos/Preparados (Ready Queue).
 * Almacena los PCB de los procesos que compiten por recursos físicos. La extracción está
 * protegida por cerrojo para evitar carreras críticas entre múltiples despachadores.
 */
@Component
public class ListaEspera {

    // Almacenamiento concurrente sin límite de capacidad
    private final Map<String, Pedido> pedidos = new ConcurrentHashMap<>();
    private final ReentrantLock cerrojoExtraccion = new ReentrantLock();

    public void agregar(Pedido pedido) {
        pedido.setEstado(EstadoPedido.EN_ESPERA_RECURSOS);
        pedidos.put(pedido.getId(), pedido);
    }

    public boolean remover(String pedidoId) {
        return pedidos.remove(pedidoId) != null;
    }

    public Optional<Pedido> obtener(String pedidoId) {
        return Optional.ofNullable(pedidos.get(pedidoId));
    }

    /**
     * Extrae de forma atómica el proceso con mayor prioridad según la política activa.
     */
    public Optional<Pedido> extraerSiguiente(PoliticaPlanificacion politica, long tiempoActualMs) {
        cerrojoExtraccion.lock();
        try {
            if (pedidos.isEmpty()) {
                return Optional.empty();
            }
            // Sincronización segura: selecciona y remueve atómicamente el mejor candidato
            Comparator<Pedido> comparador = politica.obtenerComparador(tiempoActualMs);
            Pedido mejor = pedidos.values().stream().min(comparador).orElse(null);
            if (mejor != null) {
                pedidos.remove(mejor.getId());
                return Optional.of(mejor);
            }
            return Optional.empty();
        } finally {
            cerrojoExtraccion.unlock();
        }
    }

    /**
     * Genera una vista ordenada instantánea con la justificación de turno de cada elemento.
     */
    public List<ElementoListaEspera> obtenerVistaOrdenada(PoliticaPlanificacion politica, long tiempoActualMs) {
        Comparator<Pedido> comparador = politica.obtenerComparador(tiempoActualMs);
        List<Pedido> ordenados = pedidos.values().stream()
                .sorted(comparador)
                .toList();

        List<ElementoListaEspera> resultado = new ArrayList<>(ordenados.size());
        for (int i = 0; i < ordenados.size(); i++) {
            Pedido p = ordenados.get(i);
            double esperaMin = Math.max(0.0, (tiempoActualMs - p.getTCreacion()) / (double) p.getEscalaTiempoMs());
            double prioridad = politica.calcularPrioridadEfectiva(p, tiempoActualMs);
            String motivo = politica.explicarPrioridad(p, tiempoActualMs);

            resultado.add(new ElementoListaEspera(
                    i + 1,
                    p.getId(),
                    p.getCliente().nombre(),
                    p.getNivel(),
                    p.getTotalArticulos(),
                    p.getTCreacion(),
                    esperaMin,
                    prioridad,
                    motivo
            ));
        }
        return Collections.unmodifiableList(resultado);
    }

    public int tamano() { return pedidos.size(); }
    public boolean estaVacia() { return pedidos.isEmpty(); }
    public void vaciar() { pedidos.clear(); }
}
