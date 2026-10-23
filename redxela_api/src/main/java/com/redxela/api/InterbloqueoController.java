package com.redxela.api;

import com.redxela.api.dto.CambiarModoRequest;
import com.redxela.api.dto.ResolverInterbloqueoRequest;
import com.redxela.interbloqueo.DetectorInterbloqueo;
import com.redxela.interbloqueo.EscenarioConflicto;
import com.redxela.interbloqueo.ReporteInterbloqueo;
import com.redxela.interbloqueo.ResolutorInterbloqueo;
import com.redxela.recursos.GestorRecursos;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para observar, provocar y resolver interbloqueos.
 * 
 * Concepto de SO: Interfaz administrativa de detección y recuperación de interbloqueos
 * (deadlock management). Permite al operador cambiar el modo de prevención, forzar el
 * escenario de conflicto y elegir manualmente qué proceso conserva los recursos.
 */
@RestController
@RequestMapping("/api/interbloqueo")
public class InterbloqueoController {

    private final GestorRecursos gestorRecursos;
    private final DetectorInterbloqueo detector;
    private final EscenarioConflicto escenario;
    private final ResolutorInterbloqueo resolutor;

    public InterbloqueoController(
            GestorRecursos gestorRecursos,
            DetectorInterbloqueo detector,
            EscenarioConflicto escenario,
            ResolutorInterbloqueo resolutor
    ) {
        this.gestorRecursos = gestorRecursos;
        this.detector = detector;
        this.escenario = escenario;
        this.resolutor = resolutor;
    }

    @GetMapping
    public ResponseEntity<ReporteInterbloqueo> estado() {
        return ResponseEntity.ok(detector.ultimoReporte());
    }

    @PutMapping("/modo")
    public ResponseEntity<Map<String, Object>> cambiarModo(@Valid @RequestBody CambiarModoRequest request) {
        gestorRecursos.cambiarModo(request.modo());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Modo de prevención actualizado",
                "modo", gestorRecursos.getModo()
        ));
    }

    @PostMapping("/forzar")
    public ResponseEntity<ReporteInterbloqueo> forzar() {
        return ResponseEntity.ok(escenario.forzar());
    }

    @PostMapping("/resolver")
    public ResponseEntity<Map<String, Object>> resolver(
            @Valid @RequestBody ResolverInterbloqueoRequest request
    ) {
        ReporteInterbloqueo reporte = resolutor.resolver(request.pedidoGanadorId());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Interbloqueo resuelto: el ganador conserva los recursos y el perdedor vuelve a la cola",
                "pedidoGanador", request.pedidoGanadorId(),
                "reporte", reporte
        ));
    }
}
