# Cine UADE

Sistema de gestión de un cine: cartelera, funciones, reserva de butacas, cobro, candy y
arqueo de caja. TPO de Aplicaciones Interactivas (UADE).

25 casos de uso y 20 reglas de negocio sobre MySQL, con 552 tests en el backend y 55 en Swing.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring MVC, Spring Data JPA |
| Base | MySQL 8.4, también para los bloqueos de butaca mientras se elige |
| Frontend | React, React Router y Mantine, compilado con Vite y servido por nginx |
| Escritorio | Java 21 y Swing (FlatLaf, JCalendar): el panel del encargado y la Puerta |
| Despliegue | Docker Compose, 4 servicios en dos redes |

## Estructura

| Carpeta | Qué hay |
|---|---|
| `cine-backend/` | La API y las reglas de negocio |
| `cine-frontend/` | Las pantallas del cliente (la venta web, sin login) |
| `cine-swing/` | El panel del encargado de escritorio, que habla con la API por HTTP |
| `cine-docker/` | El `docker-compose.yml` que levanta todo |
| `_other/` | Documentación e instrucciones |

## Hacia dónde va

| | Qué | Estado |
|---|---|---|
| **1** | Backend con las reglas R1..R20, API REST y Spring Security | Hecho |
| **2** | Web del cliente en **React** (cartelera, compra, ticket) | Hecho |
| **3** | Panel del encargado y Puerta en **Java Swing**, en línea contra la API | Hecho |
| **4** | **Terminal de boletería** Swing que vende sin red, con base local que sincroniza | Pendiente |
| **5** | **Desplegar** en un servidor | Pendiente |

La terminal del punto 4 es otra cosa que el panel del punto 3: vende y valida entradas sin
depender del servidor, con su propia base, y sube cada venta al volver la conexión. Cómo
está pensada —qué corre en cada lado, qué se sincroniza y qué pasa cuando el servidor
rechaza una venta— está en [la sección del manual](_other/docs/manual/index.html#terminal).

## Documentación

| Archivo | Qué es |
|---|---|
| [`_other/COMO-LEVANTARLO.md`](_other/COMO-LEVANTARLO.md) | Cómo ponerlo a andar, paso a paso |
| [`_other/docs/manual/index.html`](_other/docs/manual/index.html) | El manual: requerimientos, casos de uso, reglas, arquitectura y 18 diagramas |
| [`cine-frontend/API.md`](cine-frontend/API.md) | El contrato HTTP, endpoint por endpoint |
| [`_other/demo/cine-uade.postman_collection.json`](_other/demo/cine-uade.postman_collection.json) | La demo de la Etapa 1: GET, POST, PUT y DELETE, con casos exitosos y de error. Se importa en Postman, Bruno o Insomnia |
| `localhost:8080/swagger-ui.html` | El mismo contrato, para probar desde el navegador (con el sistema levantado) |

Panel del encargado de escritorio, con el sistema levantado: `cd cine-swing && mvn exec:java`
(JDK 21), `encargado@cine.uade.ar` / `cine2026` (demo; `puerta@cine.uade.ar` / `cine2026` entra solo a Puerta). Ver [`cine-swing/README.md`](cine-swing/README.md).

## Tareas

El backlog está en [Linear](https://linear.app/tpo-aplicaciones-interactivas/team/TPO/all).

## Convenciones

1. Las reglas de negocio van en `service/`, nunca en `controller/` ni en el frontend.
2. `ArquitecturaTest` falla si una capa importa a otra que no tiene debajo.
3. Si tocás la API, actualizá `API.md` en el mismo commit.
4. Todo en español: clases, métodos, variables, comentarios y mensajes de error.
