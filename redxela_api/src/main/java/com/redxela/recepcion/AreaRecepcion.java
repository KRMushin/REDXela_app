package com.redxela.recepcion;

import com.redxela.config.SimulacionProperties;
import com.redxela.dominio.CatalogoProductos;
import com.redxela.recursos.Escaner;
import com.redxela.simulacion.RelojSimulacion;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Área de Recepción de mercadería del centro de distribución.
 * 
 * Concepto de SO: Productor dentro del patrón Productor-Consumidor y buzon acotado
 * (Bounded Buffer / Message Mailbox). Recepción produce {@link MensajeLlegada} y los
 * deposita con {@code put()}; si el buzon está lleno el productor se bloquea
 * (contrapresión) en lugar de descartar datos. El escáner es un recurso compartido
 * con Despacho y se toma en exclusión mutua solo durante la descarga.
 */
@Component
public class AreaRecepcion {

    /** Tiempo simulado de escaneo de un lote (minutos simulados). */
    private static final double MINUTOS_ESCANEO = 0.05;

    private final BlockingQueue<MensajeLlegada> buzon;
    private final int capacidad;
    private final Escaner escaner;
    private final RelojSimulacion reloj;
    private final CatalogoProductos catalogo;
    private final AtomicInteger secuencialLote = new AtomicInteger();

    public AreaRecepcion(
            SimulacionProperties properties,
            Escaner escaner,
            RelojSimulacion reloj,
            CatalogoProductos catalogo
    ) {
        int configurada = properties.capacidadColaRecepcion() > 0
                ? properties.capacidadColaRecepcion()
                : 50;
        this.capacidad = configurada;
        // Cola acotada y justa: impone contrapresión y respeta el orden de llegada
        this.buzon = new ArrayBlockingQueue<>(configurada, true);
        this.escaner = escaner;
        this.reloj = reloj;
        this.catalogo = catalogo;
    }

    /**
     * Registra la llegada de un lote y lo publica en el buzon de Inventario.
     * El hilo se bloquea si el buzon está lleno; nunca se pierde un mensaje.
     */
    public MensajeLlegada recibirLlegada(String productoId, int cantidad) throws InterruptedException {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad recibida debe ser mayor a cero");
        }
        catalogo.buscarPorId(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no existe: " + productoId));

        MensajeLlegada mensaje = new MensajeLlegada(generarLoteId(), productoId, cantidad, System.currentTimeMillis());

        // Sección crítica: el escáner es único y compartido con el despacho de pedidos.
        escaner.adquirirPorRecepcion();
        try {
            reloj.pausarSimulado(MINUTOS_ESCANEO); // simula la descarga y lectura del código
        } finally {
            escaner.liberarPorRecepcion(); // liberación garantizada incluso ante interrupción
        }

        // Contrapresión: si el inventario va más lento, el productor espera aquí.
        buzon.put(mensaje);
        return mensaje;
    }

    /**
     * Consumo bloqueante usado por el hilo de Inventario.
     */
    public MensajeLlegada tomar() throws InterruptedException {
        return buzon.take();
    }

    public int tamanoCola() {
        return buzon.size();
    }

    public int capacidadCola() {
        return capacidad;
    }

    public boolean colaVacia() {
        return buzon.isEmpty();
    }

    /**
     * Vacía el buzón y reinicia la numeración de lotes para una nueva simulación.
     */
    public void vaciar() {
        buzon.clear();
        secuencialLote.set(0);
    }

    private String generarLoteId() {
        return String.format("LOTE-2026-%04d", secuencialLote.incrementAndGet());
    }
}
