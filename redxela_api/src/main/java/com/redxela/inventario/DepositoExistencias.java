package com.redxela.inventario;

import com.redxela.dominio.ItemPedido;
import com.redxela.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Depósito de existencias por producto del centro de distribución.
 * 
 * Concepto de SO: Lectores-Escritores (Readers-Writers) sobre una estructura compartida.
 * Muchos hilos consultan existencias en paralelo (cerrojo de lectura) mientras la
 * reserva y la actualización de stock son exclusivas (cerrojo de escritura).
 * La operación "verificar y descontar" es atómica, evitando sobreventa entre pedidos.
 */
@Component
public class DepositoExistencias {

    private final Map<String, Integer> existencias = new HashMap<>();
    private final ReentrantReadWriteLock cerrojo = new ReentrantReadWriteLock(true);

    /**
     * Reserva atómica de todos los artículos del pedido.
     * Si falta al menos un producto no se descuenta nada.
     */
    public boolean reservar(Pedido pedido) {
        Map<String, Integer> requerido = requerimientosPorProducto(pedido);
        cerrojo.writeLock().lock();
        try {
            for (Map.Entry<String, Integer> entrada : requerido.entrySet()) {
                if (existencias.getOrDefault(entrada.getKey(), 0) < entrada.getValue()) {
                    return false; // stock insuficiente: el pedido queda EN_ESPERA_STOCK
                }
            }
            for (Map.Entry<String, Integer> entrada : requerido.entrySet()) {
                existencias.merge(entrada.getKey(), -entrada.getValue(), Integer::sum);
            }
            return true;
        } finally {
            cerrojo.writeLock().unlock(); // liberación garantizada de la sección crítica
        }
    }

    /**
     * Devuelve al depósito el stock previamente reservado (rollback).
     */
    public void devolver(Pedido pedido) {
        Map<String, Integer> requerido = requerimientosPorProducto(pedido);
        cerrojo.writeLock().lock();
        try {
            requerido.forEach((productoId, cantidad) -> existencias.merge(productoId, cantidad, Integer::sum));
        } finally {
            cerrojo.writeLock().unlock();
        }
    }

    /**
     * Incrementa las existencias cuando Inventario almacena un lote.
     */
    public void aumentar(String productoId, int cantidad) {
        if (cantidad <= 0) {
            return;
        }
        cerrojo.writeLock().lock();
        try {
            existencias.merge(productoId, cantidad, Integer::sum);
        } finally {
            cerrojo.writeLock().unlock();
        }
    }

    public int existenciaDe(String productoId) {
        return enLectura(() -> existencias.getOrDefault(productoId, 0));
    }

    public Map<String, Integer> snapshot() {
        return enLectura(() -> Map.copyOf(existencias));
    }

    /** Consulta concurrente bajo el cerrojo de lectura compartido. */
    private <T> T enLectura(Supplier<T> accion) {
        cerrojo.readLock().lock();
        try {
            return accion.get();
        } finally {
            cerrojo.readLock().unlock();
        }
    }

    private Map<String, Integer> requerimientosPorProducto(Pedido pedido) {
        Map<String, Integer> requerido = new HashMap<>();
        for (ItemPedido item : pedido.getItems()) {
            requerido.merge(item.producto().id(), item.cantidad(), Integer::sum);
        }
        return requerido;
    }
}
