# Plan de trabajo: REDXela (Spring Boot + React)

Proyecto Final, Sistemas Operativos 2, USAC CUNOC. Entrega: martes 27 de octubre de 2026.
Plan creado el jueves 22 de octubre: quedan 5 días. Backend y frontend avanzan en paralelo desde la Fase 0 y los manuales no se dejan para el último día.

## 0. Reglas generales para el agente

- **Primero leer la base existente** de Spring Boot y de React y respetar sus convenciones. No reescribir lo que ya funciona.
- **Comentarios en español.** Cada clase lleva un Javadoc que indica qué concepto de Sistemas Operativos implementa. Cada bloque de sincronización lleva un comentario que explica por qué es seguro.
- **Clases pequeñas** (menos de 150 líneas) con una sola responsabilidad. Los controladores no contienen lógica de negocio, solo llaman a servicios.
- **Sin estado mutable compartido sin protección.** Solo primitivas reales de `java.util.concurrent`. Nada de `synchronized` disperso ni `Thread.sleep` como "sincronización".
- **Todo lo variable va en `application.yml`**: cantidad de recursos, capacidad del almacén, escala de tiempo, tamaño de la cola, tasa del generador.
- **Excepciones propias** (`RecursoNoDisponibleException`, `AlmacenLlenoException`, `StockInsuficienteException`, etc.) con un `@RestControllerAdvice` global. En los hilos se captura `InterruptedException` y se restaura la bandera de interrupción.
- **Sin base de datos obligatoria.** Todo en memoria con colecciones concurrentes, detrás de interfaces de repositorio.
- **Cada fase termina con una prueba** (unitaria o de integración) que demuestre su criterio de aceptación.

## 1. Arquitectura propuesta

```
com.redxela
├── config/            Propiedades (application.yml), WebSocket
├── dominio/           Cliente, Pedido, ItemPedido, Producto, TipoMercancia,
│                      NivelServicio, EstadoPedido, TipoRecurso
├── recursos/          PoolRecursos, Escaner, GestorRecursos
├── planificacion/     PoliticaPlanificacion (interfaz) + 3 políticas,
│                      ListaEspera, Despachador
├── procesamiento/     ProcesadorPedido, EtapaProceso
├── recepcion/         AreaRecepcion, MensajeLlegada
├── inventario/        ServicioInventario, Lote
├── almacen/           Almacen, Ubicacion, AsignadorUbicaciones
├── interbloqueo/      DetectorInterbloqueo, GrafoEspera, ResolutorInterbloqueo,
│                      EscenarioConflicto
├── simulacion/        GeneradorPedidos, RelojSimulacion
├── estadisticas/      ServicioEstadisticas, ComparadorPoliticas
└── api/               Controladores REST, DTOs, PublicadorEstado (WebSocket),
                       GlobalExceptionHandler
```

### Mapa de conceptos del curso (para la defensa)

| Requisito | Mecanismo | Concepto teórico |
|---|---|---|
| Estaciones, encargados, montacargas, áreas de carga y control de calidad | `Semaphore(n, fair=true)` envuelto en `PoolRecursos` | Semáforo contador |
| Sistema de escaneo (1 unidad, compartido) | `ReentrantLock` (fair) o `Semaphore(1)` | Exclusión mutua |
| Recepción ↔ Inventario | `ArrayBlockingQueue<MensajeLlegada>` acotada | Productor-consumidor y paso de mensajes |
| Estado de inventario y almacén | `ReentrantReadWriteLock` | Lectores-escritores |
| Políticas de atención | Patrón Strategy con 3 `Comparator` | Planificación (prioridad, aging, SJF) |
| Evitar inanición | Envejecimiento (aging) | Starvation |
| Prevención de deadlock | Adquisición en orden global de recursos | Se rompe la espera circular |
| Detección y resolución | Grafo de espera + selección manual de víctima | Detección y recuperación por expropiación |
| Lotes en ubicaciones no contiguas | Celdas de tamaño fijo y tabla de ubicaciones por lote | Asignación no contigua (paginación / indexada) |

## 2. Fases de trabajo

### Fase 0: Contratos y dominio (Día 1, mañana)

