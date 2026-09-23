# Contrato de la API

Base `/api`; cada función de `src/api/api-http.js` es un endpoint de acá. Probable en
<http://localhost:8080/swagger-ui.html>.

## Convenciones

| | |
|---|---|
| Formato | JSON en request y response |
| Fechas | ISO local sin zona: `2026-08-13T20:30:00`; solo fecha `2026-08-13` |
| Enums | Nombre de la constante (`MAS_16`, `TRES_D`). El front traduce |
| Precios | Número, con los multiplicadores ya aplicados |
| Altas | `201` con `Location` al recurso creado (`/api/salas/7`), mismo cuerpo. Sin `Location`: checkout, grilla automática, importación y venta de candy, que no tienen `GET` por id. El cliente apunta a `/api/clientes?email=…` y el pago a `/api/reservas/{id}/pago` |
| Auth | HTTP Basic sin sesión: `Authorization: Basic base64(email:contraseña)` de un empleado en cada pedido |
| Errores | `{"error": "…"}`, texto que se muestra tal cual. `400` dato inválido o regla incumplida (un campo obligatorio que falta se rechaza antes de buscar el recurso de la ruta) · `401` login fallido o sin credenciales · `403` el rol no alcanza · `404` recurso o ruta inexistente, también un id del cuerpo que no existe (`peliculaId`, `salaId`, `clienteId`, `reservaId`…) · `405` método no aceptado · `409` butaca ganada por otro, o nombre/email/título ya usado (película, sala, cliente, producto, promoción) · `415` cuerpo no JSON · `500` falla del servidor (detalle solo al log) |

### Quién puede llamar a qué

| Nivel | Rutas |
|---|---|
| Público | `POST /api/sesion`, `POST /api/clientes`, `POST /api/reservas`, `POST /api/funciones/{id}/bloqueos`, `POST /api/reservas/codigo/{codigo}/cancelacion`; `GET` de `/api/cartelera`, `/api/peliculas/{id}`, `/api/peliculas/{id}/funciones`, `/api/funciones/{id}`, `/api/reservas/codigo/{codigo}`, `/api/reservas?email=` (con email), `/api/candy/productos` y `/{id}`, los siete catálogos; Swagger (`/swagger-ui/**`, `/v3/api-docs/**`) |
| `ACOMODADOR` o `ADMINISTRADOR` | `POST /api/acceso` |
| `ADMINISTRADOR` | Todo lo demás, incluidos `GET /api/reservas` sin email, las rutas de reserva por `{id}` y `GET /api/peliculas/pendientes`. Una ruta nueva nace así |

`401` sin `WWW-Authenticate`: «Hace falta iniciar sesión para esta operación» o, con credenciales
inválidas (rechazadas también en rutas públicas), «Email o contraseña incorrectos». `403`: «Tu rol
no tiene permiso para esta operación».

## Catálogos

| Ruta | Devuelve |
|---|---|
| `GET /api/generos` | `["ACCION", "COMEDIA", …]` |
| `GET /api/clasificaciones` | `[{"nombre":"ATP","edadMinima":0}, …]` |
| `GET /api/tipos-sala` | `[{"nombre":"IMAX","multiplicador":1.6,"soportaTresD":true}, …]`: `DOS_D`, `TRES_D`, `IMAX`, `CUATRO_D` |
| `GET /api/idiomas` | `["DOBLADA","SUBTITULADA"]` |
| `GET /api/proyecciones` | `["DOS_D","TRES_D"]` |
| `GET /api/medios-pago` | `[{"nombre":"EFECTIVO","requiereAutorizacion":false}, …]` |
| `GET /api/tarifas` | `[{"nombre":"JUBILADO","multiplicadorPrecio":0.5,"requiereAcreditacion":true}, …]` |

El front anticipa con ellos R8 (`multiplicador`, `soportaTresD`), R11 (`requiereAutorizacion`)
y el «traé el carnet» (`requiereAcreditacion`).

---

# Cliente

