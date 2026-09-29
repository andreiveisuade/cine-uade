# Cine UADE — frontend

Las pantallas del cliente, sin login. React 19 + React Router + Mantine, sin TypeScript ni
librería de estado, compilado con Vite y servido por nginx.

## Cómo correrlo

Con el sistema completo, desde `cine-docker`:

```sh
docker compose up -d --build --no-deps frontend   # tras tocar el front
open http://localhost:8080
```

Para desarrollar, con el sistema levantado en Docker (Node 20.19 o 22.12 en adelante, lo que
pide Vite 8):

```sh
npm install
npm run dev        # localhost:5173, recarga en caliente
npm run build      # lo que hace el Dockerfile: compila a dist/
```

El dev server de Vite reenvía `/api` y Swagger a `localhost:8080` (ver `vite.config.js`): el
código pide `/api` igual que en producción y no hace falta CORS. Si cambiaste `PUERTO_WEB`,
cambiá `SISTEMA` en `vite.config.js`. No hay tests: `npm run build` es el chequeo de que compila.

`index.html` es la única entrada: cartelera (con filtro por género), película, butacas,
confirmación, ticket, mis reservas y registro. El encargado y el acomodador no usan la web:
tienen la app de escritorio [`cine-swing/`](../cine-swing/README.md), que consume el mismo
[API.md](API.md).

## Estructura

```
index.html               la entrada; ruteo por hash (#/pelicula/3)
vite.config.js           build + proxy de desarrollo
Dockerfile               node compila, nginx sin root sirve dist/
nginx.conf               estáticos + reverse proxy de /api y Swagger, errores propios en JSON
API.md                   contrato con el backend
src/
  Base.jsx               tema de Mantine, modo oscuro y avisos
  api/                   api-http.js (único acceso a la API), etiquetas.js, formato.js
  componentes/           useCargar, MapaButacas, Chips, Poster, Avisos, Estado, BotonTema,
                         NoExiste, Volver
  cliente/               main.jsx (arranque), AppCliente.jsx (rutas) + una pantalla por
                         archivo; compra.jsx es la selección en curso y el bloqueo de butacas
```

Sumar una pantalla es escribir su componente y agregar su `<Route>` en `AppCliente.jsx`.
Sumar una operación es agregarla en `src/api/api-http.js` y en [API.md](API.md).

## Lo que el front respeta

- Las reglas viven en el backend: el precio, el descuento y la validación los resuelve la API,
  y su `{"error"}` se muestra tal cual. Una respuesta de error sin ese JSON no se muestra cruda:
  va a la consola y en pantalla queda un mensaje según el código.
- Los enums viajan con el nombre de la constante; `etiquetas.js` los traduce.
- *Ocupada* es de la función y *fuera de servicio* es del asiento: el mapa los recibe
  separados y los pinta distinto. Cada fila tiene su propio largo (`butacasPorFila`).
- Las butacas elegidas quedan bloqueadas 3 minutos a nombre de la pestaña y se renuevan cada
  minuto mientras se elige: `sessionStorage` guarda solo ese id de compra, no credenciales. Si
  el backend rechaza una butaca al bloquearla o otra compra la ganó, se suelta de la selección
  y se dice por qué.
- El cliente llega a su reserva solo por el código de acceso (`#/ticket/<codigo>`): *Mis
  reservas* pide el código y no el email, porque el email no prueba ser el dueño.

## nginx

Es el único puerto que sale del compose. Además de servir `dist/` y reenviar `/api`,
`/swagger-ui` y `/v3/api-docs` al backend:

- Acepta cuerpos de hasta **1 MB**; más es un `413` en JSON.
- Sus propios `413`, `502` (backend caído) y `504` (no contestó a tiempo) salen como
  `{"error": "…"}`, igual que los del backend, así la web y Swing los muestran tal cual.
- `/api` corta a los 30 s; `/api/importaciones`, que espera a TMDB, a los 180 s.
- El HTML se revalida siempre y los `assets/` con hash se cachean para siempre.