**Objetivo:** que backend y frontend puedan trabajar en paralelo.

- Crear enums y entidades del dominio. `Pedido` lleva: id único, cliente, nivel, lista de `ItemPedido` (producto, tipo de mercancía, cantidad), total de artículos, estado, `tCreacion`, `tInicio`, `tFin`, progreso, recursos que retiene y recurso que espera.
- Estados del pedido: `EN_ESPERA_STOCK`, `EN_ESPERA_RECURSOS`, `EN_PROCESO`, `BLOQUEADO`, `COMPLETADO`.
- Definir el **catálogo de productos** (unos 12, cada uno con su tipo de mercancía).
- Definir los **6 tipos de mercancía**, cada uno con una combinación distinta de recursos:

| Tipo | Recursos (orden natural de uso) |
|---|---|
| Frágil | Encargado → Control de calidad → Empaque |
| Pesado | Montacargas → Encargado → Área de carga |
| Voluminoso | Área de carga → Montacargas → Empaque |
| Electrónico | Control de calidad → Escáner → Empaque |
| Perecedero | Encargado → Empaque → Área de carga |
| General | Encargado → Empaque → Escáner |

  La contención realista entre dos pedidos que piden los mismos dos recursos aparece entre Pesado y Voluminoso (Montacargas + Área de carga, en orden inverso). Esa pareja es la base de la demostración de deadlock.
- Escribir `application.yml` con cantidades de recursos, capacidad del almacén (por ejemplo 20 ubicaciones de 50 unidades), `time-scale` y `capacidad-cola-recepcion`.
- Documentar el **contrato de la API** (endpoints y forma del JSON del snapshot) para que el frontend use datos simulados mientras tanto.

**Aceptación:** el proyecto compila, el contrato está escrito y el catálogo y los tipos se cargan al arrancar.

### Fase 1: Recursos y sincronización (Día 1)

**Objetivo:** gestionar los recursos compartidos con primitivas reales.

- `PoolRecursos`: envuelve un `Semaphore` justo y registra quién lo retiene (`Map<idPedido, unidades>`) y quién espera. Expone `adquirir(pedido)`, `liberar(pedido)` y `estado()`.
- La adquisición usa `tryAcquire` con tiempo de espera corto dentro de un ciclo que revisa la interrupción, para que un pedido bloqueado pueda ser expropiado en la Fase 5.
- `Escaner`: exclusión mutua compartida entre Recepción y Despacho.
- `GestorRecursos`: punto único de acceso a todos los pools, con modo configurable:
  - `PREVENCION_ACTIVA`: ordena los recursos del pedido por un rango global y los adquiere en ese orden.
  - `SIN_PREVENCION`: los adquiere en el orden natural del tipo de mercancía, reteniendo los anteriores (permite el deadlock).

**Aceptación:** una prueba con 50 hilos nunca excede la capacidad de un pool y el estado de cada recurso se consulta sin inconsistencias.

### Fase 2: Planificación y lista de espera (Día 2)

**Objetivo:** decidir qué pedido se atiende primero, con políticas intercambiables.

- Interfaz `PoliticaPlanificacion` con un `Comparator<Pedido>`.
- Tres implementaciones:
  1. `PrioridadEstricta`: por nivel y, en empate, FIFO.
  2. `PrioridadConEnvejecimiento`: la prioridad efectiva mejora según la relación entre el tiempo esperado y el tiempo objetivo del nivel (10, 30, 60 y 120 min). Evita la inanición de los niveles 4 y 5.
  3. `MenorCantidadPrimero`: por total de artículos (SJF), desempate por nivel.
- `ListaEspera`: estructura concurrente sin límite, reordenada con la política activa. Expone una vista ordenada con el motivo de la posición.
- `Despachador`: hilo que toma pedidos según la política y los lanza a procesamiento. Es el único punto de salida de la lista de espera.
- Cambio de política en caliente mediante un endpoint.

**Aceptación:** con los mismos pedidos, cada política produce un orden distinto y verificable en pruebas unitarias.

### Fase 3: Procesamiento de pedidos y generador (Día 2)

