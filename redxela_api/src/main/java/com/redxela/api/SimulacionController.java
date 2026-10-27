package com.redxela.api;

import com.redxela.api.dto.AjustarTasaRequest;
import com.redxela.simulacion.GeneradorPedidos;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para supervisar y operar el generador de carga del sistema.
 * 
 * Interfaz de Inyección de Carga y Calibración del Sistema (Benchmark / Stress Test Controller).
 */
@RestController
@RequestMapping("/api/simulacion/generador")
public class SimulacionController {

    private final GeneradorPedidos generador;

    public SimulacionController(GeneradorPedidos generador) {
        this.generador = generador;
    }

    @PostMapping("/iniciar")
    public ResponseEntity<Map<String, Object>> iniciar() {
        generador.iniciar();
        return ResponseEntity.ok(Map.of("mensaje", "Generador de pedidos iniciado", "activo", true));
    }

    @PostMapping("/detener")
    public ResponseEntity<Map<String, Object>> detener() {
        generador.detener();
        return ResponseEntity.ok(Map.of("mensaje", "Generador de pedidos detenido", "activo", false));
    }

    @PutMapping("/tasa")
    public ResponseEntity<Map<String, Object>> ajustarTasa(@Valid @RequestBody AjustarTasaRequest request) {
        generador.ajustarTasa(request.pedidosPorMinuto());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Tasa de generación actualizada",
                "tasaPorMinuto", request.pedidosPorMinuto()
        ));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenerEstado() {
        return ResponseEntity.ok(Map.of(
                "activo", generador.isActivo(),
                "tasaPorMinuto", generador.getTasaPorMinuto()
        ));
    }
}
