package com.redxela.dominio;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Catálogo centralizado y en memoria de productos disponibles en REDXela.
 * 
 * Concepto de SO: Tabla de descriptores de archivos/datos estáticos cargados
 * en la memoria del kernel al inicializar el sistema (VFS / Static Inodes Table).
 */
@Component
public class CatalogoProductos {

    private final Map<String, Producto> productos = new ConcurrentHashMap<>();

    public CatalogoProductos() {
        cargarCatalogoPorDefecto();
    }

    private void cargarCatalogoPorDefecto() {
        // 2 productos por cada uno de los 6 tipos de mercancía (12 productos en total)
        registrar(new Producto("PROD-01", "Cristalería Fina", TipoMercancia.FRAGIL, "Juego de copas y cristales delicados"));
        registrar(new Producto("PROD-02", "Cerámica Artesanal", TipoMercancia.FRAGIL, "Piezas moldeadas a mano de alta fragilidad"));

        registrar(new Producto("PROD-03", "Prensa Hidráulica", TipoMercancia.PESADO, "Maquinaria pesada para taller mecánico"));
        registrar(new Producto("PROD-04", "Bloques de Acero", TipoMercancia.PESADO, "Vigas y lingotes macizos de construcción"));

        registrar(new Producto("PROD-05", "Sofá Modular", TipoMercancia.VOLUMINOSO, "Mueble de sala en varias secciones amplias"));
        registrar(new Producto("PROD-06", "Armario Metálico", TipoMercancia.VOLUMINOSO, "Estructura de almacenamiento de gran volumen"));

        registrar(new Producto("PROD-07", "Laptop Profesional", TipoMercancia.ELECTRONICO, "Equipo de cómputo de alta tecnología"));
        registrar(new Producto("PROD-08", "Servidor Blade", TipoMercancia.ELECTRONICO, "Módulo computacional para centros de datos"));

        registrar(new Producto("PROD-09", "Lote de Lácteos", TipoMercancia.PERECEDERO, "Productos refrigerados con fecha límite corta"));
        registrar(new Producto("PROD-10", "Frutas Frescas", TipoMercancia.PERECEDERO, "Alimentos perecederos que demandan despacho ágil"));

        registrar(new Producto("PROD-11", "Cajas de Papel Bond", TipoMercancia.GENERAL, "Paquetes estándar de suministros de oficina"));
        registrar(new Producto("PROD-12", "Herramientas Manuales", TipoMercancia.GENERAL, "Cajas con llaves y destornilladores generales"));
    }

    public void registrar(Producto producto) {
        productos.put(producto.id(), producto);
    }

    public Optional<Producto> buscarPorId(String id) {
        return Optional.ofNullable(productos.get(id));
    }

    public Collection<Producto> listarTodos() {
        return Collections.unmodifiableCollection(productos.values());
    }

    public int totalProductos() {
        return productos.size();
    }
}
