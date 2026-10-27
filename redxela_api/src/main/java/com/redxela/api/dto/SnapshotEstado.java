package com.redxela.api.dto;

import com.redxela.dominio.EstadoPedido;
import com.redxela.dominio.NivelServicio;
import com.redxela.dominio.TipoRecurso;
import com.redxela.interbloqueo.ReporteInterbloqueo;
import com.redxela.planificacion.TipoPolitica;
import com.redxela.recursos.ModoPrevencion;

import java.util.List;

/**
 * Snapshot completo del estado del centro de distribución para la interfaz en vivo.
 * Instantánea coherente del estado del sistema (System State Snapshot),
 * equivalente a leer /proc en Linux: procesos, recursos, memoria física, colas y métricas
 * en un único documento consistente para observabilidad.
 */
public record SnapshotEstado(
        long timestamp,
        double tiempoSimuladoMinutos,
        ModoPrevencion modoPrevencion,
        TipoPolitica politicaPlanificacion,
        boolean generadorActivo,
        int tasaGeneradorPorMinuto,
        List<RecursoEstado> recursos,
        List<PedidoProceso> pedidosEnProceso,
        List<ElementoEspera> listaEspera,
        AlmacenEstado almacen,
        ReporteInterbloqueo interbloqueo,
        Estadisticas estadisticas
) {

    public sealed interface RecursoEstado permits RecursoSimple, RecursoEscaner {}

    public record RecursoSimple(
            TipoRecurso tipo,
            String nombre,
            int total,
            int disponibles,
            int enUso,
            List<String> pedidosAsignados,
            List<String> pedidosEnEspera
    ) implements RecursoEstado {}

    public record RecursoEscaner(
            TipoRecurso tipo,
            String nombre,
            int total,
            int disponibles,
            int enUso,
            String retenidoPor,
            List<String> pedidosAsignados,
            List<String> pedidosEnEspera
    ) implements RecursoEstado {}

    public record PedidoProceso(
            String id,
            String clienteId,
            String clienteNombre,
            NivelServicio nivelServicio,
            int totalArticulos,
            EstadoPedido estado,
            int progreso,
            long tiempoRestanteEstimadoSegundos,
            List<TipoRecurso> recursosRetenidos,
            TipoRecurso recursoEsperado
    ) {}

    public record ElementoEspera(
            String id,
            String clienteNombre,
            NivelServicio nivelServicio,
            int totalArticulos,
            EstadoPedido estado,
            long tCreacion,
            double tiempoEsperaMinutos,
            double prioridadCalculada,
            String motivoPosicion
    ) {}

    public record AlmacenEstado(
            int capacidadTotal,
            int unidadesOcupadas,
            List<UbicacionEstado> ubicaciones,
            ColaRecepcion colaRecepcion
    ) {}

    public record UbicacionEstado(
            String id,
            int pasillo,
            int nivel,
            int posicion,
            String loteId,
            String productoId,
            String productoNombre,
            int unidades,
            int capacidad
    ) {}

    public record ColaRecepcion(
            int elementosEnCola,
            int capacidadMaxima
    ) {}

    public record Estadisticas(
            int pedidosCompletados,
            double tiempoEsperaPromedioMinutos,
            double tiempoProcesamientoPromedioMinutos,
            double cumplimientoSlaPorcentaje,
            double tasaThroughputPorMinuto,
            long pedidosEnEsperaStock,
            long pedidosEnEsperaRecursos
    ) {}
}
