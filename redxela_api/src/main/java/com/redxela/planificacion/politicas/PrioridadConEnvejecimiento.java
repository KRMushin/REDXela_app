package com.redxela.planificacion.politicas;

import com.redxela.dominio.Pedido;
import com.redxela.planificacion.PoliticaPlanificacion;
import com.redxela.planificacion.TipoPolitica;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * Política de planificación por prioridad con técnica de envejecimiento (Aging).
 * 
 * Concepto de SO: Solución a la Inanición (Starvation Avoidance via Aging).
 * Incrementa progresivamente la prioridad efectiva de un proceso a medida que su tiempo
 * de espera acumulado se aproxima o supera su tiempo límite de servicio (SLA).
 */
@Component
public class PrioridadConEnvejecimiento implements PoliticaPlanificacion {

    @Override
    public TipoPolitica getTipo() {
        return TipoPolitica.PRIORIDAD_CON_ENVEJECIMIENTO;
    }

    @Override
    public Comparator<Pedido> obtenerComparador(long tiempoActualMs) {
        return Comparator
                .comparingDouble((Pedido p) -> calcularPrioridadEfectiva(p, tiempoActualMs))
                .thenComparingLong(Pedido::getTCreacion)
                .thenComparing(Pedido::getId);
    }

    @Override
    public double calcularPrioridadEfectiva(Pedido pedido, long tiempoActualMs) {
        double nivelBase = pedido.getNivel().getNivel();
        long objetivoMinutos = pedido.getNivel().getTiempoObjetivoMinutos();

        // Conversión de tiempo de espera a minutos reales o simulados
        double esperaMinutos = Math.max(0.0, (tiempoActualMs - pedido.getTCreacion()) / 60000.0);
        double denominadorObjetivo = (objetivoMinutos <= 0) ? 1.0 : (double) objetivoMinutos;

        // La relación espera/objetivo reduce el valor numérico (aumenta su prioridad)
        double ratioEnvejecimiento = esperaMinutos / denominadorObjetivo;
        return nivelBase - (ratioEnvejecimiento * 2.0);
    }

    @Override
    public String explicarPrioridad(Pedido pedido, long tiempoActualMs) {
        double pEfectiva = calcularPrioridadEfectiva(pedido, tiempoActualMs);
        double esperaMin = Math.max(0.0, (tiempoActualMs - pedido.getTCreacion()) / 60000.0);
        return String.format(
                "Prioridad efectiva: %.2f (Nivel %s, Espera: %.1f min / SLA: %d min)",
                pEfectiva,
                pedido.getNivel().getNombre(),
                esperaMin,
                pedido.getNivel().getTiempoObjetivoMinutos()
        );
    }
}
