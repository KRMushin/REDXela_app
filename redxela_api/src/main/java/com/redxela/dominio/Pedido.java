package com.redxela.dominio;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloque de Control de Proceso (PCB) de un pedido en el sistema REDXela.
 * 
 * Concepto de SO: Process Control Block (PCB). Almacena el identificador del proceso (PID),
 * su estado actual, prioridad, tiempos de creación/inicio/fin, progreso del ciclo de ejecución
 * y la lista de descriptores de recursos que retiene y por el que se encuentra bloqueado.
 */
public class Pedido {

    private final String id;
    private final Cliente cliente;
    private final NivelServicio nivel;
    private final List<ItemPedido> items;
    private final int totalArticulos;
    private final long tCreacion;

    private volatile EstadoPedido estado;
    private volatile Long tInicio;
    private volatile Long tFin;
    private volatile int progreso; // Porcentaje de 0 a 100
    private volatile TipoRecurso recursoEsperado;

    // Estructura concurrente para rastrear recursos en posesión (evita condiciones de carrera)
    private final Set<TipoRecurso> recursosRetenidos = ConcurrentHashMap.newKeySet();

    public Pedido(String id, Cliente cliente, List<ItemPedido> items) {
        this(id, cliente, items, System.currentTimeMillis());
    }

    /**
     * Constructor con instante de creación explícito para simulaciones deterministas.
     */
    public Pedido(String id, Cliente cliente, List<ItemPedido> items, long tCreacion) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("El id es obligatorio");
        if (cliente == null) throw new IllegalArgumentException("El cliente es obligatorio");
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Debe contener artículos");

        this.id = id;
        this.cliente = cliente;
        this.nivel = cliente.nivelServicio();
        this.items = List.copyOf(items);
        this.totalArticulos = items.stream().mapToInt(ItemPedido::cantidad).sum();
        this.tCreacion = tCreacion;
        this.estado = EstadoPedido.EN_ESPERA_STOCK;
        this.progreso = 0;
    }

    public String getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public NivelServicio getNivel() { return nivel; }
    public List<ItemPedido> getItems() { return items; }
    public int getTotalArticulos() { return totalArticulos; }
    public long getTCreacion() { return tCreacion; }

    public EstadoPedido getEstado() { return estado; }
    public void setEstado(EstadoPedido estado) { this.estado = estado; }

    public Long getTInicio() { return tInicio; }
    public void setTInicio(Long tInicio) { this.tInicio = tInicio; }

    public Long getTFin() { return tFin; }
    public void setTFin(Long tFin) { this.tFin = tFin; }

    public int getProgreso() { return progreso; }
    public void setProgreso(int progreso) { this.progreso = Math.min(100, Math.max(0, progreso)); }

    public TipoRecurso getRecursoEsperado() { return recursoEsperado; }
    public void setRecursoEsperado(TipoRecurso recurso) { this.recursoEsperado = recurso; }

    public Set<TipoRecurso> getRecursosRetenidos() {
        return Collections.unmodifiableSet(recursosRetenidos);
    }

    public void agregarRecursoRetenido(TipoRecurso recurso) {
        this.recursosRetenidos.add(recurso);
    }

    public void removerRecursoRetenido(TipoRecurso recurso) {
        this.recursosRetenidos.remove(recurso);
    }

    public void liberarTodosLosRecursos() {
        this.recursosRetenidos.clear();
        this.recursoEsperado = null;
    }
}