| Ruta | Qué hace |
|---|---|
| `GET /api/cartelera?genero=` | Solo en exhibición. `genero` opcional |
| `GET /api/peliculas/{id}` | Una película |
| `GET /api/peliculas/{id}/funciones` | Sus funciones por `inicio`, con la sala embebida |
| `GET /api/clientes?email=` | El cliente o `null`. Sin distinguir mayúsculas |
| `POST /api/clientes` | `{nombre, email}`. Email único (`409`). Opcional: reservar da de alta igual |

**Película**

```json
{ "id": 1, "titulo": "Matrix", "duracionMinutos": 136, "generos": ["ACCION"],
  "clasificacion": "MAS_16", "posterUrl": "…", "director": "…", "anio": 1999,
  "idiomaOriginal": "Inglés", "sinopsis": "…",
  "enCartelera": true, "estadoRevision": "CONFIRMADA" }
```

`estadoRevision` (`PENDIENTE`/`CONFIRMADA`/`DESCARTADA`): si entró al catálogo; lo importado
nace `PENDIENTE` y no se programa. `enCartelera`: si se está dando.

**Función**: suma `precioDesde` (precio × multiplicador de sala) y la sala con `minutosLimpieza`,
que cuenta para R3.

## Mapa de butacas

`GET /api/funciones/{id}?sesion=…`: la función con todas las butacas.

```json
{ "id": 1, "…": "…", "libres": 50,
  "asientos": [{ "id": 5, "fila": 1, "numero": 5, "codigo": "A5", "tipo": "ESTANDAR",
                 "estado": "HABILITADO", "ocupado": false, "precio": 8000 }] }
```

`ocupado` es de esta función (reservada o bloqueada, R4); `estado`, del asiento (R9). Mandar
siempre `sesion` en la compra: sin ella tus propios bloqueos se ven ocupados.

`POST /api/funciones/{id}/bloqueos`: aparta butacas mientras se elige, sin cliente ni ticket.

```json
{ "sesion": "3f9a…", "butacas": ["C5", "C6"] }
→ { "sesion": "3f9a…", "butacas": ["C5"], "rechazadas": ["C6"], "vencenEnSegundos": 180 }
```

- Va la **selección entera** (toma, renueva y suelta; `[]` suelta todo). Idempotente. Renovar antes de `vencenEnSegundos`.
- Perder una butaca es `200` con `rechazadas`, no `409`. Butaca inexistente: `400`.
- `sesion` = `crypto.randomUUID()` en `sessionStorage`; no es credencial.
- Sin Redis responde todo conseguido; la doble venta la frena la base.

## Reserva y pago

`POST /api/reservas`

```json
{ "funcionId": 1, "nombre": "…", "email": "…", "sesion": "3f9a…",
  "butacas": { "C5": "GENERAL", "C6": "JUBILADO" } }
```

- `butacas`: código → tarifa (`GENERAL`, `MENOR`, `JUBILADO`, `ESTUDIANTE`). Formato viejo `"codigos": [...]` = todas `GENERAL`.
- `sesion`: la de los bloqueos (sin ella tu bloqueo la rechaza; opcional en boletería). Al confirmar los suelta.
- R4, R9; alta del cliente si el email es nuevo; `409` si otra compra ganó la butaca. Devuelve `codigo` (QR) y `entradas[].tarifa`.

| Ruta | Qué hace |
|---|---|
| `GET /api/reservas/codigo/{codigo}` | El ticket del cliente: con `funcion`, `pelicula`, `sala`, `cliente`, `total` (subtotal de lista), `codigo`, `ingresadaEn`. Sin distinguir mayúsculas; inexistente: `404` |
| `POST /api/reservas/codigo/{codigo}/cancelacion` | El cliente cancela la suya. R6 libera butacas. R13: solo si está `RESERVADA` |
| `GET /api/reservas?email=` | Las de ese cliente, **sin `codigo`**: el email no prueba ser el dueño. Sin cliente: `200` con `[]` |
| `GET /api/reservas/{id}`, `POST /api/reservas/{id}/cancelacion` | Lo mismo por id, solo `ADMINISTRADOR`: el id es secuencial y se adivina |
| `GET /api/reservas` | Todas (listado del encargado) |

