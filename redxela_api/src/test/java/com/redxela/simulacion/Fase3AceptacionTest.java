package com.redxela.simulacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redxela.api.dto.AjustarTasaRequest;
import com.redxela.api.dto.CrearClienteRequest;
import com.redxela.api.dto.CrearPedidoRequest;
import com.redxela.api.dto.ItemPedidoRequest;
import com.redxela.dominio.*;
import com.redxela.procesamiento.GestorProcesamiento;
import com.redxela.recursos.EstadoRecurso;
import com.redxela.recursos.GestorRecursos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de aceptación para la FASE 3: Procesamiento y generador.
 * 
 * Concepto de SO: Evaluación del ciclo de ejecución de procesos, terminación de tareas,
 * liberación garantizada de recursos periféricos (RAII/finally), generador continuo
 * y llamadas al sistema para creación manual de tareas y usuarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Fase3AceptacionTest {

    @Autowired
    private RelojSimulacion relojSimulacion;

    @Autowired
    private GestorProcesamiento gestorProcesamiento;

    @Autowired
    private GestorRecursos gestorRecursos;

    @Autowired
    private GeneradorPedidos generadorPedidos;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private CatalogoProductos catalogoProductos;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws InterruptedException {
        generadorPedidos.detener();
        pedidoService.limpiarParaPruebas();
        relojSimulacion.setTimeScaleMs(30L); // Escala rápida por defecto
        Thread.sleep(100);
    }

    @Test
    @DisplayName("Pedidos de todos los niveles de servicio completan su ejecución y liberan todos los recursos")
    void testPedidosDeTodosLosNivelesTerminanYRecursosSeLiberan() throws Exception {
        Producto prod = catalogoProductos.buscarPorId("PROD-01").orElseThrow();
        List<Future<?>> futuros = new ArrayList<>();
        List<Pedido> pedidosTest = new ArrayList<>();

        for (NivelServicio nivel : NivelServicio.values()) {
            Cliente cliente = new Cliente("CLI-TEST-" + nivel.name(), "Cliente " + nivel.getNombre(), nivel);
            Pedido pedido = new Pedido("PED-NIVEL-" + nivel.name(), cliente, List.of(new ItemPedido(prod, 2)));
            pedidosTest.add(pedido);
            futuros.add(gestorProcesamiento.ejecutar(pedido));
        }

        // Esperar la terminación de cada proceso en su hilo respectivo
        for (Future<?> f : futuros) {
            f.get(5, TimeUnit.SECONDS);
        }

        // 1. Verificar que los 5 niveles terminaron en estado COMPLETADO
        for (Pedido p : pedidosTest) {
            assertEquals(EstadoPedido.COMPLETADO, p.getEstado(), "El pedido de nivel " + p.getNivel() + " debió terminar");
            assertEquals(100, p.getProgreso());
            assertNotNull(p.getTInicio());
            assertNotNull(p.getTFin());
            assertTrue(p.getRecursosRetenidos().isEmpty(), "No deben quedar recursos retenidos en el PCB");
        }

        // 2. Invariante: todos los recursos del centro de distribución deben estar 100% libres
        for (EstadoRecurso estado : gestorRecursos.obtenerEstados()) {
            assertEquals(0, estado.enUso(), "El recurso " + estado.nombre() + " no debe tener unidades en uso");
            assertEquals(estado.total(), estado.disponibles(), "El recurso " + estado.nombre() + " debe tener todas sus unidades disponibles");
            assertTrue(estado.pedidosAsignados().isEmpty());
        }
    }

    @Test
    @DisplayName("El generador de pedidos corre continuamente, produce todos los niveles y permite ajuste de tasa")
    void testGeneradorCorreSinTopeYRegulaTasa() throws InterruptedException {
        assertFalse(generadorPedidos.isActivo());

        // Iniciar con alta tasa para capturar ráfagas en pocos milisegundos
        generadorPedidos.ajustarTasa(1200); // 1200 pedidos/minuto = 1 cada 50ms
        generadorPedidos.iniciar();
        assertTrue(generadorPedidos.isActivo());

        // Esperar que se generen al menos 10 pedidos continuos sin tope
        Thread.sleep(600);
        List<Pedido> lista = pedidoService.listarPedidos();
        assertTrue(lista.size() >= 10, "El generador debe haber creado al menos 10 pedidos continuos");

        // Verificar variedad de niveles generados
        Set<NivelServicio> nivelesGenerados = lista.stream()
                .map(Pedido::getNivel)
                .collect(Collectors.toSet());
        assertTrue(nivelesGenerados.size() >= 2, "El generador debe producir pedidos con distintos niveles");

        // Ajustar tasa en caliente
        generadorPedidos.ajustarTasa(2400);
        assertEquals(2400, generadorPedidos.getTasaPorMinuto());
        assertTrue(generadorPedidos.isActivo());

        // Detener generador
        generadorPedidos.detener();
        assertFalse(generadorPedidos.isActivo());
    }

    @Test
    @DisplayName("Garantía de liberación de recursos ante interrupción de un hilo de pedido")
    void testLiberacionGarantizadaAnteInterrupcion() throws Exception {
        relojSimulacion.setTimeScaleMs(10000L); // Simulación lenta para atraparlo en vuelo
        Producto prod = catalogoProductos.buscarPorId("PROD-03").orElseThrow();
        Cliente cliente = new Cliente("CLI-INT", "Cliente Interrupción", NivelServicio.ESTANDAR);
        Pedido pedido = new Pedido("PED-INTERRUPT", cliente, List.of(new ItemPedido(prod, 20)));

        Future<?> future = gestorProcesamiento.ejecutar(pedido);
        Thread.sleep(80); // Dejar que inicie y adquiera recursos

        // Simular expropiación por cancelación del hilo
        boolean cancelado = gestorProcesamiento.interrumpirPedido("PED-INTERRUPT");
        assertTrue(cancelado, "El pedido debió ser interrumpido exitosamente");

        // Esperar que el hilo complete su bloque finally
        try {
            future.get(2, TimeUnit.SECONDS);
        } catch (CancellationException | ExecutionException ignored) {
        }

        // Garantía RAII / finally: todos los recursos deben haber vuelto al pool
        assertTrue(pedido.getRecursosRetenidos().isEmpty());
        for (EstadoRecurso estado : gestorRecursos.obtenerEstados()) {
            assertEquals(0, estado.enUso(), "El recurso " + estado.nombre() + " debió quedar libre tras la interrupción");
            assertEquals(estado.total(), estado.disponibles());
        }
    }

    @Test
    @DisplayName("Endpoints REST de alta manual de clientes, pedidos y control del generador")
    void testEndpointsAltaManualYGenerador() throws Exception {
        // 1. Alta manual de cliente
        CrearClienteRequest reqCliente = new CrearClienteRequest("Nueva Empresa S.A.", NivelServicio.EXPRES);
        String resCliente = mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqCliente)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value("Nueva Empresa S.A."))
                .andReturn().getResponse().getContentAsString();

        Cliente nuevoCliente = objectMapper.readValue(resCliente, Cliente.class);

        // 2. Alta manual de pedido
        CrearPedidoRequest reqPedido = new CrearPedidoRequest(
                nuevoCliente.id(),
                List.of(new ItemPedidoRequest("PROD-01", 3), new ItemPedidoRequest("PROD-07", 2))
        );

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqPedido)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.cliente.id").value(nuevoCliente.id()))
                .andExpect(jsonPath("$.totalArticulos").value(5));

        // 3. Control REST del generador
        mockMvc.perform(post("/api/simulacion/generador/iniciar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));

        mockMvc.perform(put("/api/simulacion/generador/tasa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AjustarTasaRequest(12))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasaPorMinuto").value(12));

        mockMvc.perform(get("/api/simulacion/generador"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.tasaPorMinuto").value(12));

        mockMvc.perform(post("/api/simulacion/generador/detener"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }
}
