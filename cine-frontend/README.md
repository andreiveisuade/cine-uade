# cine-frontend

Venta web al cliente, sin login: cartelera, butacas, confirmación, ticket y mis reservas. React 19, React
Router y Mantine, compilado con Vite y servido por nginx. El encargado usa [`cine-swing`](../cine-swing/README.md).

## Correrlo

Con el sistema levantado ([`cine-docker`](../cine-docker/README.md)) y Node 20.19 o superior:

```sh
npm install
npm run dev        # localhost:5173, con recarga; Vite reenvía /api al 8080
npm run build      # compila a dist/; es el único chequeo, no hay tests
```

Tras tocar el front, para verlo en el 8080: `docker compose up -d --build --no-deps frontend` desde `cine-docker`.

## Estructura

```
nginx.conf        estáticos + proxy de /api y Swagger
API.md            contrato con el backend
src/
  api/            api-http.js (único acceso a la API), etiquetas.js, formato.js
  componentes/    useCargar, MapaButacas, Poster, Avisos…
  cliente/        AppCliente.jsx (rutas) + una pantalla por archivo
```

Una pantalla nueva es un componente en `cliente/` más su `<Route>` en `AppCliente.jsx`. Una operación nueva
va en `api-http.js` y en `API.md`.

## Lo que el front respeta

- El precio, el descuento y las reglas los resuelve el backend; su `{error}` se muestra tal cual.
- Los enums viajan con el nombre de la constante y `etiquetas.js` los traduce.
- Las butacas elegidas quedan bloqueadas 3 minutos y se renuevan cada minuto. `sessionStorage` guarda solo
  el id de compra.
- A una reserva se llega solo con el código de acceso: el email no prueba ser el dueño.

## nginx

Es el único puerto que sale del compose. Acepta cuerpos de hasta 1 MB y contesta sus propios 413, 502 y 504
como `{"error": "…"}`, así los clientes los muestran igual que los del backend. `/api` corta a los 30 s;
`/api/importaciones`, que espera a TMDB, a los 180 s.
