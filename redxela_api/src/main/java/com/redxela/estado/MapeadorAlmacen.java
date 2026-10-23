package com.redxela.estado;

import com.redxela.almacen.Almacen;
import com.redxela.almacen.Ubicacion;
import com.redxela.api.dto.SnapshotEstado;
import com.redxela.dominio.CatalogoProductos;
import com.redxela.recepcion.AreaRecepcion;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Traduce el espacio físico de almacenamiento y el buzón de recepción al snapshot.
 *
 * Concepto de SO: Vista de memoria física y de la cola de E/S (Physical Memory Map).
 * Expone celdas, lote, producto y ocupación sin revelar referencias mutables internas.
 */
@Component
public class MapeadorAlmacen {

    private final Almacen almacen;
    private final CatalogoProductos catalogo;
    private final AreaRecepcion recepcion;

    public MapeadorAlmacen(
            Almacen almacen,
            CatalogoProductos catalogo,
            AreaRecepcion recepcion
    ) {
        this.almacen = almacen;
        this.catalogo = catalogo;
        this.recepcion = recepcion;
    }

    SnapshotEstado.AlmacenEstado almacen() {
        List<SnapshotEstado.UbicacionEstado> ubicaciones = almacen.obtenerUbicaciones().stream()
                .map(this::ubicacion)
                .toList();
        return new SnapshotEstado.AlmacenEstado(
                almacen.getCapacidadTotal(),
                almacen.getUnidadesOcupadas(),
                ubicaciones,
                new SnapshotEstado.ColaRecepcion(recepcion.tamanoCola(), recepcion.capacidadCola())
        );
    }

    private SnapshotEstado.UbicacionEstado ubicacion(Ubicacion ubicacion) {
        String productoId = ubicacion.getProductoId();
        String productoNombre = productoId == null ? null
                : catalogo.buscarPorId(productoId).map(p -> p.nombre()).orElse(null);
        return new SnapshotEstado.UbicacionEstado(
                ubicacion.getId(),
                ubicacion.getPasillo(),
                ubicacion.getNivel(),
                ubicacion.getPosicion(),
                ubicacion.getLoteId(),
                productoId,
                productoNombre,
                ubicacion.getUnidadesOcupadas(),
                ubicacion.getCapacidad()
        );
    }
}
