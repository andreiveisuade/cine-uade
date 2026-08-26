# Cine UADE — frontend

Interfaz web del sistema de salas de cine de Aplicaciones Interactivas.
HTML + JavaScript + Tailwind, sin framework ni build step.

Es una de las tres carpetas del monorepo:

```
cine-uade/
├── cine-backend/    Java 21 + Spring Boot + Spring Data JPA + MySQL
├── cine-frontend/   esta carpeta
└── cine-docker/     el compose que levanta todo
```

## Cómo correrlo

Con los contenedores, desde `cine-docker`:

```sh
docker compose up -d
open http://localhost:8080
```

- `index.html` — cliente, sin login: cartelera, funciones, butacas, reserva y ticket.
- `admin.html` — encargado, con login: películas, salas, funciones, grillas, el
  planificador de la semana, reservas, cobro, informes por función y caja.

Credenciales de prueba del encargado: `encargado@cine.uade.ar` / `cine2026`.

Después de tocar el código hay que rebuildear la imagen. Con `--no-deps`, para que
Compose no recree también MySQL y el backend:

```sh
docker compose up -d --build --no-deps frontend
```

## De dónde salen los datos

Todo el acceso a datos pasa por `js/api.js`, que reexporta `js/api-http.js`: la
implementación contra la API REST del backend.

El contrato de los endpoints está en [API.md](API.md).

Para desarrollar hace falta el backend levantado (`docker compose up` en
`../cine-docker`): `python3 -m http.server` no alcanza, porque `/api` lo resuelve nginx
como reverse proxy hacia el backend por la red interna, y por eso el front no conoce ni
el host ni el puerto del backend, y no hace falta CORS.

## Estructura

```
index.html        cliente
admin.html        encargado
Dockerfile        nginx unprivileged
nginx.conf        estáticos + reverse proxy de /api
API.md            contrato con el backend
js/
  api.js          reexporta api-http.js
  api-http.js     implementación contra la API REST
  router.js       ruteo por hash (#/pelicula/3)
  theme.js        toggle claro/oscuro, persistido en localStorage
  butacas.js      dibujo del mapa de la sala, compartido por cliente y encargado
  componentes.js  piezas de HTML reutilizables (campo, panel, tabla, botón...)
  etiquetas.js    traducción de los enums del dominio a texto legible
  formato.js      formateo de plata, fecha y hora
  dom.js          escapado, avisos por pantalla y el resto del contacto con el DOM
  cliente.js      mapa de rutas del cliente
  admin.js        mapa de rutas del encargado + guardia por rol
  cliente/        una vista por archivo: cartelera, pelicula, funcion, compra,
                  confirmar, ticket, mis-reservas, registro
  admin/          una vista por archivo: peliculas, salas, funciones, funcion,
                  agenda, programaciones, planificador, promociones, reservas,
                  caja, puerta, importador, pendientes, login, sesion
```

Cada vista es una funcion `async (contenedor, ...params)` registrada en el mapa de
`cliente.js` / `admin.js`. Agregar una pantalla es agregar un archivo y una linea en ese
mapa: los modulos no se conocen entre si.

## Dos cosas del dominio que el front respeta

**Una butaca no está ocupada en sí misma**: lo está *en una función*, si alguna reserva
no cancelada de esa función la tomó. *Fuera de servicio*, en cambio, le pertenece al
asiento y vale para todas las funciones. Por eso el mapa recibe `ocupado` y `estado`
como campos separados, y los pinta distinto.

**Una sala no es un rectángulo**: `butacasPorFila` dice cuántas butacas tiene cada fila,
así que `[8,10,12,12,14]` es una sala en cuña. El mapa se dibuja fila por fila con esa
lista, no con un ancho fijo.

El precio de cada butaca sale de
`precio base de la función × multiplicador de sala × multiplicador de butaca`, y lo
calcula el backend: el front nunca lo recalcula, solo lo muestra.

## Estado

Cubre todos los casos de uso del manual **menos el candy** (CU-13 a CU-16): la API ya
sirve la carta, los combos y las ventas, y ninguna pantalla las consume todavia. Es la
unica deuda de este tipo que queda.

Con pantalla y andando: el armado automatico de la grilla (`#/planificador`), la agenda
(`#/agenda`), el bordero del INCAA y el informe por funcion (`#/funcion/{id}`), el cobro
por checkout de la pasarela y la validacion de entradas en la puerta (`#/puerta`).
