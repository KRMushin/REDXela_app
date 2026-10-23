package com.redxela.api;

import com.redxela.dominio.CatalogoProductos;
import com.redxela.dominio.Producto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * Controlador REST del catálogo fijo de productos.
 *
 * Concepto de SO: Tabla de descriptores estáticos (Static Inode/File Table).
 * Solo lectura: los productos existen desde el arranque y no cambian en caliente.
 */
@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {

    private final CatalogoProductos catalogo;

    public CatalogoController(CatalogoProductos catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping
    public ResponseEntity<List<Producto>> listar() {
        List<Producto> productos = catalogo.listarTodos().stream()
                .sorted(Comparator.comparing(Producto::id))
                .toList();
        return ResponseEntity.ok(productos);
    }
}
