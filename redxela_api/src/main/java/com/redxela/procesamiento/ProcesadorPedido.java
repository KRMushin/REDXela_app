package com.redxela.procesamiento;

import com.redxela.dominio.EstadoPedido;
import com.redxela.dominio.ItemPedido;
import com.redxela.dominio.Pedido;
import com.redxela.dominio.TipoRecurso;
import com.redxela.recursos.GestorRecursos;
import com.redxela.simulacion.RelojSimulacion;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tarea de ejecución concurrente que procesa un pedido individual en su propio hilo.
 * 
 * Concepto de SO: Hilo de Trabajo (Worker Thread / Task Execution).
 * Representa la ráfaga de CPU y asignación de periféricos de un proceso. Solicita recursos
 * según la política del sistema, ejecuta el cómputo y garantiza la devolución de recursos
 * al sistema mediante un bloque finally (mecanismo RAII / Clean Exit).
 */
public class ProcesadorPedido implements Runnable {

    private static final double MINUTOS_POR_ARTICULO = 0.1;
    private static final int PASOS_PROGRESO = 10;

    private final Pedido pedido;
    private final GestorRecursos gestorRecursos;
    private final RelojSimulacion reloj;

    public ProcesadorPedido(Pedido pedido, GestorRecursos gestorRecursos, RelojSimulacion reloj) {
        this.pedido = pedido;
        this.gestorRecursos = gestorRecursos;
        this.reloj = reloj;
    }

    @Override
    public void run() {
        if (pedido.getTInicio() == null) {
            pedido.setTInicio(System.currentTimeMillis());
        }
        try {
            // 1. Identificar recursos requeridos por los artículos del pedido
            List<TipoRecurso> requeridos = recopilarRecursosRequeridos();
            List<TipoRecurso> ordenados = gestorRecursos.determinarOrdenAdquisicion(requeridos);

            // 2. Adquirir recursos paso a paso
            for (TipoRecurso recurso : ordenados) {
                gestorRecursos.adquirirRecurso(pedido, recurso);
            }

            // 3. Ejecutar simulación de empaque y procesamiento
            pedido.setEstado(EstadoPedido.EN_PROCESO);
            ejecutarTrabajo();

            // 4. Finalización exitosa
            pedido.setProgreso(100);
            pedido.setEstado(EstadoPedido.COMPLETADO);
            pedido.setTFin(System.currentTimeMillis());

        } catch (InterruptedException e) {
            // Manejo de expropiación (preemption): se restaura la bandera del hilo
            Thread.currentThread().interrupt();
            if (pedido.getEstado() != EstadoPedido.COMPLETADO) {
                pedido.setEstado(EstadoPedido.BLOQUEADO);
            }
        } finally {
            // Regla obligatoria: liberación garantizada de todo recurso en finally
            gestorRecursos.liberarTodos(pedido);
        }
    }

    private List<TipoRecurso> recopilarRecursosRequeridos() {
        Set<TipoRecurso> set = new LinkedHashSet<>();
        for (ItemPedido item : pedido.getItems()) {
            set.addAll(item.tipoMercancia().getRecursosRequeridos());
        }
        return new ArrayList<>(set);
    }

    private void ejecutarTrabajo() throws InterruptedException {
        double duracionMinutos = Math.max(0.1, pedido.getTotalArticulos() * MINUTOS_POR_ARTICULO);
        long duracionTotalMs = reloj.convertirMinutosSimuladosAMsReales(duracionMinutos);
        long pasoMs = Math.max(10L, duracionTotalMs / PASOS_PROGRESO);

        for (int i = 1; i <= PASOS_PROGRESO; i++) {
            Thread.sleep(pasoMs);
            int porcentaje = (i * 100) / PASOS_PROGRESO;
            pedido.setProgreso(porcentaje);
        }
    }

    public Pedido getPedido() { return pedido; }
}
