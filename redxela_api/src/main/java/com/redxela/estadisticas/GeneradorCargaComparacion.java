package com.redxela.estadisticas;

import com.redxela.dominio.CatalogoProductos;
import com.redxela.dominio.Cliente;
import com.redxela.dominio.ItemPedido;
import com.redxela.dominio.NivelServicio;
import com.redxela.dominio.Pedido;
import com.redxela.dominio.Producto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Genera una carga sintética reproducible para comparar políticas de planificación.
 *
 * Carga de trabajo sintética con semilla fija (Deterministic Workload).
 * La misma secuencia de llegadas, niveles y ráfagas se reutiliza bajo cada algoritmo,
 * garantizando que las diferencias observadas provengan solo del planificador.
 */
@Component
public class GeneradorCargaComparacion {

    private static final double INTERVALO_LLEGADA_MINUTOS = 0.4;
    private static final int MAX_ARTICULOS = 12;

    private final CatalogoProductos catalogo;

    public GeneradorCargaComparacion(CatalogoProductos catalogo) {
        this.catalogo = catalogo;
    }

    public List<Trabajo> generar(long semilla, int cantidadPedidos) {
        Random aleatorio = new Random(semilla);
        List<Producto> productos = catalogo.listarTodos().stream()
                .sorted(Comparator.comparing(Producto::id))
                .toList();
        List<Trabajo> trabajos = new ArrayList<>(cantidadPedidos);
        NivelServicio[] niveles = NivelServicio.values();
        double llegadaMinutos = 0.0;

        for (int i = 0; i < cantidadPedidos; i++) {
            NivelServicio nivel = niveles[i % niveles.length];
            Producto producto = productos.get(aleatorio.nextInt(productos.size()));
            int articulos = 1 + aleatorio.nextInt(MAX_ARTICULOS);
            Cliente cliente = new Cliente(
                    "SIM-CLI-%02d".formatted((i % niveles.length) + 1),
                    "Cliente simulado %02d".formatted(i + 1),
                    nivel
            );
            trabajos.add(new Trabajo(
                    "SIM-PED-%03d".formatted(i + 1),
                    cliente,
                    producto,
                    articulos,
                    Math.round(llegadaMinutos * 60_000)
            ));
            llegadaMinutos += INTERVALO_LLEGADA_MINUTOS;
        }
        return List.copyOf(trabajos);
    }

    /** Materializa una copia nueva del pedido para que cada política parta del mismo estado. */
    public Pedido crearPedido(Trabajo trabajo) {
        List<ItemPedido> items = List.of(new ItemPedido(trabajo.producto(), trabajo.articulos()));
        return new Pedido(trabajo.id(), trabajo.cliente(), items, trabajo.tCreacionMs());
    }

    /** Descriptor inmutable de una unidad de carga. */
    public record Trabajo(
            String id,
            Cliente cliente,
            Producto producto,
            int articulos,
            long tCreacionMs
    ) {}
}
