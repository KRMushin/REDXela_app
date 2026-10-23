package com.redxela.dominio;

import com.redxela.inventario.DepositoExistencias;
import com.redxela.planificacion.Despachador;
import com.redxela.planificacion.ListaEspera;
import com.redxela.procesamiento.GestorProcesamiento;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Servicio de coordinación del ciclo de vida de los pedidos.
 * 
 * Concepto de SO: Planificador a Largo Plazo y Gestor de Procesos (Long-Term Scheduler / Job Scheduler).
 * Admite nuevos procesos, crea su PCB y decide si pasan a la cola de listos o quedan
 * bloqueados en EN_ESPERA_STOCK. La reserva de existencias es atómica: el pedido solo
 * entra a la lista de espera si obtuvo todos sus productos.
 */
@Service
public class PedidoService {

    private final Map<String, Pedido> registroPedidos = new ConcurrentHashMap<>();
    // Procesos bloqueados esperando E/S de abastecimiento (stock)
    private final Map<String, Pedido> pedidosEnEsperaStock = new ConcurrentHashMap<>();
    private final AtomicLong secuencialPedido = new AtomicLong(100);
    // Evita que dos eventos de llegada reevalúen la misma cola al mismo tiempo
    private final ReentrantLock cerrojoReevaluacion = new ReentrantLock(true);

    private final ListaEspera listaEspera;
    private final Despachador despachador;
    private final GestorProcesamiento gestorProcesamiento;
    private final DepositoExistencias deposito;

    public PedidoService(
            ListaEspera listaEspera,
            Despachador despachador,
            GestorProcesamiento gestorProcesamiento,
            DepositoExistencias deposito
    ) {
        this.listaEspera = listaEspera;
        this.despachador = despachador;
        this.gestorProcesamiento = gestorProcesamiento;
        this.deposito = deposito;
    }

    public Pedido registrarNuevoPedido(Cliente cliente, List<ItemPedido> items) {
        String id = String.format("PED-%d", secuencialPedido.incrementAndGet());
        Pedido pedido = new Pedido(id, cliente, items); // nace en EN_ESPERA_STOCK
        registroPedidos.put(id, pedido);

        if (deposito.reservar(pedido)) {
            listaEspera.agregar(pedido); // pasa a EN_ESPERA_RECURSOS
            intentarDespachar();
        } else {
            pedidosEnEsperaStock.put(id, pedido); // bloqueado por falta de existencias
        }
        return pedido;
    }

    /**
     * Reintenta reservar stock para los pedidos detenidos. Se invoca al recibir
     * mercadería (evento de Inventario). El cerrojo serializa las reevaluaciones.
     */
    public void reintentarPedidosEnEsperaStock() {
        cerrojoReevaluacion.lock();
        try {
            for (Pedido pedido : new ArrayList<>(pedidosEnEsperaStock.values())) {
                if (!deposito.reservar(pedido)) {
                    continue; // aún sin existencias suficientes
                }
                if (pedidosEnEsperaStock.remove(pedido.getId()) != null) {
                    listaEspera.agregar(pedido);
                    intentarDespachar();
                } else {
                    deposito.devolver(pedido); // rollback si otro hilo ya lo tomó
                }
            }
        } finally {
            cerrojoReevaluacion.unlock();
        }
    }

    public void intentarDespachar() {
        long ahora = System.currentTimeMillis();
        // Intenta extraer el siguiente pedido listo y enviarlo al pool de hilos
        despachador.despacharSiguiente(ahora).ifPresent(gestorProcesamiento::ejecutar);
    }

    /**
     * Un proceso terminado libera la CPU: se intenta despachar al siguiente.
     * Los procesos expropiados no se atienden aquí; el resolutor los reencola.
     */
    public void despacharSiCompletado(String pedidoId) {
        Pedido pedido = registroPedidos.get(pedidoId);
        if (pedido == null || pedido.getEstado() == EstadoPedido.COMPLETADO) {
            intentarDespachar();
        }
    }

    public Optional<Pedido> buscarPorId(String id) {
        return Optional.ofNullable(registroPedidos.get(id));
    }

    public List<Pedido> listarPedidos() {
        return new ArrayList<>(registroPedidos.values());
    }

    /**
     * Registra un pedido ya construido (por ejemplo, los del escenario de conflicto)
     * sin pasar por la reserva de stock. Se usa para inyección controlada de fallas.
     */
    public void registrarPedidoExistente(Pedido pedido) {
        registroPedidos.put(pedido.getId(), pedido);
        if (pedido.getEstado() == EstadoPedido.EN_ESPERA_STOCK) {
            pedido.setEstado(EstadoPedido.EN_ESPERA_RECURSOS);
        }
    }

    /**
     * Devuelve un proceso expropiado a la cola de listos para que se atienda más adelante.
     */
    public boolean devolverAListaEspera(String pedidoId) {
        Pedido pedido = registroPedidos.get(pedidoId);
        if (pedido == null || pedido.getEstado() == EstadoPedido.COMPLETADO) {
            return false;
        }
        pedidosEnEsperaStock.remove(pedidoId);
        pedido.setProgreso(0);
        pedido.setTInicio(null);
        pedido.setTFin(null);
        pedido.setRecursoEsperado(null);
        listaEspera.agregar(pedido);
        return true;
    }

    public int pedidosEnEsperaStock() {
        return pedidosEnEsperaStock.size();
    }

    public void limpiarParaPruebas() {
        registroPedidos.clear();
        listaEspera.vaciar();
        pedidosEnEsperaStock.clear();
        secuencialPedido.set(100); // una simulación reiniciada vuelve a PED-101
    }
}
