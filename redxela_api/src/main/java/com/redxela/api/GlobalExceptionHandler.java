package com.redxela.api;

import com.redxela.almacen.AlmacenLlenoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Manejador global de excepciones de la API REST.
 * Manejo centralizado de fallas del sistema (trap/exception handler):
 * traduce errores de asignación de recursos y de validación a códigos de estado HTTP
 * sin filtrar detalles internos al cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AlmacenLlenoException.class)
    public ResponseEntity<Map<String, Object>> manejarAlmacenLleno(AlmacenLlenoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "ALMACEN_LLENO",
                "mensaje", e.getMessage(),
                "unidadesPendientes", e.getUnidadesPendientes()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> manejarArgumentoInvalido(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "SOLICITUD_INVALIDA",
                "mensaje", e.getMessage() == null ? "Solicitud inválida" : e.getMessage()
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> manejarEstadoInvalido(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "ESTADO_INVALIDO",
                "mensaje", e.getMessage() == null ? "El sistema no está en el estado requerido" : e.getMessage()
        ));
    }

    @ExceptionHandler(InterruptedException.class)
    public ResponseEntity<Map<String, Object>> manejarInterrupcion(InterruptedException e) {
        Thread.currentThread().interrupt(); // restaura la bandera de interrupción
        log.warn("Operación interrumpida durante la recepción de mercadería");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "OPERACION_INTERRUMPIDA",
                "mensaje", "La operación fue interrumpida; reintente más tarde"
        ));
    }
}
