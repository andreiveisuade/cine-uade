# Cine UADE

Sistema de gestión de un cine: cartelera, funciones, reserva de butacas, cobro, candy,
control de acceso y arqueo de caja. TPO de Aplicaciones Interactivas (UADE).

25 casos de uso (CU-01 a CU-20 y sus variantes) y 20 reglas de negocio (R1 a R20) sobre
MySQL, con 1149 tests en el backend y 60 en Swing.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 3.5 (Spring MVC, Spring Data JPA, Spring Security), Lombok |
| Base | MySQL 8.4, también para los bloqueos de butaca mientras se elige |
| Frontend | React 19, React Router y Mantine, compilado con Vite y servido por nginx |
| Escritorio | Java 21 y Swing (FlatLaf, JCalendar, Jackson): el panel del encargado y la Puerta |
| Despliegue | Docker Compose, 4 servicios en dos redes |

## Estructura

Un monorepo con un solo historial. Cada carpeta tiene su README:

| Carpeta | Qué hay |
|---|---|
| `cine-backend/` | La API, las reglas de negocio y el importador de cartelera de TMDB. Su arquitectura está en [el manual](_other/docs/manual/index.html#capas) |
| [`cine-frontend/`](cine-frontend/README.md) | La venta web al cliente, sin login |
| [`cine-swing/`](cine-swing/README.md) | El panel del encargado y la Puerta del acomodador, de escritorio, que hablan con la API por HTTP |
| [`cine-docker/`](cine-docker/README.md) | El `docker-compose.yml` que levanta todo, el seed y los scripts de instalación |
| [`_other/`](_other/docs/README.md) | El manual, los diagramas, la guía para levantarlo y la colección de Postman |

## Levantarlo

Con Docker Desktop abierto:

```bash
cd cine-docker && ./setup.sh      # Windows: .\setup.ps1
```

Queda la web del cliente en <http://localhost:8080>. El panel de escritorio, con JDK 21 y
Maven: `cd cine-swing && mvn exec:java`, con `encargado@cine.uade.ar` / `cine2026` (ve todo) o
`puerta@cine.uade.ar` / `cine2026` (solo Puerta). Paso a paso, a mano, tests y problemas
comunes: [`_other/COMO-LEVANTARLO.md`](_other/COMO-LEVANTARLO.md).

## Lo que conviene saber al usarlo

Son reglas del backend: la web y el panel muestran su mensaje tal cual. El detalle de cada
una está en [`API.md`](cine-frontend/API.md) y en el manual.

- La butaca que un cliente está eligiendo queda apartada **3 minutos**. Una reserva sin cobrar
  **vence a los 30 minutos** (R17) y una función que ya empezó no se reserva ni se cobra (R19).
- Hasta **10 butacas** por compra y **20 unidades** por producto en una venta de candy.
- El candy asociado a una reserva exige que esté **pagada** y sea del mismo cliente.
- Por la **Puerta** se entra solo con la reserva pagada y el **día de la función**; cada
  entrada se usa una vez.
- Las promociones no se acumulan y las tarifas reducidas quedan afuera (R15, R16). El
  descuento se calcula al cobrar, porque depende del medio de pago.
- Salas de hasta 26 filas y 40 butacas por fila; películas de 1 a 600 minutos; limpieza de 0
  a 120; precios hasta $ 1.000.000. No se programa una función que ya empezó (R20) ni a más
  de un año.
- Las películas no se cargan a mano: las trae el **Importador** del panel desde TMDB y el
  encargado las confirma en **Por revisar**.

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
| [`_other/COMO-LEVANTARLO.md`](_other/COMO-LEVANTARLO.md) | Cómo ponerlo a andar, paso a paso, y qué hacer si algo falla |
| [`_other/docs/PRUEBAS.md`](_other/docs/PRUEBAS.md) | Cómo se verifica antes de entregar: suites, Postman, humo de punta a punta y recorrida visual |
| [`_other/docs/manual/index.html`](_other/docs/manual/index.html) | El manual y fuente de verdad: requerimientos, casos de uso, reglas, arquitectura, decisiones y diagramas. Se regenera como dice [`_other/docs/README.md`](_other/docs/README.md) |
| [`cine-frontend/API.md`](cine-frontend/API.md) | El contrato HTTP, endpoint por endpoint, con validaciones, topes y errores |
| [`_other/demo/cine-uade.postman_collection.json`](_other/demo/cine-uade.postman_collection.json) | La demo de la Etapa 1: GET, POST, PUT, PATCH y DELETE, con casos exitosos y de error. Se importa en Postman, Bruno o Insomnia |
| `localhost:8080/swagger-ui.html` | El mismo contrato, para probar desde el navegador (con el sistema levantado) |

## Tareas

El backlog está en [Linear](https://linear.app/tpo-aplicaciones-interactivas/team/TPO/all).

## Convenciones

1. Las reglas de negocio van en el backend: los invariantes de un objeto en su entidad (con su
   `Validador<Entidad>` en `model/`), lo que necesita repositorios o el reloj en los gestores de
   `service/`. Nunca en `controller/` ni en los clientes, que validan solo formato y presencia.
2. `ArquitecturaTest` falla si una capa importa a otra que no tiene debajo.
3. Si tocás la API, actualizá `API.md` en el mismo commit; si cambia una regla o una decisión,
   `_other/docs/manual/template.html`.
4. Todo en español: clases, métodos, variables, comentarios y mensajes de error.
5. Commits con conventional commits en español y en minúscula, sin acentos.
