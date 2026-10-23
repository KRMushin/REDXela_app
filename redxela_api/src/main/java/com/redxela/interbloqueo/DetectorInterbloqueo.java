package com.redxela.interbloqueo;

import com.redxela.config.SimulacionProperties;
import com.redxela.dominio.TipoRecurso;
import com.redxela.recursos.GestorRecursos;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Detector periódico de interbloqueos basado en el grafo de espera.
 * 
 * Concepto de SO: Detección de interbloqueo mediante grafo de espera y búsqueda DFS
 * de ciclos. Se ejecuta en un hilo de monitoreo cada cierto intervalo, sin bloquear
 * a los procesos que trabajan, y publica un reporte cuando encuentra una espera circular
 * cuyos retenedores están todos bloqueados.
 */
@Component
public class DetectorInterbloqueo {

    private static final Logger log = LoggerFactory.getLogger(DetectorInterbloqueo.class);

    private final GestorRecursos gestorRecursos;
    private final long intervaloMs;
    private final AtomicReference<ReporteInterbloqueo> ultimoReporte =
            new AtomicReference<>(ReporteInterbloqueo.sinInterbloqueo());

    private final ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor(tarea -> {
        Thread hilo = new Thread(tarea, "detector-interbloqueo");
        hilo.setDaemon(true);
        return hilo;
    });

    public DetectorInterbloqueo(GestorRecursos gestorRecursos, SimulacionProperties properties) {
        this.gestorRecursos = gestorRecursos;
        this.intervaloMs = properties.deteccionInterbloqueoMs() > 0
                ? properties.deteccionInterbloqueoMs()
                : 200L;
    }

    @PostConstruct
    public void iniciar() {
        monitor.scheduleWithFixedDelay(this::detectarYPublicar, intervaloMs, intervaloMs, TimeUnit.MILLISECONDS);
    }

    private void detectarYPublicar() {
        try {
            detectarAhora();
        } catch (RuntimeException e) {
            // Un fallo del monitor no debe derribar al sistema
            log.error("Error al analizar el grafo de espera", e);
        }
    }

    /**
     * Analiza el estado actual y actualiza el último reporte.
     */
    public ReporteInterbloqueo detectarAhora() {
        GrafoEspera grafo = GrafoEspera.construir(gestorRecursos.obtenerEstados());
        List<String> ciclo = grafo.buscarCiclo();

        ReporteInterbloqueo reporte = ciclo.isEmpty()
                ? ReporteInterbloqueo.sinInterbloqueo()
                : construirReporte(grafo, ciclo);

        ultimoReporte.set(reporte);
        return reporte;
    }

    private ReporteInterbloqueo construirReporte(GrafoEspera grafo, List<String> ciclo) {
        Set<TipoRecurso> recursos = grafo.recursosDelCiclo(ciclo);
        if (recursos.isEmpty() || !todosRetenedoresBloqueados(grafo, recursos)) {
            return ReporteInterbloqueo.sinInterbloqueo();
        }

        List<String> pedidos = new ArrayList<>(new TreeSet<>(involucrados(grafo, ciclo, recursos)));
        List<TipoRecurso> recursosOrdenados = new ArrayList<>(
                recursos.stream().sorted(Comparator.comparing(TipoRecurso::name)).toList());
        return new ReporteInterbloqueo(true, pedidos, recursosOrdenados, explicar(ciclo, grafo));
    }

    /**
     * Operaciones involucradas: el ciclo detectado más todos los retenedores de los
     * recursos en disputa (todos ellos están bloqueados, según la verificación previa).
     */
    private Set<String> involucrados(GrafoEspera grafo, List<String> ciclo, Set<TipoRecurso> recursos) {
        Set<String> involucrados = new TreeSet<>(ciclo);
        for (TipoRecurso recurso : recursos) {
            involucrados.addAll(grafo.retenedoresDe(recurso));
        }
        return involucrados;
    }

    /**
     * Confirma que ningún retenedor de los recursos en disputa pueda liberarlos por sí solo.
     */
    private boolean todosRetenedoresBloqueados(GrafoEspera grafo, Set<TipoRecurso> recursos) {
        Set<String> bloqueados = grafo.bloqueados();
        for (TipoRecurso recurso : recursos) {
            for (String retenedor : grafo.retenedoresDe(recurso)) {
                if (!bloqueados.contains(retenedor)) {
                    return false;
                }
            }
        }
        return true;
    }

    private String explicar(List<String> ciclo, GrafoEspera grafo) {
        StringBuilder texto = new StringBuilder("Espera circular detectada: ");
        for (int i = 0; i < ciclo.size(); i++) {
            String actual = ciclo.get(i);
            String siguiente = ciclo.get((i + 1) % ciclo.size());
            grafo.aristas().stream()
                    .filter(a -> a.espera().equals(actual) && a.retiene().equals(siguiente))
                    .findFirst()
                    .ifPresent(a -> texto.append(actual)
                            .append(" espera ")
                            .append(a.recurso().getNombre())
                            .append(" retenido por ")
                            .append(siguiente)
                            .append("; "));
        }
        return texto.append("todos los retenedores están bloqueados y no pueden liberar sus recursos.")
                .toString();
    }

    public ReporteInterbloqueo ultimoReporte() {
        return ultimoReporte.get();
    }

    @PreDestroy
    public void cerrar() {
        monitor.shutdownNow();
    }
}
