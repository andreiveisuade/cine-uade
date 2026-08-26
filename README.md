# Cine UADE

Sistema de gestión de un cine: cartelera, funciones, reserva de butacas, cobro, candy y
arqueo de caja. TPO de Aplicaciones Interactivas (UADE).

25 casos de uso y 19 reglas de negocio sobre MySQL, con 385 tests.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring MVC, Spring Data JPA |
| Base | MySQL 8.4. Redis para los bloqueos de butaca |
| Frontend | HTML, JavaScript y Tailwind, servido por nginx |
| Despliegue | Docker Compose, 5 servicios en dos redes |

## Estructura

| Carpeta | Qué hay |
|---|---|
| `cine-backend/` | La API y las reglas de negocio |
| `cine-frontend/` | Las pantallas del cliente y del panel |
| `cine-docker/` | El `docker-compose.yml` que levanta todo |
| `_other/` | Documentación e instrucciones |

## Hacia dónde va

Tres frentes, en este orden:

| | Qué | Cuándo |
|---|---|---|
| **1** | Backend sólido y **terminal de boletería en Java Swing**, con base local que sincroniza | Primera entrega |
| **2** | Migrar las pantallas web a **React** | Segunda entrega |
| **3** | **Desplegar** en un servidor | Segunda entrega |

La terminal vende y valida entradas sin depender del servidor: tiene su propia base y sube
cada venta al toque. Cómo está pensada —qué corre en cada lado, qué se sincroniza y qué
pasa cuando el servidor rechaza una venta— está en
[la sección del manual](_other/docs/manual/index.html#terminal).

## Documentación

| Archivo | Qué es |
|---|---|
| [`_other/COMO-LEVANTARLO.md`](_other/COMO-LEVANTARLO.md) | Cómo ponerlo a andar, paso a paso |
| [`_other/docs/manual/index.html`](_other/docs/manual/index.html) | El manual: requerimientos, casos de uso, reglas, arquitectura y 18 diagramas |
| [`cine-frontend/API.md`](cine-frontend/API.md) | El contrato HTTP, endpoint por endpoint |

## Tareas

El backlog está en [Linear](https://linear.app/tpo-aplicaciones-interactivas/team/TPO/all).

## Convenciones

1. Las reglas de negocio van en `service/`, nunca en `controller/` ni en el frontend.
2. `ArquitecturaTest` falla si una capa importa a otra que no tiene debajo.
3. Si tocás la API, actualizá `API.md` en el mismo commit.
4. Todo en español: clases, métodos, variables, comentarios y mensajes de error.
