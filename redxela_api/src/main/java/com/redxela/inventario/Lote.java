package com.redxela.inventario;

import com.redxela.almacen.AsignacionCelda;

import java.util.List;

/**
 * Registro de un lote recibido y de las ubicaciones físicas que ocupa.
 * 
 * Concepto de SO: Entrada de la tabla de páginas de un segmento (Segment/Page Mapping Entry):
 * relaciona el identificador lógico del lote con sus marcos físicos dispersos y
 * conserva el remanente que no pudo almacenarse (pendiente visible).
 */
public record Lote(
        String loteId,
        String productoId,
        int cantidadRecibida,
        int unidadesPendientes,
        List<AsignacionCelda> ubicaciones,
        long tRegistro
) {
    public Lote {
        if (loteId == null || loteId.isBlank()) {
            throw new IllegalArgumentException("El id del lote es obligatorio");
        }
        if (productoId == null || productoId.isBlank()) {
            throw new IllegalArgumentException("El id del producto es obligatorio");
        }
        if (cantidadRecibida <= 0) {
            throw new IllegalArgumentException("La cantidad recibida debe ser mayor a cero");
        }
        if (unidadesPendientes < 0 || unidadesPendientes > cantidadRecibida) {
            throw new IllegalArgumentException("Las unidades pendientes no son válidas");
        }
        ubicaciones = List.copyOf(ubicaciones);
    }

    public int unidadesAlmacenadas() {
        return ubicaciones.stream().mapToInt(AsignacionCelda::unidades).sum();
    }

    public boolean quedoPendiente() {
        return unidadesPendientes > 0;
    }
}
