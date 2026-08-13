# Cine UADE — frontend

Interfaz web del sistema de salas de cine de Aplicaciones Interactivas.
HTML + JavaScript + Tailwind, sin framework ni build step.

El backend Java vive en un repo aparte: `Aplicaciones-Interactivas`, donde también
está el manual del proyecto con los requerimientos, casos de uso y reglas de negocio.

## Cómo correrlo

Hace falta servirlo por HTTP: son módulos ES y el navegador los bloquea sobre `file://`.

```sh
python3 -m http.server 5500
open http://localhost:5500/
```

- `index.html` — cliente, sin login: cartelera, funciones, butacas, reserva y ticket.
- `admin.html` — encargado, con login: ABM de películas y salas, funciones y reservas.

Credenciales de prueba del encargado: `encargado@cine.uade.ar` / `cine2026`.

## Estructura

```
index.html        cliente
admin.html        encargado
mock/datos.js     datos de ejemplo con la forma de la respuesta del backend
js/
  api.js          único punto de acceso a datos
  router.js       ruteo por hash (#/pelicula/3)
  butacas.js      dibujo del mapa de la sala
  ui.js           formatos y etiquetas de los enums del dominio
  cliente.js      vistas del cliente
  admin.js        vistas del encargado
```

## Todavía no hay API REST

Los datos salen de `mock/datos.js` y se acceden siempre a través de `js/api.js`, que
devuelve promesas con la forma exacta que tendría la respuesta del backend. Cuando la
API exista, se reemplaza el cuerpo de esas funciones por `fetch` y no cambia nada más.

El mock replica el dominio del backend: las seis salas de `SalasDeEjemplo`, sus 694
butacas con tipo y estado, y el cálculo de precio
`base × multiplicador de sala × multiplicador de butaca`.

Una butaca no está ocupada en sí misma: lo está **en una función**, si alguna reserva
no cancelada de esa función la tomó. *Fuera de servicio*, en cambio, le pertenece al
asiento y vale para todas las funciones.
