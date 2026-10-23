package com.redxela.dominio;

import com.redxela.planificacion.Despachador;
import com.redxela.planificacion.ListaEspera;
import com.redxela.procesamiento.GestorProcesamiento;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servicio de coordinación del ciclo de vida de los pedidos.
 * 
 * Concepto de SO: Planificador a Largo Plazo y Gestor de Procesos (Long-Term Scheduler / Job Scheduler).
 * Admite nuevos procesos en el sistema, crea sus PCB, los ingresa a la Ready Queue y
 * coordina la asignación inicial de CPU/hilos de trabajo.
 */
@Service
public class PedidoService {

    private final Map<String, Pedido> registroPedidos = new ConcurrentHashMap<>();
    private final AtomicLong secuencialPedido = new AtomicLong(100);

    private final ListaEspera listaEspera;
    private final Despachador despachador;
    private final GestorProcesamiento gestorProcesamiento;

    public PedidoService(ListaEspera listaEspera, Despachador despachador, GestorProcesamiento gestorProcesamiento) {
        this.listaEspera = listaEspera;
        this.despachador = despachador;
        this.gestorProcesamiento = gestorProcesamiento;
    }

    public Pedido registrarNuevoPedido(Cliente cliente, List<ItemPedido> items) {
        String id = String.format("PED-%d", secuencialPedido.incrementAndGet());
        Pedido pedido = new Pedido(id, cliente, items);
        registroPedidos.put(id, pedido);

        // Se encola en la lista de espera para ser seleccionado por el despachador
        listaEspera.agregar(pedido);
        intentarDespachar();
        return pedido;
    }

    public void intentarDespachar() {
        long ahora = System.currentTimeMillis();
        // Intenta extraer el siguiente pedido listo y enviarlo al pool de hilos
        despachador.despacharSiguiente(ahora).ifPresent(gestorProcesamiento::ejecutar);
    }

    public Optional<Pedido> buscarPorId(String id) {
        return Optional.ofNullable(registroPedidos.get(id));
    }

    public List<Pedido> listarPedidos() {
        return new ArrayList<>(registroPedidos.values());
    }

    public void limpiarParaPruebas() {
        registroPedidos.clear();
        listaEspera.vaciar();
    }
}
