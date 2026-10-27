package com.redxela.api;

import com.redxela.api.dto.EstadisticasSistema;
import com.redxela.estadisticas.ServicioEstadisticas;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone las métricas detalladas del centro de distribución.
 * Lectura de contabilidad del sistema (procfs de métricas) sin
 * alterar el snapshot periódico que consume el tablero en vivo.
 */
@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasController {

    private final ServicioEstadisticas estadisticas;

    public EstadisticasController(ServicioEstadisticas estadisticas) {
        this.estadisticas = estadisticas;
    }

    @GetMapping
    public ResponseEntity<EstadisticasSistema> consultar() {
        return ResponseEntity.ok(estadisticas.calcularDetalladas());
    }
}
