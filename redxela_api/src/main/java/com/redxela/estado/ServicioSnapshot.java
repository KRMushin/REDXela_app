package com.redxela.estado;

import com.redxela.api.dto.SnapshotEstado;
import com.redxela.estadisticas.ServicioEstadisticas;
import com.redxela.interbloqueo.DetectorInterbloqueo;
import com.redxela.planificacion.Despachador;
import com.redxela.recursos.GestorRecursos;
import com.redxela.simulacion.GeneradorPedidos;
import com.redxela.simulacion.RelojSimulacion;
import org.springframework.stereotype.Service;

/**
 * Construye el snapshot completo que consume el frontend en tiempo real.
 *
 * Concepto de SO: Recolector de telemetría del kernel (System State Collector).
 * Lee de forma no invasiva los subsistemas de procesos, recursos, memoria y colas
 * para producir una vista consistente del estado del sistema.
 */
@Service
public class ServicioSnapshot {

    private final MapeadorEstado mapeador;
    private final MapeadorAlmacen mapeadorAlmacen;
    private final GestorRecursos recursos;
    private final Despachador despachador;
    private final GeneradorPedidos generador;
    private final DetectorInterbloqueo detector;
    private final RelojSimulacion reloj;
    private final ServicioEstadisticas estadisticas;

    public ServicioSnapshot(
            MapeadorEstado mapeador,
            MapeadorAlmacen mapeadorAlmacen,
            GestorRecursos recursos,
            Despachador despachador,
            GeneradorPedidos generador,
            DetectorInterbloqueo detector,
            RelojSimulacion reloj,
            ServicioEstadisticas estadisticas
    ) {
        this.mapeador = mapeador;
        this.mapeadorAlmacen = mapeadorAlmacen;
        this.recursos = recursos;
        this.despachador = despachador;
        this.generador = generador;
        this.detector = detector;
        this.reloj = reloj;
        this.estadisticas = estadisticas;
    }

    public SnapshotEstado construir() {
        long ahora = System.currentTimeMillis();
        return new SnapshotEstado(
                ahora,
                ServicioEstadisticas.redondear(reloj.getTiempoSimuladoTranscurridoMinutos()),
                recursos.getModo(),
                despachador.getTipoPoliticaActiva(),
                generador.isActivo(),
                generador.getTasaPorMinuto(),
                mapeador.recursos(),
                mapeador.pedidosEnProceso(),
                mapeador.listaEspera(ahora),
                mapeadorAlmacen.almacen(),
                detector.ultimoReporte(),
                estadisticas.calcularResumen()
        );
    }
}
