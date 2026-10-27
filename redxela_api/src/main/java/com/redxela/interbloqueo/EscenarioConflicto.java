package com.redxela.interbloqueo;

import com.redxela.dominio.CatalogoProductos;
import com.redxela.dominio.Cliente;
import com.redxela.dominio.EstadoPedido;
import com.redxela.dominio.ItemPedido;
import com.redxela.dominio.NivelServicio;
import com.redxela.dominio.Pedido;
import com.redxela.dominio.PedidoService;
import com.redxela.dominio.Producto;
import com.redxela.procesamiento.GestorProcesamiento;
import com.redxela.recursos.GestorRecursos;
import com.redxela.recursos.ModoPrevencion;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * Escenario controlado que provoca deliberadamente un interbloqueo.
 * 
 * Inyección de fallas para demostrar la condición de espera circular
 * de Coffman. Tres pedidos Pesados retienen todos los montacargas y dos Voluminosos
 * retienen todas las áreas de carga; la barrera garantiza que ambos grupos ya tengan
 * su primer recurso antes de pedir el segundo, en orden inverso.
 */
@Service
public class EscenarioConflicto {

    private static final int PESADOS = 3;
    private static final int VOLUMINOSOS = 2;
    private static final long ESPERA_MAXIMA_MS = 5_000L;
    private static final long PAUSA_ENTRE_INTENTOS_MS = 25L;

    private final GestorRecursos gestorRecursos;
    private final GestorProcesamiento gestorProcesamiento;
    private final SincronizadorConflicto sincronizador;
    private final DetectorInterbloqueo detector;
    private final PedidoService pedidoService;
    private final CatalogoProductos catalogo;
    private final AtomicInteger secuencial = new AtomicInteger();
    private final AtomicBoolean enEjecucion = new AtomicBoolean(false);

    public EscenarioConflicto(
            GestorRecursos gestorRecursos,
            GestorProcesamiento gestorProcesamiento,
            SincronizadorConflicto sincronizador,
            DetectorInterbloqueo detector,
            PedidoService pedidoService,
            CatalogoProductos catalogo
    ) {
        this.gestorRecursos = gestorRecursos;
        this.gestorProcesamiento = gestorProcesamiento;
        this.sincronizador = sincronizador;
        this.detector = detector;
        this.pedidoService = pedidoService;
        this.catalogo = catalogo;
    }

    /**
     * Fuerza SIN_PREVENCION, crea la carga conflictiva y espera el reporte del detector.
     */
    public ReporteInterbloqueo forzar() {
        if (!enEjecucion.compareAndSet(false, true)) {
            throw new IllegalStateException("Ya hay un escenario de conflicto en ejecución");
        }
        try {
            if (detector.detectarAhora().hayInterbloqueo()) {
                throw new IllegalStateException("Ya existe un interbloqueo activo; resuélvalo antes de forzar otro");
            }
            gestorRecursos.cambiarModo(ModoPrevencion.SIN_PREVENCION);

            Cliente cliente = new Cliente("CLI-CONFLICTO", "Cliente del Escenario de Conflicto", NivelServicio.EXPRES);
            Producto pesado = obtenerProducto("PROD-03");
            Producto voluminoso = obtenerProducto("PROD-05");

            List<Pedido> pedidos = new ArrayList<>();
            for (int i = 1; i <= PESADOS; i++) {
                pedidos.add(crearPedido("PESADO-" + i, pesado, cliente));
            }
            for (int i = 1; i <= VOLUMINOSOS; i++) {
                pedidos.add(crearPedido("VOLUMINOSO-" + i, voluminoso, cliente));
            }

            List<String> ids = pedidos.stream().map(Pedido::getId).toList();
            sincronizador.preparar(ids, pedidos.size());
            try {
                pedidos.forEach(pedido -> gestorProcesamiento.ejecutar(pedido));
                return esperarDeteccion();
            } finally {
                sincronizador.limpiar();
            }
        } finally {
            enEjecucion.set(false);
        }
    }

    private Pedido crearPedido(String sufijo, Producto producto, Cliente cliente) {
        String id = String.format("CONF-%03d-%s", secuencial.incrementAndGet(), sufijo);
        Pedido pedido = new Pedido(id, cliente, List.of(new ItemPedido(producto, 2)));
        pedido.setEstado(EstadoPedido.EN_ESPERA_RECURSOS);
        pedidoService.registrarPedidoExistente(pedido);
        return pedido;
    }

    private Producto obtenerProducto(String id) {
        return catalogo.buscarPorId(id)
                .orElseThrow(() -> new IllegalStateException("Producto del catálogo no encontrado: " + id));
    }

    /**
     * Espera activa (parking) hasta que el detector encuentre el ciclo o venza el plazo.
     */
    private ReporteInterbloqueo esperarDeteccion() {
        long limite = System.currentTimeMillis() + ESPERA_MAXIMA_MS;
        while (System.currentTimeMillis() < limite) {
            ReporteInterbloqueo reporte = detector.detectarAhora();
            if (reporte.hayInterbloqueo()) {
                return reporte;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(PAUSA_ENTRE_INTENTOS_MS));
        }
        throw new IllegalStateException("No se logró provocar el interbloqueo dentro del tiempo esperado");
    }
}
