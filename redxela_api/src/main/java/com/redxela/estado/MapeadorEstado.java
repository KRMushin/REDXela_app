package com.redxela.estado;

import com.redxela.api.dto.SnapshotEstado;
import com.redxela.dominio.EstadoPedido;
import com.redxela.dominio.Pedido;
import com.redxela.dominio.PedidoService;
import com.redxela.dominio.TipoRecurso;
import com.redxela.planificacion.Despachador;
import com.redxela.planificacion.ElementoListaEspera;
import com.redxela.recursos.EstadoRecurso;
import com.redxela.recursos.GestorRecursos;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Traduce las estructuras internas del sistema a los DTO del snapshot.
 *
 * Concepto de SO: Adaptador de telemetría (Procfs Adapter). Desacopla el modelo
 * interno del kernel simulado del contrato JSON que consume la interfaz.
 */
@Component
public class MapeadorEstado {

    private final GestorRecursos recursos;
    private final Despachador despachador;
    private final PedidoService pedidos;

    public MapeadorEstado(
            GestorRecursos recursos,
            Despachador despachador,
            PedidoService pedidos
    ) {
        this.recursos = recursos;
        this.despachador = despachador;
        this.pedidos = pedidos;
    }

    List<SnapshotEstado.RecursoEstado> recursos() {
        return recursos.obtenerEstados().stream().map(this::recurso).toList();
    }

    private SnapshotEstado.RecursoEstado recurso(EstadoRecurso estado) {
        if (estado.tipo() == TipoRecurso.ESCANER) {
            String retenidoPor = estado.pedidosAsignados().isEmpty()
                    ? null
                    : estado.pedidosAsignados().getFirst();
            return new SnapshotEstado.RecursoEscaner(
                    estado.tipo(),
                    estado.nombre(),
                    estado.total(),
                    estado.disponibles(),
                    estado.enUso(),
                    retenidoPor,
                    List.of(),
                    estado.pedidosEnEspera()
            );
        }
        return new SnapshotEstado.RecursoSimple(
                estado.tipo(),
                estado.nombre(),
                estado.total(),
                estado.disponibles(),
                estado.enUso(),
                estado.pedidosAsignados(),
                estado.pedidosEnEspera()
        );
    }

    List<SnapshotEstado.PedidoProceso> pedidosEnProceso() {
        return pedidos.listarPedidos().stream()
                .filter(p -> p.getEstado() == EstadoPedido.EN_PROCESO)
                .sorted(Comparator.comparing(Pedido::getId))
                .map(this::pedido)
                .toList();
    }

    private SnapshotEstado.PedidoProceso pedido(Pedido pedido) {
        return new SnapshotEstado.PedidoProceso(
                pedido.getId(),
                pedido.getCliente().id(),
                pedido.getCliente().nombre(),
                pedido.getNivel(),
                pedido.getTotalArticulos(),
                pedido.getEstado(),
                pedido.getProgreso(),
                tiempoRestanteSegundos(pedido),
                pedido.getRecursosRetenidos().stream()
                        .sorted(Comparator.comparingInt(TipoRecurso::getOrdenGlobal))
                        .toList(),
                pedido.getRecursoEsperado()
        );
    }

    /**
     * Estimación en segundos simulados: cada artículo consume 0.1 minuto virtual.
     */
    private long tiempoRestanteSegundos(Pedido pedido) {
        double duracionMinutos = Math.max(0.1, pedido.getTotalArticulos() * 0.1);
        double factorRestante = 1.0 - (pedido.getProgreso() / 100.0);
        return Math.max(0L, Math.round(duracionMinutos * factorRestante * 60.0));
    }

    List<SnapshotEstado.ElementoEspera> listaEspera(long ahora) {
        return despachador.obtenerVistaListaEspera(ahora).stream()
                .map(this::elementoEspera)
                .toList();
    }

    private SnapshotEstado.ElementoEspera elementoEspera(ElementoListaEspera elemento) {
        return new SnapshotEstado.ElementoEspera(
                elemento.pedidoId(),
                elemento.clienteNombre(),
                elemento.nivelServicio(),
                elemento.totalArticulos(),
                EstadoPedido.EN_ESPERA_RECURSOS,
                elemento.tCreacion(),
                CalculadoraEstadisticas.redondear(elemento.tiempoEsperaMinutos()),
                CalculadoraEstadisticas.redondear(elemento.prioridadCalculada()),
                elemento.motivoPosicion()
        );
    }
}
