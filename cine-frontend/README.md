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
- `admin.html` — encargado, con login: películas, salas, funciones, reservas y caja.

Credenciales de prueba del encargado: `encargado@cine.uade.ar` / `cine2026`.

Después de tocar el código hay que rebuildear la imagen. Con `--no-deps`, para que
Compose no recree también MySQL y el backend:

```sh
docker compose up -d --build --no-deps frontend
```

## De dónde salen los datos

Todo el acceso a datos pasa por `js/api.js`, que es un selector:

```js
export const FUENTE = "http"; // "mock" | "http"
```

- `"http"` → `js/api-http.js`, contra la API REST del backend. **Es el modo actual.**
- `"mock"` → `js/api-mock.js`, contra datos de ejemplo en memoria, sin backend.

Las dos implementaciones exponen exactamente el mismo conjunto de operaciones con la
misma forma, así que alternar es cambiar esa línea: ninguna vista se entera. El mock
sirve para trabajar en las pantallas sin levantar nada, y replica las validaciones del
backend para que el front no mande algo que va a fallar.

El contrato de los endpoints está en [API.md](API.md).

Servir el repo con `python3 -m http.server` alcanza **solo en modo mock**. En modo
`"http"` hace falta el contenedor: el `/api` lo resuelve nginx como reverse proxy hacia
el backend por la red interna, y por eso el front no conoce ni el host ni el puerto del
backend, y no hace falta CORS.

## Estructura

```
index.html        cliente
admin.html        encargado
Dockerfile        nginx unprivileged
nginx.conf        estáticos + reverse proxy de /api
API.md            contrato con el backend
mock/datos.js     datos de ejemplo, solo para el modo mock
js/
  api.js          selector: elige mock o http
  api-http.js     implementación contra la API REST
  api-mock.js     implementación contra el mock
  router.js       ruteo por hash (#/pelicula/3)
  butacas.js      dibujo del mapa de la sala, compartido por cliente y encargado
  ui.js           formatos y etiquetas de los enums del dominio
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
