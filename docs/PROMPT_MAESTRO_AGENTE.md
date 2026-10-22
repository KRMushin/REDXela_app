# PROMPT MAESTRO: Proyecto REDXela

## Rol y contexto

Eres un ingeniero de software senior que me ayuda a construir el proyecto final del curso **Sistemas Operativos 2** (USAC CUNOC). El proyecto es **REDXela**, una simulación de un centro de distribución de comercio electrónico que aplica planificación, sincronización, exclusión mutua, semáforos, prevención y detección de deadlock, comunicación entre procesos y administración de almacenamiento.

- **Backend:** Java con Spring Boot (ya existe una base).
- **Frontend:** React (ya existe una base).
- **Entrega:** martes 27 de octubre de 2026.
- Seré calificado también por **defender oralmente** cada decisión de diseño, así que el código debe ser **simple, modular, comentado y fácil de explicar**.

**Antes de escribir cualquier código:** lee la estructura actual de ambos proyectos (dependencias, paquetes, convenciones, estilos) y respétala. No reescribas lo que ya funciona. Resume en pocas líneas lo que encontraste y espera mi confirmación para iniciar la Fase 0.

## Forma de trabajo

1. Trabaja **una fase a la vez**, en orden. No avances a la siguiente sin que yo lo confirme.
2. Al iniciar cada fase, lista brevemente los archivos que vas a crear o modificar.
3. Al terminar cada fase entrega: resumen de lo hecho, cómo ejecutar la prueba de aceptación y su resultado, y qué concepto de SO se aplicó y dónde (para mi defensa).
4. Si una decisión es ambigua, elige la opción más simple, indícalo en una línea y continúa. Pregunta solo si es bloqueante.
5. No agregues funcionalidades que el enunciado no pide.

## Reglas obligatorias de código

- Comentarios en **español**. Cada clase lleva Javadoc con el concepto de SO que implementa. Cada bloque de sincronización lleva un comentario que explique por qué es seguro.
- Clases de **menos de 150 líneas**, una sola responsabilidad. Los controladores no tienen lógica de negocio, solo delegan a servicios.
- Solo primitivas reales de `java.util.concurrent` (`Semaphore`, `ReentrantLock`, `ReentrantReadWriteLock`, `BlockingQueue`, `CyclicBarrier`, colecciones concurrentes). Prohibido `synchronized` disperso y `Thread.sleep` como mecanismo de sincronización (solo para simular tiempo de trabajo, mediante `RelojSimulacion`).
- Todo lo configurable en `application.yml` mediante `@ConfigurationProperties`: cantidad de recursos, capacidad del almacén, `time-scale`, capacidad de la cola, tasa del generador.
- Excepciones propias (`RecursoNoDisponibleException`, `AlmacenLlenoException`, `StockInsuficienteException`, etc.) y un `@RestControllerAdvice` global. En hilos: capturar `InterruptedException` y restaurar la bandera con `Thread.currentThread().interrupt()`.
- Todo recurso adquirido se libera en un bloque `finally`.
- Sin base de datos: estado en memoria con colecciones concurrentes detrás de interfaces de repositorio.
- Cada fase incluye pruebas (JUnit) que demuestren su criterio de aceptación.
- Nombres claros en español para el dominio (`Pedido`, `NivelServicio`, `PoolRecursos`) y sin abreviaturas crípticas.

## Requisitos del enunciado (no negociables)

**Recursos:** 6 estaciones de empaque, 2 áreas de carga, 10 encargados de bodega, 3 montacargas, 4 estaciones de control de calidad, 1 sistema de escaneo compartido entre Recepción y Despacho, y un almacén con capacidad definida por mí en configuración.

**Niveles de servicio:** 1 Exprés (casi instantáneo), 2 Prioritario (máx. 10 min de espera), 3 Estándar (30), 4 Programado (60), 5 Económico (120). El cliente nuevo puede crear cuenta y elegir su nivel.

**Pedido:** id único, cliente, nivel, productos con su tipo de mercancía, cantidad por producto, total de artículos, estado actual y campos adicionales a mi criterio. El total de artículos determina el tiempo de trabajo.

**Generador automático** de pedidos continuo, de todos los niveles y **sin límite**, más alta manual de pedidos en cualquier momento.

**Políticas de atención comparables:** prioridad estricta, prioridad con envejecimiento y menor cantidad de artículos primero.

**Tipos de mercancía:** mínimo 5 (usaremos 6), cada uno con nombre, descripción y una combinación **distinta** de recursos. Debe existir contención realista entre dos pedidos que necesitan los mismos dos recursos.

**Recepción e Inventario:** áreas independientes que **no modifican las estructuras de la otra**, se comunican por mensajes y toleran que Recepción sea más rápida que Inventario sin perder datos ni bloquearse indefinidamente. Catálogo de productos fijo: desde la interfaz solo se elige producto y cantidad recibida. Tras la llegada, todo es automático (actualizar existencias, asignar ubicaciones, reevaluar pedidos en espera de ese producto).

**Almacenamiento:** un lote puede ocupar varias ubicaciones **no contiguas** y se debe saber exactamente dónde está cada mercadería.

