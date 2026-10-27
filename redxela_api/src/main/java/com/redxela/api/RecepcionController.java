package com.redxela.api;

import com.redxela.api.dto.EstadoInventarioResponse;
import com.redxela.api.dto.RegistrarLlegadaRequest;
import com.redxela.inventario.ServicioInventario;
import com.redxela.recepcion.AreaRecepcion;
import com.redxela.recepcion.MensajeLlegada;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST del área de Recepción de mercadería.
 * 
 * Llamada al sistema de entrada/salida (write syscall): el cliente
 * entrega un lote y el mensaje queda encolado para que el consumidor de Inventario
 * lo procese de forma asíncrona (respuesta 202 Accepted).
 */
@RestController
@RequestMapping("/api/recepcion")
public class RecepcionController {

    private final AreaRecepcion areaRecepcion;
    private final ServicioInventario servicioInventario;

    public RecepcionController(AreaRecepcion areaRecepcion, ServicioInventario servicioInventario) {
        this.areaRecepcion = areaRecepcion;
        this.servicioInventario = servicioInventario;
    }

    @PostMapping("/llegada")
    public ResponseEntity<Map<String, Object>> registrarLlegada(
            @Valid @RequestBody RegistrarLlegadaRequest request
    ) throws InterruptedException {
        MensajeLlegada mensaje = areaRecepcion.recibirLlegada(request.productoId(), request.cantidad());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "mensaje", "Cargamento recibido en cola de escaneo",
                "loteId", mensaje.loteId(),
                "productoId", mensaje.productoId(),
                "cantidad", mensaje.cantidad()
        ));
    }

    @GetMapping("/inventario")
    public ResponseEntity<EstadoInventarioResponse> consultarInventario() {
        EstadoInventarioResponse estado = new EstadoInventarioResponse(
                servicioInventario.existencias(),
                servicioInventario.lotes().size(),
                servicioInventario.unidadesPendientesTotales(),
                areaRecepcion.tamanoCola(),
                areaRecepcion.capacidadCola(),
                servicioInventario.ultimoError().orElse(null)
        );
        return ResponseEntity.ok(estado);
    }
}