`POST /api/reservas/{id}/pago`

```json
{ "medio": "CREDITO", "codigoAutorizacion": "AUTH-40219" }
→ { "id": 2, "reservaId": 25, "subtotal": 15360, "promocionId": 1,
    "descuento": 7680, "monto": 7680, "medio": "EFECTIVO" }
```

- El monto no viaja: el descuento depende del medio. `monto` es lo que entra en caja.
- R5 (solo `RESERVADA`), R11 (código si el medio lo exige), R17 (no cobra vencida), un pago por reserva.
- `GET /api/reservas/{id}/pago`: lo mismo, o `null` si no se cobró.

`POST /api/reservas/{id}/checkout`: medios electrónicos, el código lo da el procesador. Efectivo: `400`.

```json
{ "medio": "QR" }
→ { "id": "MP-1234567890", "reservaId": 25, "medio": "QR", "monto": 7680,
    "urlPago": "https://checkout.emulado.local/mp/MP-1234567890",
    "codigoQr": "MP-QR|MP-1234567890" }
```

- `monto` con descuento; `codigoQr` es texto. R5, R17 y R19 se validan acá (no hay devolución, R13).
- `POST /api/checkouts/{id}/confirmacion`, sin cuerpo: devuelve el pago con `codigoAutorizacion`; repetirlo choca con R5.
- Pasarela emulada, sin red.

---

# Encargado

`POST /api/sesion`: `{email, password}` → el empleado sin hash, `rol` `ADMINISTRADOR` o `ACOMODADOR`.
Mismo `401` para email y clave. Sin token: el front guarda `email:contraseña` en `sessionStorage`
y ante un `401` fuera del login vuelve a `#/login`.

## Cartelera y salas

| Ruta | Notas |
|---|---|
| `GET /api/peliculas` | Todas, incluso fuera de cartelera |
| `GET /api/peliculas/pendientes` | El buzón. Va antes que `/{id}` en las rutas |
| `POST /api/peliculas` | R1 título único (`409`), R2 duración > 0, R7 un género, R10 clasificación. Nace `CONFIRMADA` |
| `POST /api/peliculas/importadas` | Igual, pero `PENDIENTE` y fuera de cartelera |
| `POST /api/peliculas/{id}/confirmacion` | `CONFIRMADA` y en cartelera |
| `POST /api/peliculas/{id}/descarte` | `DESCARTADA`. `400` si tiene funciones |
| `PUT /api/peliculas/{id}` | Parcial. Título único contra las otras (`409`) |
| `DELETE /api/peliculas/{id}` | `400` si tiene funciones o una grilla que la programe |
| `GET /api/salas` · `GET /api/salas/{id}` | El detalle trae `asientos` |
| `POST /api/salas` | `{nombre, tipo, butacasPorFila, codigosVip, codigosPareja, codigosAccesibles, minutosLimpieza}`. Limpieza opcional, 15 por defecto, no negativa |
| `PUT /api/salas/{id}` | `{nombre, tipo, minutosLimpieza}`. Butacas no editables; sin limpieza conserva la anterior; tipo fijo si tiene funciones (`400`) |
| `DELETE /api/salas/{id}` | `400` si tiene funciones |
| `PUT /api/salas/{salaId}/asientos/{codigo}` | `{"estado":"FUERA_DE_SERVICIO"}` o `HABILITADO` (R9) |
| `GET /api/funciones` | Con `pelicula` y `sala` embebidas |
| `POST /api/funciones` | R3 superposición, R8 3D en sala que no soporta |
| `DELETE /api/funciones/{id}` | `400` si tiene reservas, aun canceladas (R12: son historial) |

## Arqueo e informes

`GET /api/arqueo?fecha=2026-08-13`: la caja del día.

```json
{ "fecha": "…", "total": 45450, "entradas": 5,
  "porMedio": { "EFECTIVO": {"cantidad":1,"total":24000} },
  "pagos": [{ "id":1, "reservaId":1, "monto":24000, "medio":"EFECTIVO",
              "pelicula": {}, "cliente": {}, "entradas": 3 }] }
```