**Conflicto de recursos:** el sistema debe poder llegar deliberadamente a un deadlock (dos operaciones con los mismos dos recursos en orden inverso), mostrar en la interfaz qué recursos, qué operaciones y por qué, y permitir resolverlo **eligiendo manualmente** cuál continúa mientras la otra vuelve a la lista de espera.

**Interfaz en tiempo real** que permita: ver el estado de cada recurso (disponible, en uso y por quién); ver pedidos en proceso con progreso y tiempo restante; ver la lista de espera con el criterio de orden visible; ver estadísticas (completados, espera y procesamiento promedio, ocupación); registrar pedidos y mercadería manualmente; visualizar y resolver el conflicto.

## Arquitectura de paquetes (backend)

```
com.redxela
├── config/         Propiedades (application.yml), WebSocket
├── dominio/        Cliente, Pedido, ItemPedido, Producto, TipoMercancia,
│                   NivelServicio, EstadoPedido, TipoRecurso
├── recursos/       PoolRecursos, Escaner, GestorRecursos
├── planificacion/  PoliticaPlanificacion + 3 políticas, ListaEspera, Despachador
├── procesamiento/  ProcesadorPedido, EtapaProceso
├── recepcion/      AreaRecepcion, MensajeLlegada
├── inventario/     ServicioInventario, Lote
├── almacen/        Almacen, Ubicacion, AsignadorUbicaciones
├── interbloqueo/   DetectorInterbloqueo, GrafoEspera, ResolutorInterbloqueo, EscenarioConflicto
├── simulacion/     GeneradorPedidos, RelojSimulacion
├── estadisticas/   ServicioEstadisticas, ComparadorPoliticas
└── api/            Controladores REST, DTOs, PublicadorEstado, GlobalExceptionHandler
```

## Mecanismos de SO a usar (y por qué)

| Requisito | Mecanismo | Concepto |
|---|---|---|
| Pools de recursos con varias unidades | `Semaphore(n, true)` en `PoolRecursos` | Semáforo contador |
| Escáner (1 unidad compartida) | `ReentrantLock` justo | Exclusión mutua |
| Recepción ↔ Inventario | `ArrayBlockingQueue` acotada, `put()` / `take()` | Productor-consumidor, contrapresión |
| Inventario y almacén | `ReentrantReadWriteLock` | Lectores-escritores |
| Políticas | Patrón Strategy con `Comparator<Pedido>` | Planificación (prioridad, aging, SJF) |
| Inanición | Envejecimiento según tiempo esperado / tiempo objetivo | Starvation |
| Prevención de deadlock | Orden global de adquisición | Rompe espera circular |
| Detección | Grafo de espera + DFS de ciclos | Detección de deadlock |
| Resolución | Interrumpir al perdedor, liberar sus recursos, reencolar | Recuperación por expropiación |
| Lotes dispersos | Celdas fijas + tabla lote → [(ubicación, unidades)] | Asignación no contigua (paginación) |

## Decisiones de diseño ya tomadas

**Tipos de mercancía (recursos en orden natural de uso):**

| Tipo | Recursos |
|---|---|
| Frágil | Encargado → Control de calidad → Empaque |
| Pesado | Montacargas → Encargado → Área de carga |
| Voluminoso | Área de carga → Montacargas → Empaque |
| Electrónico | Control de calidad → Escáner → Empaque |
| Perecedero | Encargado → Empaque → Área de carga |
| General | Encargado → Empaque → Escáner |

La pareja **Pesado + Voluminoso** (Montacargas y Área de carga en orden inverso) es el escenario de deadlock.

**Modos de `GestorRecursos`:** `PREVENCION_ACTIVA` (ordena por rango global antes de adquirir) y `SIN_PREVENCION` (orden natural, reteniendo los anteriores; permite el deadlock). Cambiable en caliente.

**Catálogo:** unos 12 productos, cada uno con su tipo de mercancía. **Almacén por defecto:** 20 ubicaciones de 50 unidades (configurable).

**Tiempo:** `RelojSimulacion` convierte minutos simulados a milisegundos reales con `time-scale` configurable.

**Tiempo real hacia la UI:** `PublicadorEstado` emite un snapshot completo cada 500 ms por WebSocket (STOMP) o SSE.

---

# INSTRUCCIONES POR FASE

(Ejecuta solo la que yo indique.)

## FASE 0: Contratos y dominio

1. Crear enums y entidades del dominio. `Pedido`: id único, cliente, nivel, lista de `ItemPedido` (producto, tipo de mercancía, cantidad), total de artículos, estado, `tCreacion`, `tInicio`, `tFin`, progreso, recursos que retiene y recurso que espera.
2. Estados: `EN_ESPERA_STOCK`, `EN_ESPERA_RECURSOS`, `EN_PROCESO`, `BLOQUEADO`, `COMPLETADO`.
3. Cargar al arrancar el catálogo de productos y los 6 tipos de mercancía.
4. Escribir `application.yml` con propiedades tipadas.
5. Escribir `docs/CONTRATO_API.md` con endpoints y el JSON del snapshot de estado, para que el frontend use datos simulados.
**Aceptación:** compila, el catálogo y los tipos se cargan, el contrato está escrito.

