package com.redxela.simulacion;

import com.redxela.dominio.PedidoService;
import com.redxela.inventario.ServicioInventario;
import com.redxela.interbloqueo.DetectorInterbloqueo;
import com.redxela.interbloqueo.SincronizadorConflicto;
import com.redxela.planificacion.Despachador;
import com.redxela.planificacion.TipoPolitica;
import com.redxela.procesamiento.GestorProcesamiento;
import com.redxela.recepcion.AreaRecepcion;
import com.redxela.recursos.GestorRecursos;
import com.redxela.recursos.ModoPrevencion;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Reinicia todos los subsistemas de la simulación a su estado inicial.
 *
 * Concepto de SO: Reinicio controlado del sistema (System Reboot). Detiene las
 * tareas, espera la liberación de recursos, vacía las colas y vuelve a cargar
 * el estado inicial sin reiniciar el proceso de la aplicación.
 */
@Service
public class ServicioReinicio {

    private final GeneradorPedidos generador;
    private final GestorProcesamiento procesamiento;
    private final DetectorInterbloqueo detector;
    private final SincronizadorConflicto sincronizador;
    private final PedidoService pedidos;
    private final GestorRecursos recursos;
    private final Despachador despachador;
    private final ServicioInventario inventario;
    private final AreaRecepcion recepcion;
    private final RelojSimulacion reloj;

    public ServicioReinicio(
            GeneradorPedidos generador,
            GestorProcesamiento procesamiento,
            DetectorInterbloqueo detector,
            SincronizadorConflicto sincronizador,
            PedidoService pedidos,
            GestorRecursos recursos,
            Despachador despachador,
            ServicioInventario inventario,
            AreaRecepcion recepcion,
            RelojSimulacion reloj
    ) {
        this.generador = generador;
        this.procesamiento = procesamiento;
        this.detector = detector;
        this.sincronizador = sincronizador;
        this.pedidos = pedidos;
        this.recursos = recursos;
        this.despachador = despachador;
        this.inventario = inventario;
        this.recepcion = recepcion;
        this.reloj = reloj;
    }

    public Map<String, Object> reiniciar() {
        generador.detener();
        procesamiento.cancelarTodas();
        sincronizador.limpiar();
        pedidos.limpiarParaPruebas();

        recursos.cambiarModo(ModoPrevencion.PREVENCION_ACTIVA);
        recursos.reiniciar();
        despachador.cambiarPolitica(TipoPolitica.PRIORIDAD_CON_ENVEJECIMIENTO);

        recepcion.vaciar();
        inventario.reiniciar();
        reloj.reiniciar();
        detector.detectarAhora();

        return Map.of(
                "mensaje", "Simulación reiniciada correctamente",
                "timestamp", System.currentTimeMillis()
        );
    }
}
