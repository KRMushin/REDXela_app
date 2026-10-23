package com.redxela.api;

import com.redxela.simulacion.ServicioReinicio;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador REST para reiniciar la simulación completa.
 *
 * Concepto de SO: Llamada administrativa de reinicio (reboot syscall). Devuelve
 * el sistema a su estado inicial sin terminar el proceso de la aplicación.
 */
@RestController
@RequestMapping("/api/simulacion")
public class ReinicioController {

    private final ServicioReinicio servicioReinicio;

    public ReinicioController(ServicioReinicio servicioReinicio) {
        this.servicioReinicio = servicioReinicio;
    }

    @PostMapping("/reiniciar")
    public ResponseEntity<Map<String, Object>> reiniciar() {
        return ResponseEntity.ok(servicioReinicio.reiniciar());
    }
}
