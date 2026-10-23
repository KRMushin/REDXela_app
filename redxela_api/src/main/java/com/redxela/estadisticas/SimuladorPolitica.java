package com.redxela.estadisticas;

import com.redxela.dominio.EstadoPedido;
import com.redxela.dominio.Pedido;
import com.redxela.planificacion.Despachador;
import com.redxela.planificacion.ListaEspera;
import com.redxela.planificacion.PoliticaPlanificacion;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ejecuta una réplica aislada y determinista del planificador sobre una carga fija.
 *
 * Concepto de SO: Simulador de planificación de CPU de un solo procesador.
 * Modela llegadas, cola de listos y ejecución no expropiativa sin compartir colas,
 * reloj, hilos ni recursos con la simulación viva del centro de distribución.
 */
@Component
public class SimuladorPolitica {

    private static final double MINUTOS_POR_ARTICULO = 0.1;

    private final GeneradorCargaComparacion generador;

    public SimuladorPolitica(GeneradorCargaComparacion generador) {
        this.generador = generador;
    }

    public ResultadoSimulacion ejecutar(
            List<GeneradorCargaComparacion.Trabajo> carga,
            PoliticaPlanificacion politica
    ) {
        List<Pedido> pedidos = carga.stream().map(generador::crearPedido).toList();
        ListaEspera cola = new ListaEspera();
        Despachador despachador = new Despachador(cola, List.of(politica));
        despachador.cambiarPolitica(politica.getTipo());

        List<Muestra> muestras = new ArrayList<>(pedidos.size());
        int siguienteLlegada = 0;
        double relojMinutos = 0.0;

        while (siguienteLlegada < pedidos.size() || !cola.estaVacia()) {
            while (siguienteLlegada < pedidos.size()
                    && aMinutos(pedidos.get(siguienteLlegada).getTCreacion()) <= relojMinutos) {
                cola.agregar(pedidos.get(siguienteLlegada++));
            }

            Optional<Pedido> despachado = despachador.despacharSiguiente(aMs(relojMinutos));
            if (despachado.isEmpty()) {
                // La CPU queda ociosa hasta la próxima llegada de la carga.
                if (siguienteLlegada < pedidos.size()) {
                    relojMinutos = aMinutos(pedidos.get(siguienteLlegada).getTCreacion());
                }
                continue;
            }

            Pedido pedido = despachado.get();
            double creacion = aMinutos(pedido.getTCreacion());
            double duracion = Math.max(0.1, pedido.getTotalArticulos() * MINUTOS_POR_ARTICULO);
            double inicio = relojMinutos;
            relojMinutos += duracion;

            pedido.setProgreso(100);
            pedido.setEstado(EstadoPedido.COMPLETADO);
            pedido.setTFin(aMs(relojMinutos));

            double espera = Math.max(0.0, inicio - creacion);
            double ciclo = Math.max(0.0, relojMinutos - creacion);
            muestras.add(new Muestra(
                    pedido.getNivel(),
                    espera,
                    ciclo <= pedido.getNivel().getTiempoObjetivoMinutos()
            ));
        }

        return new ResultadoSimulacion(relojMinutos, List.copyOf(muestras));
    }

    private double aMinutos(long ms) {
        return (double) ms / 60_000.0;
    }

    private long aMs(double minutos) {
        return Math.round(minutos * 60_000.0);
    }

    /** Muestra de espera y cumplimiento SLA de un proceso simulado. */
    public record Muestra(
            com.redxela.dominio.NivelServicio nivel,
            double esperaMinutos,
            boolean cumpleSla
    ) {}

    /** Resultado aislado de una política: makespan y muestras por proceso. */
    public record ResultadoSimulacion(
            double makespanMinutos,
            List<Muestra> muestras
    ) {}
}
