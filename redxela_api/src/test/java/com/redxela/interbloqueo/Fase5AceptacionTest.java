package com.redxela.interbloqueo;

import com.redxela.dominio.*;
import com.redxela.procesamiento.GestorProcesamiento;
import com.redxela.recursos.GestorRecursos;
import com.redxela.recursos.ModoPrevencion;
import com.redxela.recursos.PoolRecursos;
import com.redxela.simulacion.GeneradorPedidos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de aceptación de la FASE 5: detección y resolución de interbloqueos.
 * 
 * Concepto de SO: Espera circular de Coffman, grafo de espera con DFS,
 * prevención por orden global (Havender) y recuperación por expropiación.
 */
@SpringBootTest(properties = {
        "simulacion.time-scale-ms=5",
        "simulacion.deteccion-interbloqueo-ms=25",
        "simulacion.tasa-generador-pedidos-minuto=1"
})
class Fase5AceptacionTest {

    @Autowired private EscenarioConflicto escenario;
    @Autowired private ResolutorInterbloqueo resolutor;
    @Autowired private DetectorInterbloqueo detector;
    @Autowired private GestorRecursos gestorRecursos;
    @Autowired private GestorProcesamiento gestorProcesamiento;
    @Autowired private PedidoService pedidoService;
    @Autowired private CatalogoProductos catalogo;
    @Autowired private GeneradorPedidos generador;

    @BeforeEach
    void prepararSistema() throws InterruptedException {
        generador.detener();
        gestorRecursos.cambiarModo(ModoPrevencion.PREVENCION_ACTIVA);
        ReporteInterbloqueo reporte = detector.detectarAhora();
        if (reporte.hayInterbloqueo()) {
            resolutor.resolver(reporte.pedidosInvolucrados().get(0));
        }
        esperarHasta(() -> gestorProcesamiento.totalTareasActivas() == 0
                && !detector.detectarAhora().hayInterbloqueo()
                && recursoLibre(TipoRecurso.MONTACARGAS)
                && recursoLibre(TipoRecurso.AREA_CARGA)
                && conflictosCompletados(), 15000);
    }

    @Test
    @DisplayName("El escenario provoca interbloqueo real, lo reporta y la resolución manual lo rompe")
    void interbloqueoGarantizadoYResolucionManual() throws Exception {
        ReporteInterbloqueo reporte = escenario.forzar();

        assertTrue(reporte.hayInterbloqueo(), "El escenario debe provocar un interbloqueo");
        assertEquals(5, reporte.pedidosInvolucrados().size(),
                "Deben aparecer los 3 Pesados y los 2 Voluminosos que saturan los recursos");
        assertEquals(3L, reporte.pedidosInvolucrados().stream()
                .filter(id -> id.contains("PESADO")).count());
        assertEquals(2L, reporte.pedidosInvolucrados().stream()
                .filter(id -> id.contains("VOLUMINOSO")).count());
        assertTrue(reporte.recursosEnDisputa().contains(TipoRecurso.MONTACARGAS));
        assertTrue(reporte.recursosEnDisputa().contains(TipoRecurso.AREA_CARGA));
        assertNotNull(reporte.explicacion());
        assertTrue(reporte.explicacion().toLowerCase().contains("espera"));

        String ganador = reporte.pedidosInvolucrados().get(0);
        String perdedor = reporte.pedidosInvolucrados().get(1);

        ReporteInterbloqueo despues = resolutor.resolver(ganador);
        assertFalse(despues.hayInterbloqueo(), "Tras expropiar al perdedor no debe quedar el ciclo");

        esperarHasta(() -> gestorProcesamiento.totalTareasActivas() == 0
                && recursoLibre(TipoRecurso.MONTACARGAS)
                && recursoLibre(TipoRecurso.AREA_CARGA)
                && conflictosCompletados(), 15000);

        assertEquals(EstadoPedido.COMPLETADO,
                pedidoService.buscarPorId(ganador).orElseThrow().getEstado(),
                "El ganador debe continuar y terminar");
        assertEquals(EstadoPedido.COMPLETADO,
                pedidoService.buscarPorId(perdedor).orElseThrow().getEstado(),
                "El perdedor debe reencolarse y terminar más adelante");
    }

    @Test
    @DisplayName("Con prevención activa la misma carga nunca produce interbloqueo")
    void conPrevencionNuncaOcurre() throws Exception {
        gestorRecursos.cambiarModo(ModoPrevencion.PREVENCION_ACTIVA);
        Cliente cliente = new Cliente("CLI-PREV", "Cliente Prevención", NivelServicio.EXPRES);
        Producto pesado = catalogo.buscarPorId("PROD-03").orElseThrow();
        Producto voluminoso = catalogo.buscarPorId("PROD-05").orElseThrow();

        List<Pedido> pedidos = new ArrayList<>();
        List<Future<?>> futuros = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            pedidos.add(new Pedido("PREV-P" + i, cliente, List.of(new ItemPedido(pesado, 2))));
        }
        for (int i = 1; i <= 2; i++) {
            pedidos.add(new Pedido("PREV-V" + i, cliente, List.of(new ItemPedido(voluminoso, 2))));
        }
        pedidos.forEach(pedido -> futuros.add(gestorProcesamiento.ejecutar(pedido)));

        for (Future<?> futuro : futuros) {
            futuro.get(15, TimeUnit.SECONDS);
        }
        for (Pedido pedido : pedidos) {
            assertEquals(EstadoPedido.COMPLETADO, pedido.getEstado(),
                    "Con orden global todos los pedidos deben terminar: " + pedido.getId());
        }
        assertFalse(detector.detectarAhora().hayInterbloqueo(),
                "La prevención por orden global no debe permitir espera circular");
    }

    private boolean recursoLibre(TipoRecurso tipo) {
        PoolRecursos pool = gestorRecursos.getPool(tipo);
        return pool.getDisponibles() == pool.getTotalUnidades();
    }

    /** Todos los pedidos del escenario deben haber terminado (no solo el ganador). */
    private boolean conflictosCompletados() {
        return pedidoService.listarPedidos().stream()
                .filter(pedido -> pedido.getId().startsWith("CONF-"))
                .allMatch(pedido -> pedido.getEstado() == EstadoPedido.COMPLETADO);
    }

    private void esperarHasta(BooleanSupplier condicion, long timeoutMs) throws InterruptedException {
        long limite = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < limite) {
            if (condicion.getAsBoolean()) {
                return;
            }
            Thread.sleep(10);
        }
        fail("Tiempo de espera agotado esperando la condición del escenario");
    }
}