**Objetivo:** que un pedido recorra sus etapas usando recursos y tiempo simulado.

- `RelojSimulacion`: convierte minutos simulados en milisegundos reales con `time-scale`.
- `ProcesadorPedido`: un hilo por pedido (hilos virtuales con Java 21 o `ExecutorService`). Adquiere recursos con `GestorRecursos`, ejecuta cada etapa y actualiza progreso y tiempo restante.
- La duración de cada etapa es proporcional a `totalArticulos`.
- Sin recursos: `EN_ESPERA_RECURSOS`. Sin stock: `EN_ESPERA_STOCK` y se reevalúa cuando llega mercadería (se conecta en la Fase 4).
- `GeneradorPedidos`: hilo con iniciar, detener y ajustar tasa. Mezcla todos los niveles y es ilimitado.
- Alta manual de pedidos y creación de cuentas de cliente (cliente con nivel de servicio, luego pedido).

**Aceptación:** pedidos de todos los niveles fluyen de principio a fin, los recursos se liberan siempre (`finally`) y el generador corre sin tope.

### Fase 4: Recepción, Inventario y Almacén (Día 3)

**Objetivo:** comunicación entre áreas sin acoplamiento y almacenamiento no contiguo.

- `AreaRecepcion` (productor) solo publica `MensajeLlegada(producto, cantidad)` en una `ArrayBlockingQueue` acotada con `put()`. Si Inventario se atrasa, Recepción se bloquea de forma ordenada (contrapresión), sin pérdida de datos ni bloqueo indefinido. Usa el escáner compartido al descargar.
- `ServicioInventario` (consumidor): hilo propio con `take()`, actualiza existencias bajo `ReentrantReadWriteLock`. Recepción nunca toca sus estructuras.
- `Almacen`: cuadrícula de ubicaciones (pasillo, nivel, posición) con capacidad fija.
- `AsignadorUbicaciones`: asigna un lote a varias celdas libres, posiblemente no contiguas, con tabla `lote → lista de (ubicación, unidades)`. Mismo principio que la paginación. Al despachar se liberan unidades y celdas.
- Si no cabe todo el lote, se guarda lo que cabe y el resto queda pendiente y visible, reintentado al liberarse espacio. Se lanza `AlmacenLlenoException` donde corresponda.
- Al registrar un lote, el inventario emite un evento que despierta al `Despachador` para reevaluar los pedidos en `EN_ESPERA_STOCK`.
- La reserva de stock por pedido es atómica (todo o nada).

**Aceptación:**
- Una ráfaga con productor mucho más rápido que el consumidor termina sin perder mensajes.
- Un lote queda repartido en celdas no contiguas.
- Los pedidos en espera de stock avanzan solos al llegar mercadería.

### Fase 5: Interbloqueo (Día 3 a 4)

**Objetivo:** provocar, detectar, mostrar y resolver un deadlock.

- `EscenarioConflicto`: endpoint y botón "Provocar conflicto". Fuerza `SIN_PREVENCION` y crea pedidos Pesado y Voluminoso suficientes para saturar ambos recursos. Una pausa corta entre la primera y la segunda adquisición, o una `CyclicBarrier`, garantiza el ciclo de forma determinista.
- `DetectorInterbloqueo`: hilo periódico que construye el **grafo de espera** (X espera un recurso que retiene Y) y busca ciclos con DFS. Con recursos de varias unidades, confirma que todos los retenedores del recurso en disputa también están bloqueados, para evitar falsos positivos.
- Reporte: recursos involucrados, pedidos bloqueados entre sí y explicación en texto (ej.: "Pedido #12 retiene Montacargas y espera Área de carga; Pedido #15 retiene Área de carga y espera Montacargas").
- `ResolutorInterbloqueo`: recibe cuál pedido continúa. Al perdedor se le interrumpe el hilo, libera todo lo que retenía y vuelve a `ListaEspera`. El ganador continúa.
- Interruptor entre "prevención activa" y "sin prevención" para mostrar en la defensa que con orden global el mismo escenario no se bloquea.