Borderó e informe cortan por **función** (INCAA), no por día.

`GET /api/funciones/{id}/bordero`

```json
{ "funcionId": 3, "pelicula": "Matrix", "sala": "Sala 1", "espectadores": 15,
  "recaudacionBruta": 67500, "descuentos": 5000, "recaudacionNeta": 62500,
  "porTarifa": { "GENERAL": {"cantidad":12,"total":60000} } }
```

- Solo lo **cobrado**; sin ventas da cero. Bruta a lista, neta lo que entró.
- `POST /api/funciones/{id}/bordero`: escribe `informes/bordero-funcion-<id>.txt`, `201`, pisa el anterior.
- `GET /api/funciones/{id}/informe`: `{ "boleteria": {borderó}, "comprasCandy": 4, "candy": 12000, "total": 74500 }`. Solo candy con `reservaId`: el de mostrador está en `GET /api/candy/arqueo`.

## Programaciones (CU-03b)

Genera funciones reales. `POST /api/programaciones/previsualizar` (no escribe) y `POST /api/programaciones`, mismo contrato:

```json
{ "peliculaId": 1, "salaId": 1, "desde": "2026-09-07", "hasta": "2026-09-13",
  "horaInicio": "20:30", "diasSemana": [], "idioma": "SUBTITULADA",
  "proyeccion": "DOS_D", "precio": 5000 }
→ { "programacion": { "id": 1, "…": "…", "activa": true },
    "funciones": [{ "inicio": "2026-09-07T20:30:00", "choca": false },
                  { "inicio": "2026-09-09T20:30:00", "choca": true,
                    "motivo": "la sala ya tiene la función 1 a las 09/09 21:00" }],
    "generadas": 6, "salteadas": 1 }
```

- `diasSemana` vacío = todos. Es `idioma`, no `version`. Previsualizado: `id` `0`. `motivo` solo si `choca`.
- Aplicar revalida: repintar con la respuesta.
- `400` ya al previsualizar: 3D en sala 2D (R8), `desde` > `hasta`, `horaInicio` mal formada, rango sin ningún `diasSemana`.

| Ruta | Notas |
|---|---|
| `GET /api/programaciones` | Todas, sin sus funciones |
| `GET /api/programaciones/{id}` | Con `funciones: [{id, inicio}, …]` |
| `PATCH /api/programaciones/{id}` | `{"activa": false}` la da de baja, `true` la reactiva. No hay `DELETE`. La baja no toca las funciones generadas |

## Grilla automática

`POST /api/grilla/propuesta` (no crea) y `POST /api/grilla`, mismo cuerpo. Solo `precio` es
obligatorio; default: una semana desde hoy, 14 a 24, ocho títulos.

```json
{ "desde": "2026-09-01", "dias": 7, "apertura": "14:00", "cierre": "00:00",
  "cuantasPeliculas": 8, "precio": 5000, "idioma": "SUBTITULADA", "proyeccion": "DOS_D" }
→ { "elenco": [{ "id": 4, "titulo": "…", "puntaje": 8.2, "duracionMinutos": 166, "pases": 12 }],
    "pases": [{ "peliculaId": 4, "salaId": 1, "inicio": "2026-09-01T14:00:00" }],
    "indicadores": { "minutosProgramados": 3320, "minutosDisponibles": 4200,
                     "ocupacion": 0.79, "puntajePromedio": 7.8,
                     "generosCubiertos": 6, "generosTotales": 9 },
    "funcionesCreadas": 0 }
```

`cierre: "00:00"` = fin del día. `minutosDisponibles` descuenta lo ya programado. Solo películas
confirmadas (ninguna: `400`); no pisa funciones existentes.

## Promociones (CU-17)

`POST /api/promociones`: los campos de beneficio que no aplican van en `null`.

