package com.redxela.simulacion;

import com.redxela.config.SimulacionProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reloj virtual del centro de distribución REDXela.
 * 
 * Concepto de SO: Temporizador del Kernel y Escala Temporal Virtual (Virtual Time / System Timer).
 * Traduce el tiempo de trabajo del modelo simulado (minutos virtuales) a tiempo real (milisegundos)
 * mediante un factor de compresión o dilatación temporal configurable en caliente.
 */
@Component
public class RelojSimulacion {

    private final AtomicLong timeScaleMs;
    private final long tInicioRealMs;

    public RelojSimulacion(SimulacionProperties properties) {
        long scale = properties.timeScaleMs() > 0 ? properties.timeScaleMs() : 1000L;
        this.timeScaleMs = new AtomicLong(scale);
        this.tInicioRealMs = System.currentTimeMillis();
    }

    public long convertirMinutosSimuladosAMsReales(double minutosSimulados) {
        return Math.max(1L, (long) (minutosSimulados * timeScaleMs.get()));
    }

    public double convertirMsRealesAMinutosSimulados(long msReales) {
        return (double) msReales / timeScaleMs.get();
    }

    public double getTiempoSimuladoTranscurridoMinutos() {
        long deltaReal = System.currentTimeMillis() - tInicioRealMs;
        return convertirMsRealesAMinutosSimulados(deltaReal);
    }

    public void pausarSimulado(double minutosSimulados) throws InterruptedException {
        long msReales = convertirMinutosSimuladosAMsReales(minutosSimulados);
        TimeUnit.MILLISECONDS.sleep(msReales);
    }

    public long getTimeScaleMs() {
        return timeScaleMs.get();
    }

    public void setTimeScaleMs(long nuevoTimeScaleMs) {
        if (nuevoTimeScaleMs > 0) {
            this.timeScaleMs.set(nuevoTimeScaleMs);
        }
    }
}
