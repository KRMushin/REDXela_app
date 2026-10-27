package com.redxela.dominio;

import com.redxela.inventario.DepositoExistencias;
import com.redxela.planificacion.Despachador;
import com.redxela.planificacion.ListaEspera;
import com.redxela.procesamiento.GestorProcesamiento;
import com.redxela.procesamiento.ProcesadorPedido;
import com.redxela.recursos.EstadoRecurso;
import com.redxela.recursos.GestorRecursos;
import com.redxela.simulacion.RelojSimulacion;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Servicio de coordinación del ciclo de vida de los pedidos.
 * 
 * Planificador a Largo Plazo y Gestor de Procesos (Long-Term Scheduler / Job Scheduler).
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
    private final GestorRecursos recursos;
    private final RelojSimulacion reloj;
    private final Map<String, List<TipoRecurso>> reservasRecursos = new HashMap<>();

    public PedidoService(
            ListaEspera listaEspera,
            Despachador despachador,
            GestorProcesamiento gestorProcesamiento,
            DepositoExistencias deposito,
            GestorRecursos recursos,
            RelojSimulacion reloj
    ) {
        this.listaEspera = listaEspera;
        this.despachador = despachador;
        this.gestorProcesamiento = gestorProcesamiento;
        this.deposito = deposito;
        this.recursos = recursos;
        this.reloj = reloj;
    }

    public Pedido registrarNuevoPedido(Cliente cliente, List<ItemPedido> items) {
        cerrojoReevaluacion.lock();
        try {
            String id = String.format("PED-%d", secuencialPedido.incrementAndGet());
            Pedido pedido = new Pedido(id, cliente, items);
            pedido.setEscalaTiempoMs(reloj.getTimeScaleMs());
            registroPedidos.put(id, pedido);
            if (deposito.reservar(pedido)) {
                pedido.setStockReservado(true);
                listaEspera.agregar(pedido);
                intentarDespachar();
            } else {
                pedidosEnEsperaStock.put(id, pedido);
            }
            return pedido;
        } finally {
            cerrojoReevaluacion.unlock();
        }
    }

    /**
     * Reintenta reservar stock para los pedidos detenidos. Se invoca al recibir
     * mercadería (evento de Inventario). El cerrojo serializa las reevaluaciones.
     */
    public void reintentarPedidosEnEsperaStock() {
        cerrojoReevaluacion.lock();
        try {
            List<Pedido> pendientes = new ArrayList<>(pedidosEnEsperaStock.values());
            pendientes.sort(despachador.getPoliticaActiva().obtenerComparador(System.currentTimeMillis()));
            for (Pedido pedido : pendientes) {
                if (!deposito.reservar(pedido)) {
                    continue; // aún sin existencias suficientes
                }
                if (pedidosEnEsperaStock.remove(pedido.getId()) != null) {
                    pedido.setStockReservado(true);
                    listaEspera.agregar(pedido);
                } else {
                    deposito.devolver(pedido); // rollback si otro hilo ya lo tomó
                }
            }
            intentarDespachar();
        } finally {
            cerrojoReevaluacion.unlock();
        }
    }

    public synchronized void intentarDespachar() {
        while (true) {
            long ahora = System.currentTimeMillis();
            var ordenados = despachador.obtenerVistaListaEspera(ahora);
            if (ordenados.isEmpty()) return;
            Pedido candidato = listaEspera.obtener(ordenados.getFirst().pedidoId()).orElse(null);
            if (candidato == null) continue;
            List<TipoRecurso> necesarios = ProcesadorPedido.recopilarRecursosRequeridos(candidato);
            List<EstadoRecurso> estados = recursos.obtenerEstados();
            boolean disponibles = necesarios.stream().allMatch(tipo -> {
                EstadoRecurso estado = estados.stream().filter(e -> e.tipo() == tipo).findFirst().orElseThrow();
                long reservados = reservasRecursos.values().stream().filter(lista -> lista.contains(tipo)).count();
                long externos = estado.pedidosAsignados().stream()
                        .filter(id -> !reservasRecursos.containsKey(id)).count();
                int sinIdentificar = Math.max(0, estado.enUso() - estado.pedidosAsignados().size());
                return reservados + externos + sinIdentificar < estado.total();
            });
            if (!disponibles) return;
            Optional<Pedido> elegido = despachador.despacharSiguiente(ahora);
            if (elegido.isEmpty()) continue;
            Pedido pedido = elegido.get();
            if (pedido != candidato) {
                listaEspera.agregar(pedido);
                continue;
            }
            reservasRecursos.put(pedido.getId(), necesarios);
            try {
                gestorProcesamiento.ejecutar(pedido);
            } catch (RuntimeException e) {
                reservasRecursos.remove(pedido.getId());
                listaEspera.agregar(pedido);
                throw e;
            }
        }
    }

    /**
     * Un proceso terminado libera la CPU: se intenta despachar al siguiente.
     * Los procesos expropiados no se atienden aquí; el resolutor los reencola.
     */
    public synchronized void despacharSiCompletado(String pedidoId) {
        reservasRecursos.remove(pedidoId);
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
        pedido.setEscalaTiempoMs(reloj.getTimeScaleMs());
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

    public synchronized void limpiarParaPruebas() {
        reservasRecursos.clear();
        registroPedidos.clear();
        listaEspera.vaciar();
        pedidosEnEsperaStock.clear();
        secuencialPedido.set(100); // una simulación reiniciada vuelve a PED-101
    }
}