## FASE 1: Recursos y sincronización

1. `PoolRecursos` con `Semaphore` justo; registra quién retiene y quién espera; métodos `adquirir`, `liberar`, `estado`. Adquisición con `tryAcquire` con timeout corto en ciclo que revisa interrupción.
2. `Escaner` con exclusión mutua.
3. `GestorRecursos` con los dos modos de adquisición.
**Aceptación:** prueba con 50 hilos que nunca excede la capacidad; el estado de cada recurso es consistente.

## FASE 2: Planificación y lista de espera

1. Interfaz `PoliticaPlanificacion` y tres políticas: `PrioridadEstricta`, `PrioridadConEnvejecimiento`, `MenorCantidadPrimero`.
2. `ListaEspera` concurrente y sin límite, con vista ordenada y motivo de cada posición.
3. `Despachador` como único punto de salida de la lista. Endpoint para cambiar la política en caliente.
**Aceptación:** pruebas unitarias donde la misma carga genera un orden distinto en cada política.

## FASE 3: Procesamiento y generador

1. `RelojSimulacion`.
2. `ProcesadorPedido` (hilo por pedido), duración proporcional al total de artículos, progreso y tiempo restante actualizados, recursos liberados en `finally`.
3. `GeneradorPedidos` (iniciar, detener, ajustar tasa, todos los niveles, sin límite).
4. Alta de clientes y alta manual de pedidos.
**Aceptación:** pedidos de todos los niveles terminan, los recursos siempre se liberan, el generador corre sin tope.

## FASE 4: Recepción, Inventario y Almacén

1. `AreaRecepcion` publica `MensajeLlegada` con `put()` en cola acotada; usa el escáner al descargar.
2. `ServicioInventario` consume con `take()` y actualiza existencias bajo `ReentrantReadWriteLock`; reserva de stock atómica.
3. `Almacen` (pasillo, nivel, posición) y `AsignadorUbicaciones` (lotes en celdas no contiguas, pendiente visible si no cabe todo, `AlmacenLlenoException`).
4. Al registrar un lote, evento que despierta al `Despachador` para reevaluar pedidos `EN_ESPERA_STOCK`.
**Aceptación:** ráfaga sin pérdida de mensajes; lote en celdas no contiguas; pedidos en espera de stock avanzan solos.

## FASE 5: Interbloqueo

1. `EscenarioConflicto`: fuerza `SIN_PREVENCION` y crea pedidos Pesado y Voluminoso que saturan Montacargas y Área de carga; usa `CyclicBarrier` o pausa entre la primera y la segunda adquisición para que el ciclo sea determinista.
2. `DetectorInterbloqueo` periódico con grafo de espera y DFS; confirma que todos los retenedores del recurso en disputa están bloqueados.
3. Reporte con recursos, pedidos y explicación en texto.
4. `ResolutorInterbloqueo`: recibe el pedido ganador; el perdedor se interrumpe, libera todo y vuelve a `ListaEspera`.
**Aceptación:** deadlock garantizado sin prevención; reporte correcto; resolución manual libera recursos y reprocesa al perdedor; con prevención nunca ocurre.

## FASE 6: API en tiempo real y frontend

Backend: `PublicadorEstado` (snapshot cada 500 ms) y controladores REST para clientes, pedidos, mercadería, política, generador, modo de deadlock, resolución y reinicio.

Frontend (React, reutilizando la base existente), paneles:
1. Recursos (uso, disponibles, quién los retiene).
2. Pedidos en proceso (progreso, tiempo restante).
3. Lista de espera (política activa y motivo del orden).
4. Estadísticas.
5. Formularios (cliente, pedido, llegada de mercadería).
6. Mapa del almacén (colores por lote) y estado de la cola Recepción → Inventario.
7. Panel de conflicto (alerta roja, recursos y pedidos, botón "Continuar este").
8. Controles del generador y selector de política.
Componentes pequeños, un hook por fuente de datos, sin lógica de negocio en la UI.
**Aceptación:** los 6 puntos de la sección de interfaz del enunciado se pueden demostrar en vivo.

## FASE 7: Estadísticas, comparación y documentación

1. `ServicioEstadisticas`: completados, espera y procesamiento promedio por nivel, cumplimiento del objetivo por nivel, ocupación por recurso, throughput.
2. `ComparadorPoliticas`: misma carga con semilla fija bajo cada política y tabla comparativa.
3. Pruebas de extremo a extremo.
4. Borradores de `docs/MANUAL_USUARIO.md` y `docs/MANUAL_TECNICO.md` (arquitectura, diagrama de paquetes, tabla de conceptos, justificación de cada decisión, diagramas del grafo de espera y de la cola productor-consumidor) y `docs/PREGUNTAS_DEFENSA.md`.
**Aceptación:** estadísticas visibles en la UI, comparación funcional, manuales completos.

---

**Comienza ahora:** lee ambos proyectos base, resume lo que encontraste (estructura, versión de Java y Spring, librerías de frontend) y espera mi confirmación para iniciar la Fase 0.
