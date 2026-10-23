package com.redxela.estadisticas;

import com.redxela.api.dto.EstadisticasSistema;
import com.redxela.api.dto.SnapshotEstado;
import com.redxela.dominio.Cliente;
import com.redxela.dominio.EstadoPedido;
import com.redxela.dominio.ItemPedido;
import com.redxela.dominio.NivelServicio;
import com.redxela.dominio.Pedido;
import com.redxela.dominio.PedidoService;
import com.redxela.dominio.Producto;
import com.redxela.dominio.CatalogoProductos;
import com.redxela.simulacion.ServicioReinicio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Aceptación de FASE 7A: estadísticas globales, por nivel y por recurso.
 *
 * Concepto de SO: Contabilidad de procesos y verificación del cumplimiento de
 * plazos (SLA) con la misma escala temporal usada por el sistema.
 */
@SpringBootTest
class Fase7EstadisticasTest {

    @Autowired private ServicioEstadisticas estadisticas;
    @Autowired private PedidoService pedidos;
    @Autowired private CatalogoProductos catalogo;
    @Autowired private ServicioReinicio reinicio;

    @BeforeEach
    @AfterEach
    void limpiar() {
        reinicio.reiniciar();
    }

    @Test
    @DisplayName("Calcula espera, procesamiento, SLA y ocupación por nivel y recurso")
    void calculaMetricasGlobalesYDesagregadas() {
        Producto producto = catalogo.buscarPorId("PROD-01").orElseThrow();
        completado("STAT-EXPRES", NivelServicio.EXPRES, producto, 0, 500);
        completado("STAT-PRIORITARIO", NivelServicio.PRIORITARIO, producto, 5_000, 4_000);
        completado("STAT-ESTANDAR", NivelServicio.ESTANDAR, producto, 10_000, 15_000);
        completado("STAT-PROGRAMADO", NivelServicio.PROGRAMADO, producto, 20_000, 20_000);
        completado("STAT-ECONOMICO", NivelServicio.ECONOMICO, producto, 100_000, 30_000);

        EstadisticasSistema detalle = estadisticas.calcularDetalladas();
        SnapshotEstado.Estadisticas resumen = estadisticas.calcularResumen();

        assertEquals(5, detalle.pedidosCompletados());
        assertEquals(27.0, detalle.tiempoEsperaPromedioMinutos(), 0.05);
        assertEquals(13.9, detalle.tiempoProcesamientoPromedioMinutos(), 0.05);
        assertEquals(3, detalle.pedidosDentroSla());
        assertEquals(60.0, detalle.cumplimientoSlaPorcentaje(), 0.05);
        assertTrue(detalle.tasaThroughputPorMinuto() > 0);
        assertEquals(5, detalle.porNivel().size());
        assertEquals(6, detalle.ocupacionRecursos().size());

        assertEquals(detalle.pedidosCompletados(), resumen.pedidosCompletados());
        assertEquals(detalle.tiempoEsperaPromedioMinutos(), resumen.tiempoEsperaPromedioMinutos());
        assertEquals(detalle.cumplimientoSlaPorcentaje(), resumen.cumplimientoSlaPorcentaje());

        EstadisticasSistema.MetricaNivel prioritario = porNivel(detalle, NivelServicio.PRIORITARIO);
        assertEquals(1, prioritario.pedidosCompletados());
        assertEquals(5.0, prioritario.tiempoEsperaPromedioMinutos(), 0.05);
        assertEquals(4.0, prioritario.tiempoProcesamientoPromedioMinutos(), 0.05);
        assertEquals(1, prioritario.pedidosDentroSla());
        assertEquals(100.0, prioritario.cumplimientoSlaPorcentaje(), 0.05);

        EstadisticasSistema.MetricaNivel economico = porNivel(detalle, NivelServicio.ECONOMICO);
        assertEquals(0, economico.pedidosDentroSla());
        assertEquals(0.0, economico.cumplimientoSlaPorcentaje(), 0.05);
        assertEquals(130.0, economico.tiempoEsperaPromedioMinutos()
                + economico.tiempoProcesamientoPromedioMinutos(), 0.1);

        detalle.ocupacionRecursos().forEach(ocupacion -> {
            assertEquals(0, ocupacion.enUso());
            assertEquals(ocupacion.total(), ocupacion.disponibles());
            assertEquals(0.0, ocupacion.ocupacionPorcentaje(), 0.01);
        });
    }

    private void completado(
            String id,
            NivelServicio nivel,
            Producto producto,
            long esperaMs,
            long procesamientoMs
    ) {
        Cliente cliente = new Cliente("CLI-" + id, "Cliente " + nivel.getNombre(), nivel);
        Pedido pedido = new Pedido(id, cliente, List.of(new ItemPedido(producto, 2)));
        pedidos.registrarPedidoExistente(pedido);
        pedido.setTInicio(pedido.getTCreacion() + esperaMs);
        pedido.setTFin(pedido.getTInicio() + procesamientoMs);
        pedido.setProgreso(100);
        pedido.setEstado(EstadoPedido.COMPLETADO);
    }

    private EstadisticasSistema.MetricaNivel porNivel(
            EstadisticasSistema detalle,
            NivelServicio nivel
    ) {
        return detalle.porNivel().stream()
                .filter(metrica -> metrica.nivel() == nivel)
                .findFirst()
                .orElseThrow();
    }
}