**Aceptación:** el escenario produce deadlock cada vez en modo sin prevención, el reporte es correcto, la resolución manual libera los recursos y el perdedor se reprocesa. En modo prevención nunca ocurre.

### Fase 6: API en tiempo real y frontend (Días 2 a 4, en paralelo)

**Backend:**
- `PublicadorEstado` envía un snapshot completo por WebSocket (STOMP) o SSE cada 500 ms: recursos, pedidos en proceso, lista de espera, estadísticas, mapa del almacén, cola de mensajes e interbloqueo.
- Controladores REST: clientes, pedidos, llegada de mercadería, política activa, generador, modo de deadlock, resolución de conflicto y reinicio de la simulación.

**Frontend (React), un panel por área:**
1. Recursos: barra de uso, disponibles y quién los retiene.
2. Pedidos en proceso: progreso y tiempo restante.
3. Lista de espera con política activa visible y columna que explique el motivo del orden.
4. Estadísticas generales.
5. Formularios: nuevo cliente, nuevo pedido, llegada de mercadería.
6. Mapa del almacén (cuadrícula con colores por lote) y estado de la cola Recepción → Inventario.
7. Panel de conflicto: alerta roja, recursos y pedidos involucrados, botón "Continuar este" por pedido.
8. Controles del generador y selector de política.

Se puede empezar con datos simulados desde el contrato de la Fase 0 y conectar al backend después.

### Fase 7: Estadísticas, comparación, pruebas y documentación (Días 4 a 5)

- `ServicioEstadisticas`: pedidos completados, espera y procesamiento promedio por nivel, cumplimiento del tiempo objetivo por nivel (SLA), ocupación por recurso y throughput.
- `ComparadorPoliticas`: misma carga con semilla fija bajo cada política y tabla comparativa.
- Pruebas de extremo a extremo repasando los 6 puntos de la sección de interfaz del enunciado.
- **Manual de usuario:** capturas y flujo de cada función.
- **Manual técnico:** arquitectura, diagrama de paquetes, tabla de conceptos, justificación de cada decisión, diagramas del grafo de espera y de la cola productor-consumidor.
- **Preparación de la defensa:** hoja con preguntas probables (sección 4).

## 3. Calendario sugerido

| Día | Backend | Frontend / Docs |
|---|---|---|
| Jue 22 | Fases 0 y 1 | Maquetar vistas con datos simulados |
| Vie 23 | Fases 2 y 3 | Paneles de recursos, procesos y espera |
| Sáb 24 | Fase 4 | Formularios y mapa del almacén |
| Dom 25 | Fase 5 | Panel de conflicto, conexión al backend real |
| Lun 26 | Fase 7: estadísticas, pruebas y ajustes | Manuales de usuario y técnico |
| Mar 27 | Revisión final y entrega | Entrega |

Orden de recorte si el tiempo aprieta: primero la vista comparativa de políticas (dejar solo el selector), después el estilo visual. No se recorta nada de las Fases 1, 4 y 5: son el núcleo de lo que se califica.

## 4. Preguntas probables en la defensa

- **¿Por qué `Semaphore` para los pools y un lock para el escáner?** Recursos con varias unidades frente a uno con exclusión mutua.
- **¿Qué cuatro condiciones de Coffman se dan en el deadlock y cuál rompe tu prevención?** Exclusión mutua, retener y esperar, no expropiación y espera circular. La prevención rompe la espera circular con el orden global.
- **¿Qué pasa si Recepción es más rápida que Inventario?** La cola acotada con `put()` bloqueante da contrapresión sin pérdida de datos.
- **¿Cómo evitas la inanición del nivel 5?** Con envejecimiento (aging).
- **¿Por qué un lote puede quedar en ubicaciones no contiguas?** Asignación no contigua, que evita la fragmentación externa.
- **¿Por qué el detector confirma que todos los retenedores están bloqueados?** Para evitar falsos positivos con recursos de varias unidades.
- **¿Por qué `ReentrantReadWriteLock` en inventario?** Muchas lecturas (consultas, interfaz) y pocas escrituras (llegadas, reservas).
- **¿Por qué políticas como Strategy?** Para cambiar de política en caliente sin tocar el despachador y comparar desempeño.
