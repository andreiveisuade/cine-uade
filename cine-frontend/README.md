# Cine UADE — frontend

Las pantallas del cliente y del panel del encargado. React 19 + React Router + Mantine,
compilado con Vite y servido por nginx.

## Cómo correrlo

Con el sistema completo, desde `cine-docker`:

```sh
docker compose up -d --build --no-deps frontend   # tras tocar el front
open http://localhost:8080
```

Para desarrollar, con el sistema levantado en Docker:

```sh
npm install
npm run dev        # localhost:5173, recarga en caliente
```

El dev server de Vite reenvía `/api` y Swagger al nginx del 8080 (ver `vite.config.js`):
el código pide `/api` igual que en producción y no hace falta CORS.

- `index.html` — cliente, sin login: cartelera, película, butacas, confirmación, ticket,
  mis reservas y registro.
- `admin.html` — panel, con login. El acomodador solo ve Puerta.

Encargado de demo: `encargado@cine.uade.ar` / `cine2026`.

## Estructura

```
index.html  admin.html   las dos entradas; ruteo por hash (#/pelicula/3)
vite.config.js           build de dos páginas + proxy de desarrollo
Dockerfile               node compila, nginx sirve dist/
nginx.conf               estáticos + reverse proxy de /api y Swagger
API.md                   contrato con el backend
src/
  Base.jsx               tema de Mantine, modo oscuro y avisos
  api/                   api-http.js (único acceso a la API), etiquetas.js, formato.js
  componentes/           useCargar, MapaButacas, Chips, Poster, Avisos, Estado...
  cliente/               AppCliente.jsx (rutas) + una vista por archivo; compra.jsx es
                         la selección en curso y el bloqueo de butacas
  admin/                 AppAdmin.jsx (rutas, menú y guardia por rol), sesion.jsx,
                         comun.jsx y una vista por archivo
```

Sumar una pantalla es escribir su componente y agregar su `<Route>` en `AppCliente.jsx`
o `AppAdmin.jsx`. Sumar una operación es agregarla en `src/api/api-http.js` y en
[API.md](API.md).

## Lo que el front respeta

- Las reglas viven en el backend: el precio, el descuento y la validación los resuelve
  la API, y su `{"error"}` se muestra tal cual.
- Los enums viajan con el nombre de la constante; `etiquetas.js` los traduce.
- *Ocupada* es de la función y *fuera de servicio* es del asiento: el mapa los recibe
  separados y los pinta distinto. Cada fila tiene su propio largo (`butacasPorFila`).
