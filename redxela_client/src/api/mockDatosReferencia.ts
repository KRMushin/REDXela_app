import type { Cliente, Producto } from '../types/api';

/** Catálogo y clientes de demostración para el modo mock del frontend. */

export const productosMock: Producto[] = [
  { id: 'PROD-01', nombre: 'Cristalería Fina', tipoMercancia: 'FRAGIL', descripcion: 'Juego de copas y cristales delicados' },
  { id: 'PROD-02', nombre: 'Cerámica Artesanal', tipoMercancia: 'FRAGIL', descripcion: 'Piezas moldeadas a mano' },
  { id: 'PROD-03', nombre: 'Prensa Hidráulica', tipoMercancia: 'PESADO', descripcion: 'Maquinaria pesada para taller' },
  { id: 'PROD-04', nombre: 'Bloques de Acero', tipoMercancia: 'PESADO', descripcion: 'Vigas y lingotes macizos' },
  { id: 'PROD-05', nombre: 'Sofá Modular', tipoMercancia: 'VOLUMINOSO', descripcion: 'Mueble de sala amplio' },
  { id: 'PROD-06', nombre: 'Armario Metálico', tipoMercancia: 'VOLUMINOSO', descripcion: 'Estructura de gran volumen' },
  { id: 'PROD-07', nombre: 'Laptop Profesional', tipoMercancia: 'ELECTRONICO', descripcion: 'Equipo de cómputo de alta tecnología' },
  { id: 'PROD-08', nombre: 'Servidor Blade', tipoMercancia: 'ELECTRONICO', descripcion: 'Módulo para centros de datos' },
  { id: 'PROD-09', nombre: 'Lote de Lácteos', tipoMercancia: 'PERECEDERO', descripcion: 'Productos refrigerados' },
  { id: 'PROD-10', nombre: 'Frutas Frescas', tipoMercancia: 'PERECEDERO', descripcion: 'Alimentos de despacho ágil' },
  { id: 'PROD-11', nombre: 'Cajas de Papel Bond', tipoMercancia: 'GENERAL', descripcion: 'Suministros de oficina' },
  { id: 'PROD-12', nombre: 'Herramientas Manuales', tipoMercancia: 'GENERAL', descripcion: 'Llaves y destornilladores' },
];

export const clientesMock: Cliente[] = [
  { id: 'CLI-01', nombre: 'Distribuidora Exprés Quetzal', nivelServicio: 'EXPRES' },
  { id: 'CLI-02', nombre: 'Comercial San Marcos', nivelServicio: 'PRIORITARIO' },
  { id: 'CLI-03', nombre: 'Importadora Los Altos', nivelServicio: 'ESTANDAR' },
  { id: 'CLI-04', nombre: 'Ferretería Central Xela', nivelServicio: 'PROGRAMADO' },
  { id: 'CLI-05', nombre: 'Bodega El Carmen', nivelServicio: 'ECONOMICO' },
];
