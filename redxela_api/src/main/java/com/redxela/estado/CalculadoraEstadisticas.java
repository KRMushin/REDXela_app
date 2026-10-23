package com.redxela.estado;

import com.redxela.api.dto.SnapshotEstado;
import com.redxela.dominio.EstadoPedido;
import com.redxela.dominio.Pedido;
import com.redxela.dominio.PedidoService;
import com.redxela.simulacion.RelojSimulacion;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Calcula las métricas básicas de desempeño del centro de distribución.
 *
 * Concepto de SO: Contabilidad de tiempos de CPU y cumplimiento de plazos
 * (Accounting / Soft Real-Time SLA). Compara el ciclo de vida de cada proceso
 * con el objetivo de su clase de prioridad.
 */
@Component
public class CalculadoraEstadisticas {

    private final PedidoService pedidoService;
    private final RelojSimulacion reloj;

    public CalculadoraEstadisticas(PedidoService pedidoService, RelojSimulacion reloj) {
        this.pedidoService = pedidoService;
        this.reloj = reloj;
    }

    public SnapshotEstado.Estadisticas calcular() {
        List<Pedido> completados = pedidoService.listarPedidos().stream()
                .filter(p -> p.getEstado() == EstadoPedido.COMPLETADO)
                .filter(p -> p.getTInicio() != null && p.getTFin() != null)
                .toList();

        int total = completados.size();
        double espera = total == 0 ? 0 : completados.stream()
                .mapToDouble(this::esperaMinutos)
                .average().orElse(0);
        double procesamiento = total == 0 ? 0 : completados.stream()
                .mapToDouble(this::procesamientoMinutos)
                .average().orElse(0);
        long dentroSla = completados.stream().filter(this::cumpleSla).count();
        double sla = total == 0 ? 0 : (100.0 * dentroSla) / total;

        double transcurrido = reloj.getTiempoSimuladoTranscurridoMinutos();
        double throughput = transcurrido > 0 ? total / transcurrido : 0;

        return new SnapshotEstado.Estadisticas(
                total,
                redondear(espera),
                redondear(procesamiento),
                redondear(sla),
                redondear(throughput)
        );
    }

    private double esperaMinutos(Pedido pedido) {
        return reloj.convertirMsRealesAMinutosSimulados(pedido.getTInicio() - pedido.getTCreacion());
    }

    private double procesamientoMinutos(Pedido pedido) {
        return reloj.convertirMsRealesAMinutosSimulados(pedido.getTFin() - pedido.getTInicio());
    }

    private boolean cumpleSla(Pedido pedido) {
        double ciclo = reloj.convertirMsRealesAMinutosSimulados(pedido.getTFin() - pedido.getTCreacion());
        return ciclo <= pedido.getNivel().getTiempoObjetivoMinutos();
    }

    public static double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
