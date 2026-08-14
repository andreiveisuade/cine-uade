# Cine UADE — frontend

Interfaz web del sistema de salas de cine de Aplicaciones Interactivas.
HTML + JavaScript + Tailwind, sin framework ni build step.

Es uno de los tres repos hermanos del TP:

```
TPO/
├── cine-backend/    Java + JDBC + MySQL. También el manual, en docs/manual/
├── cine-frontend/   este repo
└── cine-docker/     el compose que levanta los tres contenedores
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
  cliente.js      vistas del cliente
  admin.js        vistas del encargado
```

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

Cubre 12 de los 16 casos de uso del manual. Falta el módulo de candy (CU-13 a CU-16) y
que el arqueo (CU-12) separe boletería de candy.

Lo último que salió de la terminal y ya tiene pantalla: el armado automático de la grilla
(`#/planificador`), el borderó del INCAA y el informe por función (`#/funcion/{id}`, desde
el listado de funciones) y el cobro de los medios electrónicos por el checkout de la
pasarela.
