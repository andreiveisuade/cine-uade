# Contrato de la API

Lo que el frontend espera del backend. Cada función de `js/api.js` corresponde a un
endpoint de esta lista: hoy resuelven contra `mock/datos.js` y se van a reemplazar por
`fetch` sin tocar ninguna vista.

Base: `/api`. El navegador la pide al mismo origen y nginx la reenvía al backend por la
red interna de Docker, así que no hace falta CORS.

## Convenciones

- JSON en request y response, `Content-Type: application/json`.
- Fechas y horas: ISO local sin zona, `2026-08-13T20:30:00`. Solo fecha: `2026-08-13`.
- Los enums viajan con el **nombre de la constante** (`MAS_16`, `TRES_D`, `EFECTIVO`),
  nunca con la etiqueta para mostrar. El frontend traduce.
- Precios: número, no string. El backend ya aplica los multiplicadores.
- Errores de validación: **400** con `{"error": "La butaca B5 está fuera de servicio"}`.
  El texto se muestra tal cual al usuario, así que tiene que ser el mensaje que hoy
  tiran los gestores. No encontrado: **404** con el mismo formato.

## Catálogos

| Método | Ruta | Devuelve |
|---|---|---|
| GET | `/api/generos` | `["ACCION", "COMEDIA", …]` |
| GET | `/api/clasificaciones` | `[{"nombre":"ATP","edadMinima":0}, …]` |
| GET | `/api/tipos-sala` | `[{"nombre":"IMAX","multiplicador":1.6,"soportaTresD":true}, …]` |
| | | Valores: `DOS_D`, `TRES_D`, `IMAX`, `CUATRO_D`. No hay tipo `VIP`: lo premium lo dice el tipo de butaca |
| GET | `/api/idiomas` | `["DOBLADA","SUBTITULADA"]` |
| GET | `/api/proyecciones` | `["DOS_D","TRES_D"]` |
| GET | `/api/medios-pago` | `[{"nombre":"EFECTIVO","requiereAutorizacion":false}, …]` |

Los tres últimos campos calculados (`multiplicador`, `soportaTresD`,
`requiereAutorizacion`) los necesita el front para anticipar R8 y R11 antes de enviar
el formulario.

## Cliente

### `GET /api/cartelera?genero=ACCION`
Solo las películas en exhibición: `listarEnCartelera()`, no `listar()`. `genero` es
opcional (CU-01b).

```json
[{ "id": 1, "titulo": "Matrix", "duracionMinutos": 136,
   "generos": ["ACCION","CIENCIA_FICCION"], "clasificacion": "MAS_16",
   "posterUrl": "https://…", "director": "…", "anio": 1999,
   "idiomaOriginal": "Inglés", "sinopsis": "…", "enCartelera": true }]
```

### `GET /api/peliculas/{id}`
Una película, con los mismos campos.

### `GET /api/peliculas/{id}/funciones`
Funciones de esa película, ordenadas por `inicio`, con la sala embebida para no pedirla
aparte:

```json
[{ "id": 1, "peliculaId": 1, "salaId": 1, "inicio": "2026-08-13T20:30:00",
   "idioma": "SUBTITULADA", "proyeccion": "DOS_D", "precio": 5000,
   "sala": { "id": 1, "nombre": "Sala 1", "tipo": "IMAX",
             "butacasPorFila": [8,10,12,12,14], "filas": 5, "capacidadSala": 56 },
   "precioDesde": 8000 }]
```

`precioDesde` es `precio × multiplicador de sala`: la butaca más barata.

### `GET /api/funciones/{id}`
El endpoint del mapa de butacas. Lo de arriba **más** todas las butacas de la sala, cada
una con su precio ya calculado y si está ocupada **en esa función**:

```json
{ "id": 1, "…": "…",
  "asientos": [
    { "id": 5, "salaId": 1, "fila": 1, "numero": 5, "codigo": "A5",
      "tipo": "ESTANDAR", "estado": "HABILITADO", "ocupado": false, "precio": 8000 }],
  "libres": 50 }
```

`ocupado` se calcula: hay una entrada con ese asiento en alguna reserva **no cancelada**
de esta función. `estado` es del asiento y vale para todas las funciones. Los dos tienen
que venir separados: el front los pinta distinto.

### `POST /api/reservas`
```json
{ "funcionId": 1, "nombre": "Andrei Veis", "email": "andrei@uade.edu.ar",
  "codigos": ["C5","C6"] }
```
Si el email no existe, da de alta el cliente. Devuelve la reserva creada con sus
entradas. Valida R4 (butaca ocupada), R9 (fuera de servicio) y butacas repetidas.

### `GET /api/reservas/{id}`
Todo lo que necesita el ticket: la reserva más `funcion`, `pelicula`, `sala`, `cliente`
y `total`.

## Encargado

### `POST /api/sesion`
`{ "email": "…", "password": "…" }` → el administrador sin el hash.
**El mismo error para email inexistente y contraseña equivocada**, como
`GestorAdministradores`.

> Pendiente de definir entre las dos partes: hoy el front guarda la sesión en
> `sessionStorage` y los endpoints de admin no piden credenciales. Si se agrega token,
> avisar y lo mando en `Authorization`.

| Método | Ruta | Notas |
|---|---|---|
| GET | `/api/peliculas` | Todas, incluidas las que no están en cartelera |
| POST | `/api/peliculas` | R1 título único, R2 duración > 0, R7 un género, R10 clasificación |
| PUT | `/api/peliculas/{id}` | Campos parciales. El título único se compara contra **las otras** |
| DELETE | `/api/peliculas/{id}` | 400 si tiene funciones programadas |
| GET | `/api/salas` | |
| GET | `/api/salas/{id}` | Con `asientos` |
| POST | `/api/salas` | `{nombre, tipo, butacasPorFila, codigosVip, codigosPareja, codigosAccesibles}` |
| DELETE | `/api/salas/{id}` | 400 si tiene funciones |
| PUT | `/api/salas/{salaId}/asientos/{codigo}` | `{"estado":"FUERA_DE_SERVICIO"}` o `HABILITADO` |
| GET | `/api/funciones` | Todas, con `pelicula` y `sala` embebidas |
| POST | `/api/funciones` | R3 superposición, R8 3D en sala que no soporta |
| DELETE | `/api/funciones/{id}` | 400 si tiene reservas activas |
| GET | `/api/reservas` | Con `funcion`, `pelicula`, `sala`, `cliente`, `total` y `pago` (o `null`) |
| POST | `/api/reservas/{id}/cancelacion` | R6: libera las butacas |

### `POST /api/reservas/{id}/pago`
```json
{ "medio": "CREDITO", "codigoAutorizacion": "AUTH-40219" }
```
**El monto no viaja**: lo calcula el backend con el total de la reserva. R5 (solo
`RESERVADA`), R11 (código obligatorio si el medio lo exige), y un pago por reserva.

### `GET /api/arqueo?fecha=2026-08-13`
```json
{ "fecha": "2026-08-13", "total": 45450, "entradas": 5,
  "porMedio": { "EFECTIVO": {"cantidad":1,"total":24000} },
  "pagos": [{ "id":1, "reservaId":1, "monto":24000, "medio":"EFECTIVO",
              "fecha":"2026-08-13T14:22:00", "codigoAutorizacion":"",
              "pelicula": {…}, "cliente": {…}, "entradas": 3 }] }
```
