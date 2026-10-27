# Manual de usuario — REDXela

REDXela permite simular la operación de un centro de distribución: registrar clientes y pedidos, recibir mercadería y consultar el avance de la atención.

## 1. Acceso al sistema

Con la aplicación en ejecución, abra <http://localhost:5173> en el navegador. Si necesita iniciar el sistema, ejecute desde la carpeta del proyecto:

```bash
docker compose up --build
```

Espere a que el tablero indique **En vivo** o **Backend conectado**. Si aparece como desconectado, compruebe que la aplicación esté en ejecución y vuelva a cargar la página. Si el problema continúa, solicite ayuda a la persona encargada de instalar el sistema.

Use las pestañas superiores o la barra lateral para abrir cada panel. En la parte superior se muestran el tiempo simulado, la tasa del generador, la política y el modo de prevención. El tiempo simulado es el tiempo usado por la simulación y puede avanzar a un ritmo distinto del reloj real.

Al iniciar, hay clientes, productos y existencias disponibles. El generador de pedidos está detenido.

## 2. Registrar clientes

1. Abra **Formularios** y busque **Nuevo cliente**.
2. Escriba el **Nombre**.
3. Seleccione el **Nivel de servicio**: Exprés, Prioritario, Estándar, Programado o Económico.
4. Pulse **Registrar cliente**. Al finalizar aparece un mensaje de confirmación y el cliente queda disponible para nuevos pedidos.

El nivel de servicio indica la prioridad del cliente y el objetivo de espera de sus pedidos. El nombre es obligatorio y admite hasta 80 caracteres.

## 3. Registrar pedidos

1. En **Formularios**, busque **Nuevo pedido**.
2. Seleccione un **Cliente** y revise la prioridad que aparece.
3. En **Productos**, seleccione un producto e indique una cantidad entera mayor que cero.
4. Para incluir otro producto, pulse **+ Agregar producto**. Use **×** para quitar una línea.
5. Pulse **Registrar pedido** y espere el mensaje de confirmación.

Si falta alguna de las existencias solicitadas, el pedido queda en **Espera stock**. Puede consultarlo en **Lista de espera** y registrar una llegada de mercadería para abastecerlo. Si hay existencias, el pedido espera los recursos necesarios o comienza su procesamiento.

## 4. Recibir mercadería y consultar el almacén

### 4.1. Registrar una llegada

1. En **Formularios**, busque **Llegada de mercadería**.
2. Seleccione el **Producto** e indique la **Cantidad de unidades**.
3. Pulse **Registrar llegada**. La interfaz mostrará el identificador del lote generado.
4. Abra **Almacén** para consultar la ocupación y las ubicaciones. Si había pedidos esperando ese producto, revise también **Lista de espera**.

La confirmación de la llegada indica que se recibió la solicitud; la mercadería puede tardar en aparecer en el almacén. Si no hay espacio suficiente, el lote puede quedar pendiente y el panel no muestra el detalle del problema.

### 4.2. Consultar el almacén

En **Almacén** puede ver la **Ocupación general**, la **Cola Recepción → Inventario** y las ubicaciones. Cada ubicación muestra su capacidad y, cuando está ocupada, el producto y el lote. Las ubicaciones de un mismo lote comparten color; la leyenda ayuda a identificarlas.

## 5. Configurar la atención de pedidos

### 5.1. Generar pedidos automáticamente

1. Abra **Controles**.
2. En **Generador de pedidos**, pulse **Iniciar**.
3. Para cambiar la cantidad generada, escriba un número entero mayor que cero en **Pedidos por minuto** y pulse **Actualizar tasa**.
4. Para detener la generación, pulse **Detener**.

La tasa también puede configurarse mientras el generador está detenido. Los pedidos manuales pueden registrarse sin iniciarlo.

### 5.2. Cambiar la política

La política determina el orden de atención de los pedidos que tienen existencias y esperan recursos:

- **Prioridad Estricta:** atiende primero los niveles de servicio más altos.
- **Prioridad con Envejecimiento:** toma en cuenta el nivel y aumenta la prioridad de los pedidos que esperan más tiempo.
- **Menor Cantidad Primero:** atiende primero los pedidos con menos artículos.

Para cambiarla:

1. En **Controles**, busque **Política de planificación**.
2. Seleccione una opción en **Algoritmo del despachador**.
3. Pulse **Aplicar política**.
4. Abra **Lista de espera** para consultar el orden actualizado.

El cambio no reinicia la simulación. Si selecciona la política que ya está activa, el botón permanece deshabilitado.

## 6. Dar seguimiento a los pedidos

### 6.1. Lista de espera

Abra **Lista de espera** para consultar el identificador, cliente, nivel, estado, cantidad de artículos, tiempo de espera, prioridad y **Motivo de la posición**.

