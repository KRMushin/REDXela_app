package com.redxela.estadisticas;

import com.redxela.api.dto.EstadisticasSistema;
import com.redxela.api.dto.SnapshotEstado;
import com.redxela.dominio.EstadoPedido;
import com.redxela.dominio.NivelServicio;
import com.redxela.dominio.Pedido;
import com.redxela.dominio.PedidoService;
import com.redxela.recursos.EstadoRecurso;
import com.redxela.recursos.GestorRecursos;
import com.redxela.simulacion.RelojSimulacion;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Calcula las métricas de desempeño globales y desagregadas del centro de distribución.
 *
 * Contabilidad de tiempos de CPU y cumplimiento de plazos
 * (Accounting / Soft Real-Time SLA). Compara el ciclo de vida de cada proceso
 * con el objetivo de su clase de prioridad y reporta la ocupación de recursos.
 */
@Service
public class ServicioEstadisticas {

    private final PedidoService pedidoService;
    private final RelojSimulacion reloj;
    private final GestorRecursos recursos;

    public ServicioEstadisticas(
            PedidoService pedidoService,
            RelojSimulacion reloj,
            GestorRecursos recursos
    ) {
        this.pedidoService = pedidoService;
        this.reloj = reloj;
        this.recursos = recursos;
    }

    /** Resumen global que forma parte del contrato del snapshot. */
    public SnapshotEstado.Estadisticas calcularResumen() {
        List<Pedido> completados = completados();
        int total = completados.size();
        long dentroSla = completados.stream().filter(this::cumpleSla).count();
        double transcurrido = reloj.getTiempoSimuladoTranscurridoMinutos();

        return new SnapshotEstado.Estadisticas(
                total,
                redondear(promedio(completados, this::esperaMinutos)),
                redondear(promedio(completados, this::procesamientoMinutos)),
                redondear(porcentaje(dentroSla, total)),
                redondear(transcurrido > 0 ? total / transcurrido : 0),
                contarEnEstado(EstadoPedido.EN_ESPERA_STOCK),
                contarEnEstado(EstadoPedido.EN_ESPERA_RECURSOS)
        );
    }

    /** Métricas completas: por nivel de servicio y ocupación de cada recurso. */
    public EstadisticasSistema calcularDetalladas() {
        List<Pedido> completados = completados();
        int total = completados.size();
        long dentroSla = completados.stream().filter(this::cumpleSla).count();
        double transcurrido = reloj.getTiempoSimuladoTranscurridoMinutos();

        return new EstadisticasSistema(
                total,
                redondear(promedio(completados, this::esperaMinutos)),
                redondear(promedio(completados, this::procesamientoMinutos)),
                (int) dentroSla,
                redondear(porcentaje(dentroSla, total)),
                redondear(transcurrido > 0 ? total / transcurrido : 0),
                metricasPorNivel(completados),
                ocupacionRecursos(),
                contarEnEstado(EstadoPedido.EN_ESPERA_STOCK),
                contarEnEstado(EstadoPedido.EN_ESPERA_RECURSOS)
        );
    }

    private long contarEnEstado(EstadoPedido estado) {
        return pedidoService.listarPedidos().stream().filter(p -> p.getEstado() == estado).count();
    }

    private List<Pedido> completados() {
        return pedidoService.listarPedidos().stream()
                .filter(p -> p.getEstado() == EstadoPedido.COMPLETADO)
                .filter(p -> p.getTInicio() != null && p.getTFin() != null)
                .toList();
    }

    private List<EstadisticasSistema.MetricaNivel> metricasPorNivel(List<Pedido> completados) {
        List<EstadisticasSistema.MetricaNivel> metricas = new ArrayList<>();
        for (NivelServicio nivel : NivelServicio.values()) {
            List<Pedido> delNivel = completados.stream()
                    .filter(p -> p.getNivel() == nivel)
                    .toList();
            long dentro = delNivel.stream().filter(this::cumpleSla).count();
            metricas.add(new EstadisticasSistema.MetricaNivel(
                    nivel,
                    nivel.getNombre(),
                    delNivel.size(),
                    redondear(promedio(delNivel, this::esperaMinutos)),
                    redondear(promedio(delNivel, this::procesamientoMinutos)),
                    nivel.getTiempoObjetivoMinutos(),
                    (int) dentro,
                    redondear(porcentaje(dentro, delNivel.size()))
            ));
        }
        return List.copyOf(metricas);
    }

    private List<EstadisticasSistema.OcupacionRecurso> ocupacionRecursos() {
        return recursos.obtenerEstados().stream()
                .map(this::ocupacion)
                .toList();
    }

    private EstadisticasSistema.OcupacionRecurso ocupacion(EstadoRecurso estado) {
        double porcentaje = estado.total() > 0
                ? (100.0 * estado.enUso()) / estado.total()
                : 0.0;
        return new EstadisticasSistema.OcupacionRecurso(
                estado.tipo(),
                estado.nombre(),
                estado.total(),
                estado.disponibles(),
                estado.enUso(),
                redondear(porcentaje)
        );
    }

    private double esperaMinutos(Pedido pedido) {
        return (pedido.getTInicio() - pedido.getTCreacion()) / (double) pedido.getEscalaTiempoMs();
    }

    private double procesamientoMinutos(Pedido pedido) {
        return (pedido.getTFin() - pedido.getTInicio()) / (double) pedido.getEscalaTiempoMs();
    }

    private boolean cumpleSla(Pedido pedido) {
        return esperaMinutos(pedido) <= pedido.getNivel().getTiempoObjetivoMinutos();
    }

    private double promedio(List<Pedido> pedidos, java.util.function.ToDoubleFunction<Pedido> metrica) {
        return pedidos.isEmpty() ? 0.0 : pedidos.stream().mapToDouble(metrica).average().orElse(0.0);
    }

    private double porcentaje(long parte, int total) {
        return total == 0 ? 0.0 : (100.0 * parte) / total;
    }

    public static double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
