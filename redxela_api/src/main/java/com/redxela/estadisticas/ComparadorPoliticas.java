package com.redxela.estadisticas;

import com.redxela.api.dto.ComparacionPoliticas;
import com.redxela.api.dto.ComparacionRequest;
import com.redxela.dominio.NivelServicio;
import com.redxela.planificacion.PoliticaPlanificacion;
import com.redxela.planificacion.TipoPolitica;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Compara las tres políticas de planificación sobre una réplica aislada del sistema.
 *
 * Benchmark de algoritmos de planificación (Scheduler Benchmark).
 * Serializa las ejecuciones con un cerrojo justo, genera una única carga con semilla
 * fija y conserva el último resultado sin tocar las colas ni los recursos vivos.
 */
@Service
public class ComparadorPoliticas {

    public static final long SEMILLA_POR_DEFECTO = 2026L;
    public static final int PEDIDOS_POR_DEFECTO = 250;
    private static final int MAXIMO_PEDIDOS = 1000;

    private final GeneradorCargaComparacion generador;
    private final SimuladorPolitica simulador;
    private final Map<TipoPolitica, PoliticaPlanificacion> politicas;
    private final ReentrantLock cerrojo = new ReentrantLock(true);
    private final AtomicReference<ComparacionPoliticas> ultimo = new AtomicReference<>();

    public ComparadorPoliticas(
            GeneradorCargaComparacion generador,
            SimuladorPolitica simulador,
            List<PoliticaPlanificacion> politicasDisponibles
    ) {
        this.generador = generador;
        this.simulador = simulador;
        this.politicas = new EnumMap<>(TipoPolitica.class);
        politicasDisponibles.forEach(politica -> this.politicas.put(politica.getTipo(), politica));
    }

    public ComparacionPoliticas comparar(ComparacionRequest solicitud) {
        long semilla = solicitud == null || solicitud.semilla() == null
                ? SEMILLA_POR_DEFECTO
                : solicitud.semilla();
        int cantidad = solicitud == null || solicitud.cantidadPedidos() == null
                ? PEDIDOS_POR_DEFECTO
                : solicitud.cantidadPedidos();
        validar(cantidad);

        cerrojo.lock();
        try {
            List<GeneradorCargaComparacion.Trabajo> carga = generador.generar(semilla, cantidad);
            List<ComparacionPoliticas.ResultadoPolitica> resultados = new ArrayList<>();
            for (TipoPolitica tipo : TipoPolitica.values()) {
                PoliticaPlanificacion politica = politicas.get(tipo);
                if (politica == null) {
                    throw new IllegalStateException("No está registrada la política " + tipo);
                }
                SimuladorPolitica.ResultadoSimulacion simulacion = simulador.ejecutar(carga, politica);
                resultados.add(resumir(tipo, simulacion));
            }

            ComparacionPoliticas comparacion = new ComparacionPoliticas(
                    semilla,
                    cantidad,
                    System.currentTimeMillis(),
                    List.copyOf(resultados)
            );
            ultimo.set(comparacion);
            return comparacion;
        } finally {
            cerrojo.unlock();
        }
    }

    public Optional<ComparacionPoliticas> ultimoResultado() {
        return Optional.ofNullable(ultimo.get());
    }
    private ComparacionPoliticas.ResultadoPolitica resumir(
            TipoPolitica tipo,
            SimuladorPolitica.ResultadoSimulacion simulacion
    ) {
        List<SimuladorPolitica.Muestra> muestras = simulacion.muestras();
        DoubleSummaryStatistics esperas = muestras.stream()
                .mapToDouble(SimuladorPolitica.Muestra::esperaMinutos)
                .summaryStatistics();
        long dentroSla = muestras.stream().filter(SimuladorPolitica.Muestra::cumpleSla).count();
        double throughput = simulacion.makespanMinutos() > 0
                ? muestras.size() / simulacion.makespanMinutos()
                : 0.0;

        return new ComparacionPoliticas.ResultadoPolitica(
                tipo,
                tipo.getNombreLegible(),
                muestras.size(),
                ServicioEstadisticas.redondear(esperas.getAverage()),
                ServicioEstadisticas.redondear(esperas.getMax()),
                (int) dentroSla,
                ServicioEstadisticas.redondear(porcentaje(dentroSla, muestras.size())),
                ServicioEstadisticas.redondear(throughput),
                porNivel(muestras)
        );
    }

    private List<ComparacionPoliticas.MetricaNivel> porNivel(List<SimuladorPolitica.Muestra> muestras) {
        List<ComparacionPoliticas.MetricaNivel> metricas = new ArrayList<>();
        for (NivelServicio nivel : NivelServicio.values()) {
            List<SimuladorPolitica.Muestra> delNivel = muestras.stream()
                    .filter(muestra -> muestra.nivel() == nivel)
                    .toList();
            DoubleSummaryStatistics esperas = delNivel.stream()
                    .mapToDouble(SimuladorPolitica.Muestra::esperaMinutos)
                    .summaryStatistics();
            long dentro = delNivel.stream().filter(SimuladorPolitica.Muestra::cumpleSla).count();
            metricas.add(new ComparacionPoliticas.MetricaNivel(
                    nivel,
                    nivel.getNombre(),
                    delNivel.size(),
                    (int) dentro,
                    nivel.getTiempoObjetivoMinutos(),
                    ServicioEstadisticas.redondear(esperas.getAverage()),
                    ServicioEstadisticas.redondear(esperas.getMax()),
                    ServicioEstadisticas.redondear(porcentaje(dentro, delNivel.size()))
            ));
        }
        return List.copyOf(metricas);
    }

    private double porcentaje(long parte, int total) {
        return total == 0 ? 0.0 : (100.0 * parte) / total;
    }

    private void validar(int cantidad) {
        if (cantidad < 1 || cantidad > MAXIMO_PEDIDOS) {
            throw new IllegalArgumentException(
                    "La cantidad de pedidos debe estar entre 1 y " + MAXIMO_PEDIDOS
            );
        }
    }
}
