package com.redxela.api;

import com.redxela.api.dto.CambiarPoliticaRequest;
import com.redxela.api.dto.PoliticaResponse;
import com.redxela.planificacion.Despachador;
import com.redxela.planificacion.ElementoListaEspera;
import com.redxela.planificacion.TipoPolitica;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de políticas de planificación y cola de espera.
 * 
 * Concepto de SO: Interfaz de Llamadas al Sistema / Interfaz Administrativa del Kernel (Syscall/ProcFS).
 * Permite al administrador inspeccionar la Ready Queue e intercambiar el algoritmo de planificación
 * del sistema en caliente sin reiniciar los procesos.
 */
@RestController
@RequestMapping("/api/planificacion")
public class PlanificacionController {

    private final Despachador despachador;

    public PlanificacionController(Despachador despachador) {
        this.despachador = despachador;
    }

    @GetMapping("/politica")
    public ResponseEntity<PoliticaResponse> obtenerPoliticaActiva() {
        TipoPolitica tipo = despachador.getTipoPoliticaActiva();
        return ResponseEntity.ok(new PoliticaResponse(
                tipo,
                tipo.getNombreLegible(),
                "Política activa del despachador"
        ));
    }

    @PutMapping("/politica")
    public ResponseEntity<PoliticaResponse> cambiarPolitica(@Valid @RequestBody CambiarPoliticaRequest request) {
        despachador.cambiarPolitica(request.politica());
        return ResponseEntity.ok(new PoliticaResponse(
                request.politica(),
                request.politica().getNombreLegible(),
                "Política de planificación actualizada en caliente"
        ));
    }

    @GetMapping("/espera")
    public ResponseEntity<List<ElementoListaEspera>> obtenerListaEspera() {
        long ahora = System.currentTimeMillis();
        return ResponseEntity.ok(despachador.obtenerVistaListaEspera(ahora));
    }
}
