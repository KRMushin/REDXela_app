package com.redxela.inventario;

import com.redxela.almacen.AlmacenLlenoException;
import com.redxela.almacen.AsignadorUbicaciones;
import com.redxela.almacen.ResultadoAsignacion;
import com.redxela.config.SimulacionProperties;
import com.redxela.dominio.CatalogoProductos;
import com.redxela.dominio.Producto;
import com.redxela.recepcion.MensajeLlegada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Área de Control de Inventario: registrar lotes y reevaluar pedidos sin stock.
 * 
 * Concepto de SO: Administración de almacenamiento secundario y comunicación entre
 * procesos. Las existencias están protegidas por {@link DepositoExistencias}
 * (ReentrantReadWriteLock) y el mapa de lotes usa una colección concurrente.
 * Cuando llega un lote se publica un evento que despierta al planificador.
 */
@Service
public class ServicioInventario {

    private static final Logger log = LoggerFactory.getLogger(ServicioInventario.class);

    private final Map<String, Lote> lotes = new ConcurrentHashMap<>();
    private final DepositoExistencias deposito;
    private final AsignadorUbicaciones asignador;
    private final CatalogoProductos catalogo;
    private final ApplicationEventPublisher eventos;

    private volatile String ultimoError;

    public ServicioInventario(
            DepositoExistencias deposito,
            AsignadorUbicaciones asignador,
            CatalogoProductos catalogo,
            ApplicationEventPublisher eventos,
            SimulacionProperties properties
    ) {
        this.deposito = deposito;
        this.asignador = asignador;
        this.catalogo = catalogo;
        this.eventos = eventos;
        sembrarStockInicial(properties.inventario() != null
                ? properties.inventario().stockInicialPorProducto()
                : 0);
    }

    /**
     * Coloca el inventario inicial en celdas reales del almacén para que las
     * existencias y el mapa físico sean consistentes desde el arranque.
     */
    private void sembrarStockInicial(int stockPorProducto) {
        if (stockPorProducto <= 0) {
            return;
        }
        int secuencial = 0;
        for (Producto producto : catalogo.listarTodos()) {
            try {
                String loteId = String.format("LOTE-INICIAL-%02d", ++secuencial);
                ResultadoAsignacion resultado = asignador.asignarLote(loteId, producto, stockPorProducto);
                deposito.aumentar(producto.id(), resultado.unidadesAsignadas());
                lotes.put(loteId, crearLote(loteId, producto.id(), stockPorProducto, resultado, System.currentTimeMillis()));
            } catch (AlmacenLlenoException e) {
                log.warn("Almacén lleno al sembrar stock inicial de {}: {}", producto.id(), e.getMessage());
            }
        }
    }

    /**
     * Registra un lote recibido: asigna celdas no contiguas, actualiza existencias
     * y publica el evento que despierta al despachador.
     */
    public void procesarMensaje(MensajeLlegada mensaje) {
        Producto producto = catalogo.buscarPorId(mensaje.productoId())
                .orElseThrow(() -> new IllegalArgumentException("Producto no existe: " + mensaje.productoId()));

        ResultadoAsignacion resultado;
        try {
            resultado = asignador.asignarLote(mensaje.loteId(), producto, mensaje.cantidad());
        } catch (AlmacenLlenoException e) {
            // Sin espacio: el lote se registra con todo pendiente para que sea visible
            lotes.put(mensaje.loteId(), new Lote(mensaje.loteId(), producto.id(), mensaje.cantidad(),
                    mensaje.cantidad(), List.of(), mensaje.tLlegada()));
            throw e;
        }

        deposito.aumentar(producto.id(), resultado.unidadesAsignadas());
        lotes.put(mensaje.loteId(), crearLote(
                mensaje.loteId(), producto.id(), mensaje.cantidad(), resultado, mensaje.tLlegada()));

        // El evento se publica fuera de los cerrojos para no re-entrar en ellos.
        eventos.publishEvent(new EventoLlegadaMercancia(
                producto.id(), resultado.unidadesAsignadas(), mensaje.loteId()));
    }

    private Lote crearLote(String loteId, String productoId, int cantidad,
                           ResultadoAsignacion resultado, long tRegistro) {
        return new Lote(loteId, productoId, cantidad,
                resultado.unidadesPendientes(), resultado.asignaciones(), tRegistro);
    }

    public int existenciaDe(String productoId) {
        return deposito.existenciaDe(productoId);
    }

    public Map<String, Integer> existencias() {
        return deposito.snapshot();
    }

    public Map<String, Lote> lotes() {
        return Map.copyOf(lotes);
    }

    public int unidadesPendientesTotales() {
        return lotes.values().stream().mapToInt(Lote::unidadesPendientes).sum();
    }

    public Optional<String> ultimoError() {
        return Optional.ofNullable(ultimoError);
    }

    public void registrarError(String error) {
        this.ultimoError = error;
    }
}
