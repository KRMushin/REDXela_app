package com.redxela.interbloqueo;

import com.redxela.dominio.PedidoService;
import com.redxela.procesamiento.GestorProcesamiento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Resolución manual del interbloqueo elegida por el operador.
 * 
 * Concepto de SO: Recuperación por expropiación (preemption). El proceso ganador
 * conserva los recursos disputados; los perdedores reciben una interrupción, su
 * bloque finally libera todo lo retenido y vuelven a la lista de espera para ser
 * atendidos más adelante. Se rompe la espera circular sin reiniciar el sistema.
 */
@Service
public class ResolutorInterbloqueo {

    private static final Logger log = LoggerFactory.getLogger(ResolutorInterbloqueo.class);
    private static final long ESPERA_LIBERACION_MS = 2_000L;

    private final DetectorInterbloqueo detector;
    private final GestorProcesamiento gestorProcesamiento;
    private final PedidoService pedidoService;

    public ResolutorInterbloqueo(
            DetectorInterbloqueo detector,
            GestorProcesamiento gestorProcesamiento,
            PedidoService pedidoService
    ) {
        this.detector = detector;
        this.gestorProcesamiento = gestorProcesamiento;
        this.pedidoService = pedidoService;
    }

    /**
     * Expropia a todos los pedidos del reporte salvo al ganador y devuelve el
     * reporte recalculado después de la liberación de recursos.
     */
    public ReporteInterbloqueo resolver(String pedidoGanadorId) {
        ReporteInterbloqueo reporte = detector.ultimoReporte();
        if (!reporte.hayInterbloqueo()) {
            reporte = detector.detectarAhora();
        }
        if (!reporte.hayInterbloqueo()) {
            throw new IllegalStateException("No hay interbloqueo activo para resolver");
        }
        if (!reporte.pedidosInvolucrados().contains(pedidoGanadorId)) {
            throw new IllegalArgumentException(
                    "El pedido ganador no participa del interbloqueo: " + pedidoGanadorId);
        }

        List<String> perdedores = reporte.pedidosInvolucrados().stream()
                .filter(id -> !id.equals(pedidoGanadorId))
                .toList();

        for (String perdedor : perdedores) {
            expropiar(perdedor);
        }

        // Los perdedores quedan en la cola de listos; el despachador los reanudará
        // cuando el ganador libere sus recursos (evento de finalización).
        return detector.detectarAhora();
    }

    private void expropiar(String pedidoId) {
        boolean interrumpido = gestorProcesamiento.interrumpirPedido(pedidoId);
        if (!interrumpido) {
            return; // la tarea ya había terminado
        }
        // Se espera a que el bloque finally libere los recursos antes de reencolar
        gestorProcesamiento.esperarFinalizacion(pedidoId, ESPERA_LIBERACION_MS);
        if (!pedidoService.devolverAListaEspera(pedidoId)) {
            log.warn("No se pudo reencolar al pedido expropiado {}", pedidoId);
        }
    }
}
