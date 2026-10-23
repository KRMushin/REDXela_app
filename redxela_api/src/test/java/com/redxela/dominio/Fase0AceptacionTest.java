package com.redxela.dominio;

import com.redxela.config.SimulacionProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de aceptación para la FASE 0: Contratos y dominio.
 * 
 * Concepto de SO: Validación de la estructura de datos del kernel,
 * parámetros de configuración del sistema (tabla de recursos) y especificación
 * del grafo de asignación de recursos (RAG) con potencial de contención.
 */
@SpringBootTest
class Fase0AceptacionTest {

    @Autowired
    private CatalogoProductos catalogoProductos;

    @Autowired
    private SimulacionProperties simulacionProperties;

    @Test
    @DisplayName("El catálogo de productos debe cargar 12 productos con sus tipos de mercancía")
    void testCatalogoProductosCargado() {
        assertNotNull(catalogoProductos, "El catálogo debe inyectarse como componente de Spring");
        assertEquals(12, catalogoProductos.totalProductos(), "El catálogo debe precargar exactamente 12 productos");

        // Validar que cada producto tenga tipo asignado y descripción
        catalogoProductos.listarTodos().forEach(producto -> {
            assertNotNull(producto.id());
            assertNotNull(producto.nombre());
            assertNotNull(producto.tipoMercancia());
            assertNotNull(producto.descripcion());
        });
    }

    @Test
    @DisplayName("Los 6 tipos de mercancía deben definir la secuencia exacta de recursos")
    void testSecuenciasRecursosPorTipoMercancia() {
        assertEquals(6, TipoMercancia.values().length, "Deben existir exactamente 6 tipos de mercancía");

        // 1. Frágil: Encargado -> Control de calidad -> Empaque
        assertEquals(
                List.of(TipoRecurso.ENCARGADO_BODEGA, TipoRecurso.CONTROL_CALIDAD, TipoRecurso.ESTACION_EMPAQUE),
                TipoMercancia.FRAGIL.getRecursosRequeridos()
        );

        // 2. Pesado: Montacargas -> Encargado -> Área de carga
        assertEquals(
                List.of(TipoRecurso.MONTACARGAS, TipoRecurso.ENCARGADO_BODEGA, TipoRecurso.AREA_CARGA),
                TipoMercancia.PESADO.getRecursosRequeridos()
        );

        // 3. Voluminoso: Área de carga -> Montacargas -> Empaque
        assertEquals(
                List.of(TipoRecurso.AREA_CARGA, TipoRecurso.MONTACARGAS, TipoRecurso.ESTACION_EMPAQUE),
                TipoMercancia.VOLUMINOSO.getRecursosRequeridos()
        );

        // 4. Electrónico: Control de calidad -> Escáner -> Empaque
        assertEquals(
                List.of(TipoRecurso.CONTROL_CALIDAD, TipoRecurso.ESCANER, TipoRecurso.ESTACION_EMPAQUE),
                TipoMercancia.ELECTRONICO.getRecursosRequeridos()
        );

        // 5. Perecedero: Encargado -> Empaque -> Área de carga
        assertEquals(
                List.of(TipoRecurso.ENCARGADO_BODEGA, TipoRecurso.ESTACION_EMPAQUE, TipoRecurso.AREA_CARGA),
                TipoMercancia.PERECEDERO.getRecursosRequeridos()
        );

        // 6. General: Encargado -> Empaque -> Escáner
        assertEquals(
                List.of(TipoRecurso.ENCARGADO_BODEGA, TipoRecurso.ESTACION_EMPAQUE, TipoRecurso.ESCANER),
                TipoMercancia.GENERAL.getRecursosRequeridos()
        );
    }

