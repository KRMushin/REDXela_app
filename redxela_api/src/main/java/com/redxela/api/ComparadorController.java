package com.redxela.api;

import com.redxela.api.dto.ComparacionPoliticas;
import com.redxela.api.dto.ComparacionRequest;
import com.redxela.estadisticas.ComparadorPoliticas;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Lanza y consulta la comparación de políticas de planificación.
 * Interfaz administrativa de benchmark (scheduler benchmark syscall).
 * La ejecución ocurre sobre una réplica aislada y no modifica la simulación en vivo.
 */
@RestController
@RequestMapping("/api/comparador/politicas")
public class ComparadorController {

    private final ComparadorPoliticas comparador;

    public ComparadorController(ComparadorPoliticas comparador) {
        this.comparador = comparador;
    }

    @PostMapping
    public ResponseEntity<ComparacionPoliticas> ejecutar(
            @Valid @RequestBody(required = false) ComparacionRequest request
    ) {
        return ResponseEntity.ok(comparador.comparar(request));
    }

    @GetMapping
    public ResponseEntity<ComparacionPoliticas> ultimoResultado() {
        return ResponseEntity.of(comparador.ultimoResultado());
    }
}
