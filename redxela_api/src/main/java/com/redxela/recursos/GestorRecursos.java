package com.redxela.recursos;

import com.redxela.config.SimulacionProperties;
import com.redxela.dominio.Pedido;
import com.redxela.dominio.TipoRecurso;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gestor centralizado del subsistema de recursos del centro de distribución REDXela.
 * 
 * Concepto de SO: Administrador de Recursos del Kernel (Resource Allocator).
 * Controla la política de concesión de recursos con prevención de Coffman/Havender
 * (rango global) o sin prevención (orden natural con contención y riesgo de deadlock).
 */
@Service
public class GestorRecursos {

    private final AtomicReference<ModoPrevencion> modo = new AtomicReference<>(ModoPrevencion.PREVENCION_ACTIVA);
    private final Map<TipoRecurso, PoolRecursos> pools = new ConcurrentHashMap<>();
    private final Escaner escaner;

    public GestorRecursos(SimulacionProperties properties, Escaner escaner) {
        this.escaner = escaner;
        var rc = properties.recursos();

        pools.put(TipoRecurso.ESTACION_EMPAQUE, new PoolRecursos(TipoRecurso.ESTACION_EMPAQUE, rc.estacionesEmpaque()));
        pools.put(TipoRecurso.AREA_CARGA, new PoolRecursos(TipoRecurso.AREA_CARGA, rc.areasCarga()));
        pools.put(TipoRecurso.ENCARGADO_BODEGA, new PoolRecursos(TipoRecurso.ENCARGADO_BODEGA, rc.encargadosBodega()));
        pools.put(TipoRecurso.MONTACARGAS, new PoolRecursos(TipoRecurso.MONTACARGAS, rc.montacargas()));
        pools.put(TipoRecurso.CONTROL_CALIDAD, new PoolRecursos(TipoRecurso.CONTROL_CALIDAD, rc.controlCalidad()));
    }

    public void cambiarModo(ModoPrevencion nuevoModo) {
        if (nuevoModo != null) {
            this.modo.set(nuevoModo);
        }
    }

    public ModoPrevencion getModo() { return modo.get(); }

    public void adquirirRecurso(Pedido pedido, TipoRecurso recurso) throws InterruptedException {
        if (recurso == TipoRecurso.ESCANER) {
            escaner.adquirir(pedido);
        } else {
            PoolRecursos pool = pools.get(recurso);
            if (pool != null) pool.adquirir(pedido);
        }
    }

    public void liberarRecurso(Pedido pedido, TipoRecurso recurso) {
        if (recurso == TipoRecurso.ESCANER) {
            escaner.liberar(pedido);
        } else {
            PoolRecursos pool = pools.get(recurso);
            if (pool != null) pool.liberar(pedido);
        }
    }

    public void liberarTodos(Pedido pedido) {
        // Copia defensiva de los recursos retenidos para liberar sin ConcurrentModification
        Set<TipoRecurso> retenidos = new HashSet<>(pedido.getRecursosRetenidos());
        for (TipoRecurso recurso : retenidos) {
            liberarRecurso(pedido, recurso);
        }
        pedido.liberarTodosLosRecursos();
    }

    /**
     * Ordena la lista de recursos a solicitar según el modo de prevención activo.
     */
    public List<TipoRecurso> determinarOrdenAdquisicion(List<TipoRecurso> requeridos) {
        List<TipoRecurso> lista = new ArrayList<>(requeridos);
        if (modo.get() == ModoPrevencion.PREVENCION_ACTIVA) {
            // Regla de Havender: orden estricto por rango global ascendente
            lista.sort(Comparator.comparingInt(TipoRecurso::getOrdenGlobal));
        }
        return Collections.unmodifiableList(lista);
    }

    public PoolRecursos getPool(TipoRecurso tipo) { return pools.get(tipo); }
    public Escaner getEscaner() { return escaner; }

    public List<EstadoRecurso> obtenerEstados() {
        List<EstadoRecurso> estados = new ArrayList<>();
        for (TipoRecurso tipo : TipoRecurso.values()) {
            if (tipo == TipoRecurso.ESCANER) {
                estados.add(escaner.estado());
            } else if (pools.containsKey(tipo)) {
                estados.add(pools.get(tipo).estado());
            }
        }
        return Collections.unmodifiableList(estados);
    }
}