```json
{ "nombre": "Miércoles 2x1", "tipo": "NXM", "lleva": 2, "paga": 1,
  "vigenciaDesde": "2026-08-01", "vigenciaHasta": "2026-12-31",
  "diasSemana": ["WEDNESDAY"], "horaDesde": null, "horaHasta": null, "mediosPago": [] }
```

| `tipo` | Campos | Ejemplo |
|---|---|---|
| `PORCENTAJE` | `porcentaje` (1 a 99) | 30% off |
| `MONTO_FIJO` | `monto` | $2000 off |
| `NXM` | `lleva` > `paga` | 2x1 |

- Listas vacías no restringen; se evalúa contra el horario de la **función**.
- `GET /api/promociones` y `GET /api/promociones/{id}`: activas e inactivas. `PATCH /api/promociones/{id}` con `{"activa": false}` la da de baja y con `true` la reactiva (sin `activa`: `400`); sin `DELETE`.
- R15: no se acumulan, gana el mayor descuento (empate: menor id). R16: tarifas reducidas afuera.

## Candy (CU-13 a CU-16)

| Ruta | Notas |
|---|---|
| `GET /api/candy/productos?todos=` | La carta; sin `todos=true`, solo lo disponible |
| `GET /api/candy/productos/{id}` | Un producto |
| `POST /api/candy/productos` | `{nombre, tipo, precio}` |
| `POST /api/candy/combos` | `{nombre, precio, componentes: {productoId: cantidad}}` |
| `PUT /api/candy/productos/{id}` | `{nombre, precio}`; tipo fijo. R14: `400` si un combo afectado deja de ser más barato que sus componentes |
| `PUT /api/candy/productos/{id}/disponibilidad` | `{disponible}`. Sin `DELETE`: vive en compras viejas |
| `POST /api/candy/compras` | La venta |
| `GET /api/candy/compras?fecha=&clienteId=` | Con `clienteId` gana el cliente; si no, el día |
| `GET /api/candy/arqueo?fecha=` | `{fecha, total, compras}`, caja aparte de boletería |

```json
{ "id": 1, "nombre": "Pochoclos grandes", "tipo": "POCHOCLOS", "precio": 4500,
  "disponible": true, "esCombo": false, "componentes": [] }
```

```json
{ "clienteId": 3, "reservaId": 25, "cantidades": { "1": 2, "4": 1 },
  "medio": "EFECTIVO", "codigoAutorizacion": "" }
→ { "id": 8, "clienteId": 3, "reservaId": 25, "fecha": "…", "medio": "EFECTIVO",
    "items": [], "total": 12000, "ahorro": 1500 }
```

Sin `reservaId` es venta de mostrador. `ahorro`: descuento del combo.

## Control de acceso (CU-18)

`POST /api/acceso` `{ "codigo": "K7M2P9XQ" }` → la reserva con butacas y tarifas. Marca la
entrada usada: repetido o impago da `400` (R18), inexistente `404`. 8 caracteres sin `O`, `I`, `0`, `1`.

## Importador

| Ruta | Notas |
|---|---|
| `POST /api/importaciones` | Corre y contesta al terminar (10-15 s). Cuerpo opcional |
| `GET /api/importaciones` | Las últimas 20 |
| `GET /api/importaciones/estado` | `{ "disponible": true, "detalle": "Listo para traer cartelera" }`. No consulta TMDB. Va antes que el listado |

```json
{ "paginas": 2 }
→ { "id": 7, "estado": "TERMINADA", "paginas": 2, "nuevas": 9, "salteadas": 20,
    "fallidas": 1, "detalle": "+ [41] Hablan las aves\n✗ Yo, narciso: La duración…" }
```

- `paginas` 1 a 3 (default 1, veinte títulos). `estado`: `EN_CURSO`, `TERMINADA`, `FALLIDA`. `detalle`: `+` al buzón, `✗` rechazada.
- Timeout nginx 180 s, backend 120 s. TMDB caído: `201` con `FALLIDA` y motivo en `detalle`.
- `400`: `Las páginas a importar van de 1 a 3` · `Ya hay una importación en curso: esperá a que termine` · `El importador corrió recién: esperá 60 segundos antes de volver a pedirlo`.
