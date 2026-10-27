package com.redxela.api;

import com.redxela.api.dto.CrearPedidoRequest;
import com.redxela.api.dto.ItemPedidoRequest;
import com.redxela.dominio.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador REST para el alta manual y consulta de pedidos en el centro de distribución.
 * 
 * Llamada al sistema para creación de nuevos procesos (fork / exec / CreateProcess Syscall).
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final CatalogoProductos catalogoProductos;

    public PedidoController(
            PedidoService pedidoService,
            ClienteService clienteService,
            CatalogoProductos catalogoProductos
    ) {
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
        this.catalogoProductos = catalogoProductos;
    }

    @PostMapping
    public ResponseEntity<Pedido> crearPedidoManual(@Valid @RequestBody CrearPedidoRequest request) {
        Cliente cliente = clienteService.buscarPorId(request.clienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + request.clienteId()));

        List<ItemPedido> items = new ArrayList<>();
        for (ItemPedidoRequest itemReq : request.items()) {
            Producto prod = catalogoProductos.buscarPorId(itemReq.productoId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no existe: " + itemReq.productoId()));
            items.add(new ItemPedido(prod, itemReq.cantidad()));
        }

        Pedido pedido = pedidoService.registrarNuevoPedido(cliente, items);
        return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
    }

    @GetMapping
    public ResponseEntity<List<Pedido>> listarPedidos() {
        return ResponseEntity.ok(pedidoService.listarPedidos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> obtenerPedido(@PathVariable String id) {
        return pedidoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
