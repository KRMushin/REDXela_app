# REDXela — Centro de Distribución Simulado

Simulador de centro de distribución de comercio electrónico desarrollado para el curso **Sistemas Operativos 2**.

---

## Requisitos

- [Docker](https://docs.docker.com/get-docker/) 24+
- [Docker Compose](https://docs.docker.com/compose/) v2

No es necesario instalar Java, Maven ni Node.js en el equipo anfitrión.

---

## Levantar el proyecto

```bash
docker compose up --build
```

La primera vez descargará imágenes y compilará el código. Las siguientes ejecuciones serán más rápidas.

## Detener el proyecto

```bash
docker compose down
```

## Ver los logs

```bash
docker compose logs -f
```

---

## Accesos

| Servicio        | URL                                          |
|-----------------|----------------------------------------------|
| Frontend        | http://localhost:5173                        |
| Backend         | http://localhost:8080                        |
| Health custom   | http://localhost:8080/api/health             |
| Actuator health | http://localhost:8080/actuator/health        |

---

## Estructura del proyecto

```
PROYECTO_FINAL/
├── redxela_api/          → Backend Spring Boot
│   ├── src/main/java/    → Código fuente Java
│   ├── src/main/resources/ → application.yml
│   ├── src/test/         → Pruebas JUnit
│   └── Dockerfile        → Imagen multi-stage
│
├── redxela_client/       → Frontend React + TypeScript
│   ├── src/
│   │   ├── components/   → Componentes reutilizables
│   │   ├── pages/        → Páginas de la aplicación
│   │   ├── services/     → Clientes HTTP
│   │   ├── hooks/        → Hooks personalizados
│   │   ├── types/        → Definiciones TypeScript
│   │   └── styles/       → CSS global y de layout
│   └── Dockerfile        → Imagen de desarrollo
│
├── docs/                 → Documentación del proyecto
├── compose.yaml          → Docker Compose
├── .env.example          → Variables de entorno de ejemplo
├── .gitignore            → Ignorados de Git
├── AGENTS.md             → Guía para agentes de IA
└── README.md             → Documentación principal
```

---

## Desarrollo

### Hot reload del frontend

Los archivos `src/` del frontend están montados como volumen en el contenedor. Cualquier cambio en los archivos de `redxela_client/src/` se reflejará automáticamente en el navegador sin necesidad de reconstruir la imagen.

### Modificar el backend

El backend de Spring Boot **no tiene recarga automática** en esta configuración. Después de modificar código Java:

```bash
docker compose up --build backend
```

Esto recompila y reinicia únicamente el contenedor del backend.

### Ejecutar pruebas del backend localmente

Si tienes Java 21 y Maven instalados en el host:

```bash
cd redxela_api
./mvnw test
```

Las pruebas también se ejecutan automáticamente durante la construcción de la imagen Docker del backend.