- **Espera stock:** faltan existencias para el pedido.
- **Espera recursos:** el pedido espera los recursos necesarios para comenzar.

Los pedidos listos se muestran primero según la política activa; después aparecen los que esperan existencias o están adquiriendo recursos. La posición de un pedido sin stock no significa que pueda atenderse antes de ser abastecido.

### 6.2. Pedidos en proceso

Abra **Pedidos** para ver los pedidos que se están procesando. La tabla muestra su cliente, nivel, progreso, tiempo restante y recursos. Cuando un pedido termina, deja de aparecer en esta tabla; puede consultar la cantidad completada en **Estadísticas**.

### 6.3. Recursos

Abra **Recursos** para consultar las unidades disponibles, en uso y la ocupación de cada recurso. En **Retienen** aparecen los pedidos que lo usan; en **Esperan**, los que aguardan una unidad. Que un recurso esté al 100 % solo indica que no tiene unidades disponibles.

## 7. Consultar estadísticas y comparar políticas

### 7.1. Estadísticas

Abra **Estadísticas** para consultar:

- **Pedidos completados**, **En espera de stock** y **En espera de recursos**.
- **Espera promedio**: tiempo desde la creación hasta que empieza el procesamiento.
- **Procesamiento promedio**: tiempo que tardan en procesarse los pedidos.
- **Cumplimiento SLA**: porcentaje de pedidos completados que empezaron dentro del tiempo de espera objetivo de su nivel.
- **Throughput**: pedidos completados por minuto simulado.
- **Ocupación por recurso** y **Detalle por nivel de servicio**.

Los promedios y el cumplimiento de SLA consideran los pedidos completados; los pedidos en espera se cuentan por separado. Los tiempos mostrados son simulados.

### 7.2. Comparador

El **Comparador** permite probar las tres políticas con la misma carga de pedidos, sin alterar la simulación en curso.

1. Abra **Comparador**.
2. Si lo desea, escriba una **Semilla (opcional)**: un número entero mayor o igual a cero. Usar la misma semilla y cantidad permite repetir la misma carga.
3. Indique la **Cantidad de pedidos (opcional)**, entre 1 y 1000. Si deja los campos vacíos, se usan la semilla 2026 y 250 pedidos.
4. Pulse **Comparar políticas**.
5. Revise **Resultado global** y **Espera promedio por nivel de servicio**. Los valores destacados ayudan a identificar los mejores resultados.

Los resultados del comparador pueden diferir de las estadísticas del tablero porque la comparación no incluye todas las condiciones de la simulación en curso, como la disponibilidad de recursos o de existencias.

## 8. Atender un conflicto

Un interbloqueo ocurre cuando varios pedidos quedan esperando recursos entre sí y no pueden continuar. En **Conflicto** se muestra el modo actual y, si existe un interbloqueo, los pedidos y recursos involucrados.

Para resolverlo:

1. Abra **Conflicto** y revise la alerta **Interbloqueo detectado**.
2. En **Elegir el pedido que continúa**, pulse **Continuar este** junto al pedido elegido.
3. Los demás pedidos involucrados liberan sus recursos y vuelven a la lista de espera. El pedido elegido puede continuar.
4. Compruebe el resultado en **Recursos** y **Lista de espera**.

Para crear un conflicto de prueba, pulse **Provocar conflicto** y espere la alerta. Esta acción desactiva la prevención; cuando termine, pulse **Activar prevención** si desea volver a habilitarla. Para una demostración más clara, detenga el generador y reinicie la simulación antes de provocar el conflicto; tenga en cuenta que el reinicio borra los pedidos y lotes actuales.

## 9. Reiniciar la simulación

1. Abra **Controles**.
2. Pulse **Reiniciar simulación**.
3. Lea el aviso y confirme solo si desea continuar.

El reinicio detiene el generador y elimina los pedidos y lotes actuales. También restablece los recursos, las existencias, el almacén, el reloj, la política y el modo de prevención. Los clientes creados permanecen registrados.

## 10. Problemas frecuentes

- **La aplicación aparece desconectada:** compruebe que esté en ejecución y recargue la página. Si no se restablece, solicite asistencia.
- **No encuentro un pedido:** revise **Lista de espera** y **Pedidos**. Si ya terminó, deja de aparecer en esas tablas y se cuenta en **Estadísticas**.
- **Un pedido permanece en espera de stock:** registre una llegada suficiente del producto que falta y revise si hay espacio en **Almacén**.
- **Una llegada no aparece en el almacén:** espere a que se procese y revise la ocupación. Si el almacén está lleno, el lote puede quedar pendiente.
- **Un botón está deshabilitado:** revise los campos obligatorios y espere si hay una operación en curso. En **Controles**, algunas acciones se deshabilitan cuando el generador ya está en el estado solicitado o la política seleccionada ya está activa.
- **No hay resultados en el comparador:** pulse **Comparar políticas** para generar una comparación.
