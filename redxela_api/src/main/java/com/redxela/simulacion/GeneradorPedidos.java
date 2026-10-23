package com.redxela.simulacion;

import com.redxela.config.SimulacionProperties;
import com.redxela.dominio.*;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generador automático y continuo de carga de trabajo para el sistema.
 * 
 * Concepto de SO: Generador de Procesos / Carga Sintética (Workload Generator).
 * Inyecta procesos concurrentes en el sistema de manera asíncrona, simulando la tasa
 * de arribo de solicitudes externas con distribución variada de niveles de prioridad y ráfagas.
 */
@Service
public class GeneradorPedidos {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final CatalogoProductos catalogo;

    private final AtomicBoolean activo = new AtomicBoolean(false);
    private final AtomicInteger tasaPorMinuto;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> tareaGeneracion = null;

    public GeneradorPedidos(
            PedidoService pedidoService,
            ClienteService clienteService,
            CatalogoProductos catalogo,
            SimulacionProperties properties
    ) {
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
        this.catalogo = catalogo;
        int tasaInicial = properties.tasaGeneradorPedidosMinuto() > 0 ? properties.tasaGeneradorPedidosMinuto() : 5;
        this.tasaPorMinuto = new AtomicInteger(tasaInicial);
    }

    public synchronized void iniciar() {
        if (activo.compareAndSet(false, true)) {
            programarTarea();
        }
    }

    public synchronized void detener() {
        if (activo.compareAndSet(true, false)) {
            if (tareaGeneracion != null && !tareaGeneracion.isDone()) {
                tareaGeneracion.cancel(false);
            }
        }
    }

    public synchronized void ajustarTasa(int nuevaTasaPorMinuto) {
        if (nuevaTasaPorMinuto <= 0) {
            throw new IllegalArgumentException("La tasa debe ser mayor a cero");
        }
        this.tasaPorMinuto.set(nuevaTasaPorMinuto);
        if (activo.get()) {
            if (tareaGeneracion != null) tareaGeneracion.cancel(false);
            programarTarea();
        }
    }

    private void programarTarea() {
        long periodoMs = Math.max(50L, (60L * 1000L) / tasaPorMinuto.get());
        tareaGeneracion = scheduler.scheduleAtFixedRate(
                this::generarPedidoAleatorio,
                0L,
                periodoMs,
                TimeUnit.MILLISECONDS
        );
    }

    public Pedido generarPedidoAleatorio() {
        Cliente cliente = clienteService.obtenerClienteAleatorio()
                .orElseGet(() -> clienteService.crearCliente("Cliente Genérico", NivelServicio.ESTANDAR));

        List<Producto> productos = new ArrayList<>(catalogo.listarTodos());
        int cantItems = 1 + (int) (Math.random() * 3);
        List<ItemPedido> items = new ArrayList<>();

        for (int i = 0; i < cantItems; i++) {
            Producto p = productos.get((int) (Math.random() * productos.size()));
            int cantidad = 1 + (int) (Math.random() * 10);
            items.add(new ItemPedido(p, cantidad));
        }

        return pedidoService.registrarNuevoPedido(cliente, items);
    }

    public boolean isActivo() { return activo.get(); }
    public int getTasaPorMinuto() { return tasaPorMinuto.get(); }

    @PreDestroy
    public void cerrar() {
        detener();
        scheduler.shutdownNow();
    }
}
