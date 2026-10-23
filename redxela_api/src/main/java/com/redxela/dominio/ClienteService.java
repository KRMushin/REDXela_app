package com.redxela.dominio;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio para la administración y registro de clientes y sus niveles de prioridad.
 * 
 * Concepto de SO: Gestión de Cuentas de Usuario y Clases de Prioridad (User Accounts & Niceness).
 * Registra los usuarios del sistema asignando sus correspondientes niveles de prioridad de servicio.
 */
@Service
public class ClienteService {

    private final Map<String, Cliente> clientes = new ConcurrentHashMap<>();
    private final AtomicInteger secuencialId = new AtomicInteger(1);

    public ClienteService() {
        // Clientes iniciales para cada uno de los 5 niveles de servicio
        registrarCliente(new Cliente("CLI-01", "Distribuidora Exprés Quetzal", NivelServicio.EXPRES));
        registrarCliente(new Cliente("CLI-02", "Comercial San Marcos", NivelServicio.PRIORITARIO));
        registrarCliente(new Cliente("CLI-03", "Importadora Los Altos", NivelServicio.ESTANDAR));
        registrarCliente(new Cliente("CLI-04", "Ferretería Central Xela", NivelServicio.PROGRAMADO));
        registrarCliente(new Cliente("CLI-05", "Bodega El Carmen", NivelServicio.ECONOMICO));
    }

    public Cliente crearCliente(String nombre, NivelServicio nivel) {
        String id = String.format("CLI-%02d", secuencialId.incrementAndGet());
        Cliente cliente = new Cliente(id, nombre, nivel);
        registrarCliente(cliente);
        return cliente;
    }

    public void registrarCliente(Cliente cliente) {
        clientes.put(cliente.id(), cliente);
    }

    public Optional<Cliente> buscarPorId(String id) {
        return Optional.ofNullable(clientes.get(id));
    }

    public List<Cliente> listarClientes() {
        return new ArrayList<>(clientes.values());
    }

    public Optional<Cliente> obtenerClienteAleatorio() {
        List<Cliente> lista = listarClientes();
        if (lista.isEmpty()) return Optional.empty();
        int idx = (int) (Math.random() * lista.size());
        return Optional.of(lista.get(idx));
    }
}
