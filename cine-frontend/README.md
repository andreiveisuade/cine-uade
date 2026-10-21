# cine-frontend

Venta web al cliente, con cuenta opcional: cartelera, butacas, confirmación, ticket y mis reservas. React 19, React
Router y Mantine, compilado con Vite y servido por nginx. El encargado usa [`cine-swing`](../cine-swing/README.md).

## Correrlo

Con el sistema levantado ([`cine-docker`](../cine-docker/README.md)) y Node 20.19 o superior:

```sh
npm install
npm run dev        # localhost:5173, con recarga; Vite reenvía /api al 8080
npm run build      # compila a dist/
npm test           # Vitest + Testing Library (src/**/*.test.jsx), sin backend
npm run lint       # ESLint con las reglas de hooks
npm run coverage   # reporte en coverage/ (ignorado en git)
```

Tras tocar el front, para verlo en el 8080: `docker compose up -d --build --no-deps frontend` desde `cine-docker`.

## Carpetas

Una carpeta por funcionalidad. `app/` arma el marco; `compartido/` y `api/` no conocen ninguna pantalla.

```
src/
  app/            main.jsx, Proveedores (tema, Motion, avisos), AppCliente (marco), Rutas, ErrorDePantalla
    layout/       Barra, Pie, Marca, BotonTema
  api/            api-http.js (único acceso al backend), etiquetas, formato, iconos
  compartido/     lo que usa más de una pantalla
    hooks/        useCargar, useTitulo
    layout/       Contenedor, Banda, Grilla, Destacados, Seccion, Encabezado, Volver
    estado/       Cargando, EsperaOError, ErrorCaja, Vacio, Avisos
    ui/           Poster, Chips, MontoAnimado, MediosDePago
    formularios/  faltantes
  cartelera/      home con portada, cartelera, película y trailer
  compra/         mapa de butacas, función, confirmar; selección, bloqueo y tarifas
  ticket/         la entrada, acciones, pagar online, espera del pago, venta cruzada
  cuenta/         sesión, ingresar, registro, mis reservas
  promos/         promos por día, candy y las tarjetas (también las usa el ticket)
  institucional/  nosotros
  test/           setup de Vitest y renderConApp
```

Un componente por archivo, con el nombre del archivo. Tests al lado de su componente.

## Cómo viaja un dato

```
Pelicula.jsx → useCargar(() => api.obtenerPelicula(id), [id]) → api-http.js → GET /api/peliculas/7 → backend
```

La pantalla nunca hace `fetch`. Si el backend rechaza, `api-http.js` tira un `Error` con su `{error}` y la
pantalla lo muestra con `EsperaOError` o `ErrorCaja`.

## Contextos y hooks propios

- `SesionDelCliente` / `useSesion`: quién tiene la sesión (`undefined` preguntando, `null` nadie).
- `CompraEnCurso` / `useCompra`: butacas elegidas con su tarifa, en memoria.
- `Avisos` / `useAvisar`: los toasts.
- `useCargar(pedir, deps)`: datos de una pantalla, descarta respuestas viejas.
- `useTitulo(texto)`: el título de la pestaña.
- `useRenovarBloqueo(funcionId)`: renueva el bloqueo de las butacas cada minuto.
- `useCambiarTarifa()`: cambia la tarifa de una butaca elegida.
- `useAlternarButaca`: elegir o soltar una butaca y bloquearla en el backend.
- `useReservar(funcionId)`: crea la reserva y va al ticket, o vuelve al mapa si perdió una butaca.
- `useEsperarPago(codigo, reserva)`: consulta cada 10 s hasta que el pago llega.
- `useFestejarPago`: confetti una vez cuando el pago llega con la pantalla abierta.
- `usePrevia`: trailer muteado al dejar el mouse sobre un póster.
- `useScrollCentrado`: el mapa de butacas arranca centrado en el celular.

## Dónde va algo nuevo

- **Pantalla:** un componente en la carpeta de su funcionalidad y su `<Route>` en `app/Rutas.jsx`. Si pasa
  de ~150 líneas, partirla en piezas con nombre de intención y la lógica a un `useXxx`.
- **Componente:** en la carpeta de la pantalla que lo usa; a `compartido/` recién cuando lo usa otra.
- **Operación de API:** una función en `api/api-http.js` y su ruta en `API.md`.

Imports: `./` dentro de la carpeta, `@/` (es `src/`) entre carpetas. Callbacks por prop: `alVerbo`
(`alElegir`, `alCambiar`); handlers internos en infinitivo (`reservar`, `cancelar`).

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
