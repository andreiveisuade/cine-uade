# Cine UADE

Sistema de gestión de un cine: cartelera, funciones, reserva de butacas, cobro, candy, control de
acceso y caja.

Trabajo Práctico Obligatorio de **Aplicaciones Interactivas** — UADE, FAIN, 2.º cuatrimestre 2026.
Docente: Juan Ignacio López.

Integrantes: Andrei Veis, Lucas Ezequiel Manrique Simonini y Valentino Garmendia.

## Estructura

| Carpeta | Qué es | Tecnología |
|---|---|---|
| `cine-backend/` | API REST y reglas de negocio | Java 21, Spring Boot 3.5, JPA, Spring Security, MySQL 8.4 |
| [`cine-frontend/`](cine-frontend/README.md) | Venta web al cliente, sin login | React 19, Mantine, Vite, nginx |
| [`cine-swing/`](cine-swing/README.md) | Panel del encargado y Puerta del acomodador | Java 21, Swing, FlatLaf |
| [`cine-docker/`](cine-docker/README.md) | Levanta todo: MySQL, backend, frontend y Adminer | Docker Compose |
| [`_other/`](_other/docs/README.md) | Manual, diagramas y colección de Postman | PlantUML |

## Levantarlo

Requiere Docker Desktop; el panel, además, JDK 21 y Maven.

```bash
cd cine-docker && ./setup.sh          # Windows: .\setup.ps1
cd ../cine-swing && mvn exec:java     # panel de escritorio
```

| | |
|---|---|
| Web del cliente | <http://localhost:8080> |
| Swagger | <http://localhost:8080/swagger-ui.html> |
| Panel | `encargado@cine.uade.ar` / `cine2026` (todo) o `puerta@cine.uade.ar` / `cine2026` (Puerta) |

A mano, tests y problemas comunes: [`COMO-LEVANTARLO.md`](_other/COMO-LEVANTARLO.md).

## Documentación

- [Manual](_other/docs/manual/index.html): requerimientos, reglas R1..R20, casos de uso, arquitectura,
  decisiones y diagramas. Es la fuente de verdad.
- [`API.md`](cine-frontend/API.md): el contrato HTTP, endpoint por endpoint.
- [`PRUEBAS.md`](_other/docs/PRUEBAS.md): cómo se verifica antes de entregar.
- [Colección de Postman](_other/demo/cine-uade.postman_collection.json): GET, POST, PUT, PATCH y DELETE,
  con casos exitosos y de error.

## Pendiente

- Terminal de boletería en Swing que venda sin red y sincronice al volver
  ([diseño](_other/docs/manual/index.html#terminal)).
- Despliegue en un servidor.

## Convenciones

- Las reglas de negocio van en el backend; los clientes validan solo formato y presencia.
- Si cambia la API, se actualiza `API.md` en el mismo commit; si cambia una regla, el manual.
- Todo en español. Commits con conventional commits, en minúscula y sin acentos.
- Tareas en [Linear](https://linear.app/tpo-aplicaciones-interactivas/team/TPO/all).
