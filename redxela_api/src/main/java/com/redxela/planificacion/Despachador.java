package com.redxela.planificacion;

import com.redxela.dominio.EstadoPedido;
import com.redxela.dominio.Pedido;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Despachador de procesos (Dispatcher) y planificador a corto plazo del centro de distribución.
 * 
 * Concepto de SO: Despachador de la CPU (Short-Term Scheduler / Dispatcher).
 * Es el único componente del sistema con autoridad para extraer un proceso de la cola de listos
 * (Ready Queue) y transicionarlo hacia el estado de ejecución activa (Running State).
 * Soporta la sustitución dinámica de la política de planificación en tiempo de ejecución.
 */
@Service
public class Despachador {

    private final ListaEspera listaEspera;
    private final Map<TipoPolitica, PoliticaPlanificacion> catalogoPoliticas = new ConcurrentHashMap<>();
    private final AtomicReference<PoliticaPlanificacion> politicaActiva = new AtomicReference<>();

    public Despachador(ListaEspera listaEspera, List<PoliticaPlanificacion> politicas) {
        this.listaEspera = listaEspera;
        for (PoliticaPlanificacion pol : politicas) {
            catalogoPoliticas.put(pol.getTipo(), pol);
        }
        // Política por defecto: Prioridad con Envejecimiento (evita inanición de niveles 4 y 5)
        PoliticaPlanificacion inicial = catalogoPoliticas.getOrDefault(
                TipoPolitica.PRIORIDAD_CON_ENVEJECIMIENTO,
                politicas.isEmpty() ? null : politicas.getFirst()
        );
        this.politicaActiva.set(inicial);
    }

    public void cambiarPolitica(TipoPolitica nuevoTipo) {
        PoliticaPlanificacion seleccionada = catalogoPoliticas.get(nuevoTipo);
        if (seleccionada == null) {
            throw new IllegalArgumentException("Política no reconocida: " + nuevoTipo);
        }
        this.politicaActiva.set(seleccionada);
    }

    public PoliticaPlanificacion getPoliticaActiva() {
        return politicaActiva.get();
    }

    public TipoPolitica getTipoPoliticaActiva() {
        return politicaActiva.get().getTipo();
    }

    /**
     * Único punto de salida de la lista de espera.
     */
    public Optional<Pedido> despacharSiguiente(long tiempoActualMs) {
        PoliticaPlanificacion politica = politicaActiva.get();
        Optional<Pedido> siguiente = listaEspera.extraerSiguiente(politica, tiempoActualMs);
        siguiente.ifPresent(pedido -> pedido.setEstado(EstadoPedido.EN_ESPERA_RECURSOS));
        return siguiente;
    }

    public List<ElementoListaEspera> obtenerVistaListaEspera(long tiempoActualMs) {
        return listaEspera.obtenerVistaOrdenada(politicaActiva.get(), tiempoActualMs);
    }

    public ListaEspera getListaEspera() {
        return listaEspera;
    }
}