    @Test
    @DisplayName("Verificar contención y potencial de interbloqueo entre Pesado y Voluminoso")
    void testCondicionInterbloqueoPesadoVoluminoso() {
        List<TipoRecurso> reqPesado = TipoMercancia.PESADO.getRecursosRequeridos();
        List<TipoRecurso> reqVoluminoso = TipoMercancia.VOLUMINOSO.getRecursosRequeridos();

        // Pesado pide Montacargas antes que Área de Carga
        int idxMontacargasPesado = reqPesado.indexOf(TipoRecurso.MONTACARGAS);
        int idxCargaPesado = reqPesado.indexOf(TipoRecurso.AREA_CARGA);
        assertTrue(idxMontacargasPesado < idxCargaPesado, "Pesado debe solicitar Montacargas antes de Área de Carga");

        // Voluminoso pide Área de Carga antes que Montacargas
        int idxCargaVoluminoso = reqVoluminoso.indexOf(TipoRecurso.AREA_CARGA);
        int idxMontacargasVoluminoso = reqVoluminoso.indexOf(TipoRecurso.MONTACARGAS);
        assertTrue(idxCargaVoluminoso < idxMontacargasVoluminoso, "Voluminoso debe solicitar Área de Carga antes de Montacargas");
    }

    @Test
    @DisplayName("Verificar propiedades tipadas cargadas desde application.yml")
    void testPropiedadesSimulacionCargadas() {
        assertNotNull(simulacionProperties);
        assertEquals(1000L, simulacionProperties.timeScaleMs());
        assertEquals(50, simulacionProperties.capacidadColaRecepcion());
        assertEquals(5, simulacionProperties.tasaGeneradorPedidosMinuto());

        var recursos = simulacionProperties.recursos();
        assertNotNull(recursos);
        assertEquals(6, recursos.estacionesEmpaque());
        assertEquals(2, recursos.areasCarga());
        assertEquals(10, recursos.encargadosBodega());
        assertEquals(3, recursos.montacargas());
        assertEquals(4, recursos.controlCalidad());
        assertEquals(1, recursos.escaner());

        var almacen = simulacionProperties.almacen();
        assertNotNull(almacen);
        assertEquals(20, almacen.totalUbicaciones());
        assertEquals(50, almacen.capacidadPorUbicacion());
    }

    @Test
    @DisplayName("Verificar creación y ciclo inicial de Pedido (PCB)")
    void testCreacionPedidoPCB() {
        Cliente cliente = new Cliente("CLI-01", "Empresa X", NivelServicio.PRIORITARIO);
        Producto prod = catalogoProductos.buscarPorId("PROD-01").orElseThrow();
        ItemPedido item = new ItemPedido(prod, 15);

        Pedido pedido = new Pedido("PED-100", cliente, List.of(item));

        assertEquals("PED-100", pedido.getId());
        assertEquals(cliente, pedido.getCliente());
        assertEquals(NivelServicio.PRIORITARIO, pedido.getNivel());
        assertEquals(15, pedido.getTotalArticulos());
        assertEquals(EstadoPedido.EN_ESPERA_STOCK, pedido.getEstado());
        assertEquals(0, pedido.getProgreso());
        assertTrue(pedido.getTCreacion() > 0);
        assertTrue(pedido.getRecursosRetenidos().isEmpty());
        assertNull(pedido.getRecursoEsperado());

        // Simular transición y retención
        pedido.agregarRecursoRetenido(TipoRecurso.ENCARGADO_BODEGA);
        pedido.setRecursoEsperado(TipoRecurso.CONTROL_CALIDAD);
        pedido.setEstado(EstadoPedido.BLOQUEADO);

        assertTrue(pedido.getRecursosRetenidos().contains(TipoRecurso.ENCARGADO_BODEGA));
        assertEquals(TipoRecurso.CONTROL_CALIDAD, pedido.getRecursoEsperado());
        assertEquals(EstadoPedido.BLOQUEADO, pedido.getEstado());

        pedido.liberarTodosLosRecursos();
        assertTrue(pedido.getRecursosRetenidos().isEmpty());
        assertNull(pedido.getRecursoEsperado());
    }
}
