# Cine UADE

Sistema de gestión de un cine: cartelera, funciones, reserva de butacas, cobro, candy, control de
acceso y caja.

Trabajo Práctico Obligatorio de **Aplicaciones Interactivas** — UADE, FAIN, 2.º cuatrimestre 2026.
Docente: Juan Ignacio López.

Integrantes: Andrei Veis, Lucas Ezequiel Manrique Simonini y Valentino Garmendia.

Sitio público: <https://cine-uade.onrender.com> (Render free, base en Aiven MySQL). Tarda hasta un minuto en
despertar si estuvo inactivo.

## Estructura

| Carpeta | Qué es | Tecnología |
|---|---|---|
| [`cine-backend/`](cine-backend/README.md) | API REST y reglas de negocio | Java 21, Spring Boot 3.5, JPA, Spring Security, MySQL 8.4 |
| [`cine-frontend/`](cine-frontend/README.md) | Venta web al cliente, con cuenta opcional | React 19, Mantine, Vite, nginx |
| [`cine-swing/`](cine-swing/README.md) | Panel del encargado y Puerta del acomodador | Java 21, Swing, FlatLaf |
| [`cine-android/`](cine-android/README.md) | Puerta del acomodador en el celular: escanea el QR del ticket | Kotlin, Jetpack Compose, CameraX, ML Kit |
| [`cine-docker/`](cine-docker/README.md) | Levanta todo: MySQL, backend, frontend y Adminer | Docker Compose |
| [`_other/`](_other/docs/README.md) | Manual, diagramas, colección de Postman y las fotos de la carta (`imagenes/`) | PlantUML |

En la raíz, además: el `Dockerfile` es la imagen de producción (compila el front y lo sirve el backend, un solo
origen `https`) y `render.yaml` el Blueprint de Render que la usa. Cómo encaja todo: [`ARQUITECTURA.md`](ARQUITECTURA.md).

## Levantarlo

Requiere Docker Desktop; el panel, además, JDK 21.

```bash
cd cine-docker && ./setup.sh          # Windows: .\setup.ps1
cd ../cine-swing && ./mvnw compile exec:java   # panel (Windows: .\mvnw.cmd)
cd ../cine-android && ./gradlew assembleDebug  # APK de la Puerta para el celular
```

La web queda en <http://localhost:8080>. Sin tokens anda entero. URLs, usuarios de demo, tokens opcionales, a mano
y problemas comunes: [`COMO-LEVANTARLO.md`](_other/COMO-LEVANTARLO.md).

## Por dónde empiezo

1. [`ARQUITECTURA.md`](ARQUITECTURA.md): capas, subdominios y el recorrido de una venta, clase por clase.
2. [Manual](_other/docs/manual/index.html), secciones Reglas y Casos de uso: qué tiene que hacer el sistema.
3. [`API.md`](cine-frontend/API.md): el contrato HTTP, endpoint por endpoint.
4. Para seguir una venta en el código, abrir `cine-backend/.../service/ventas/GestorReservas.java` (reservar) y
   `GestorPagos.java` (cobrar); el resto del camino está en [Flujo de una venta](ARQUITECTURA.md#flujo-de-una-venta).
5. [`PRUEBAS.md`](_other/docs/PRUEBAS.md): cómo se verifica antes de entregar, con la
   [colección de Postman](_other/demo/cine-uade.postman_collection.json).

El manual es la fuente de verdad: requerimientos, reglas, casos de uso, arquitectura, decisiones y diagramas.

## Scripts

| Script | Qué hace |
|---|---|
| `cine-docker/setup.sh` (`setup.ps1` en Windows) | Chequea Java 21, arma el `.env`, levanta, espera a que esté sano, siembra e imprime las URLs; se puede correr de nuevo |
| `cine-docker/seed/datos-de-ejemplo.sh` | Carga salas, la carta del candy y una promoción por la API (películas no: las trae el importador) |
| `cine-docker/aiven/cargar-aiven.sh` | Primer deploy: aplica `db/schema.sql` en Aiven y crea los dos empleados con claves nuevas |
| `cine-docker/aiven/migrar-aiven.sh <n>` | Aplica una migración de `db/migraciones/` a Aiven, por número o nombre (`17` o `clave-temporal`), antes del deploy |
| `_other/demo/humo.sh` | Pruebas de humo de punta a punta contra el sistema levantado; escribe datos: solo en una base desechable |
| `_other/imagenes/procesar.sh` | Recorta y convierte a webp las fotos de `originales/` a `listas/` (cómo se generaron: `PROMPTS.md`) |
| `_other/imagenes/cargar.sh` | Sube cada webp de `listas/` por `POST /api/imagenes` y se lo asigna a la promoción, producto o combo del mismo nombre |
| `_other/docs/diagramas/archify/exportar.sh` | Regenera un diagrama de Archify como SVG para el manual |

## Pendiente

- Terminal de boletería en Swing que venda sin red y sincronice al volver
  ([diseño](_other/docs/manual/index.html#terminal)).

## Convenciones

- Las reglas de negocio van en el backend; los clientes validan solo formato y presencia.
- Si cambia la API, se actualiza `API.md` en el mismo commit; si cambia una regla, el manual.
- Todo en español. Commits con conventional commits, en minúscula y sin acentos.
- Tareas en [Linear](https://linear.app/tpo-aplicaciones-interactivas/team/TPO/all).
