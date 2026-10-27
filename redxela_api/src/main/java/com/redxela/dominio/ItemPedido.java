package com.redxela.dominio;

/**
 * Detalle de un artículo solicitado dentro de un pedido.
 * 
 * Parámetro de carga de trabajo de un hilo de ejecución.
 * Cada ítem aporta volumen de trabajo y define la tipología de recursos que el hilo demandará.
 */
public record ItemPedido(
        Producto producto,
        TipoMercancia tipoMercancia,
        int cantidad
) {
    public ItemPedido {
        if (producto == null) {
            throw new IllegalArgumentException("El producto no puede ser nulo");
        }
        if (tipoMercancia == null) {
            tipoMercancia = producto.tipoMercancia();
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
    }

    public ItemPedido(Producto producto, int cantidad) {
        this(producto, producto.tipoMercancia(), cantidad);
    }
}
