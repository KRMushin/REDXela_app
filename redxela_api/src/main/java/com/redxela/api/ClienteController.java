package com.redxela.api;

import com.redxela.api.dto.CrearClienteRequest;
import com.redxela.dominio.Cliente;
import com.redxela.dominio.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para el alta y consulta de clientes y sus niveles de prioridad.
 * Llamada al sistema de creación de usuarios/procesos padre (User Management Syscall).
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    public ResponseEntity<Cliente> crearCliente(@Valid @RequestBody CrearClienteRequest request) {
        Cliente cliente = clienteService.crearCliente(request.nombre(), request.nivelServicio());
        return ResponseEntity.status(HttpStatus.CREATED).body(cliente);
    }

    @GetMapping
    public ResponseEntity<List<Cliente>> listarClientes() {
        return ResponseEntity.ok(clienteService.listarClientes());
    }
}
