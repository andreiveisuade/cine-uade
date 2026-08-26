# Contrato de la API

Base `/api`. Cada función de `js/api.js` es un endpoint de esta lista.

## Convenciones

| | |
|---|---|
| Formato | JSON en request y response |
| Fechas | ISO local sin zona: `2026-08-13T20:30:00`. Solo fecha: `2026-08-13` |
| Enums | Viaja el nombre de la constante (`MAS_16`, `TRES_D`). El front traduce |
| Precios | Número, con los multiplicadores ya aplicados |
| Errores | `400` o `404` con `{"error": "…"}`. El texto se muestra tal cual al usuario |

## Catálogos

| Ruta | Devuelve |
|---|---|
| `GET /api/generos` | `["ACCION", "COMEDIA", …]` |
| `GET /api/clasificaciones` | `[{"nombre":"ATP","edadMinima":0}, …]` |
| `GET /api/tipos-sala` | `[{"nombre":"IMAX","multiplicador":1.6,"soportaTresD":true}, …]` — `DOS_D`, `TRES_D`, `IMAX`, `CUATRO_D` |
| `GET /api/idiomas` | `["DOBLADA","SUBTITULADA"]` |
| `GET /api/proyecciones` | `["DOS_D","TRES_D"]` |
| `GET /api/medios-pago` | `[{"nombre":"EFECTIVO","requiereAutorizacion":false}, …]` |
| `GET /api/tarifas` | `[{"nombre":"JUBILADO","multiplicadorPrecio":0.5,"requiereAcreditacion":true}, …]` |

Los campos calculados los usa el front para anticipar reglas antes de enviar el formulario:
`multiplicador` y `soportaTresD` para R8, `requiereAutorizacion` para R11, y
`requiereAcreditacion` para avisar «traé el carnet» al elegir la tarifa y no recién en la puerta.

---

# Cliente

| Ruta | Qué hace |
|---|---|
| `GET /api/cartelera?genero=` | Solo las que están en exhibición. `genero` opcional |
| `GET /api/peliculas/{id}` | Una película |
| `GET /api/peliculas/{id}/funciones` | Sus funciones, ordenadas por `inicio`, con la sala embebida |
| `GET /api/clientes?email=` | El cliente, o `null`. Sin distinguir mayúsculas |
| `POST /api/clientes` | `{nombre, email}`. Email único. Registrarse es opcional: reservar da de alta igual |

**Película**

```json
{ "id": 1, "titulo": "Matrix", "duracionMinutos": 136, "generos": ["ACCION"],
  "clasificacion": "MAS_16", "posterUrl": "…", "director": "…", "anio": 1999,
  "idiomaOriginal": "Inglés", "sinopsis": "…",
  "enCartelera": true, "estadoRevision": "CONFIRMADA" }
```

`estadoRevision` (`PENDIENTE`/`CONFIRMADA`/`DESCARTADA`) dice si entró al catálogo;
`enCartelera`, si se está dando. Lo que trae el importador nace `PENDIENTE` y no se puede
programar hasta confirmarlo.

**Función** — suma `precioDesde` (precio × multiplicador de sala) y la sala embebida con
`minutosLimpieza`: cuánto tarda en levantarse entre funciones. Cuenta para R3, así que
programar a las 22:00 algo que termina a las 22:00 da `400`.

## Mapa de butacas

### `GET /api/funciones/{id}?sesion=…`

La función más todas las butacas, con precio calculado y si está ocupada:

```json
{ "id": 1, "…": "…", "libres": 50,
  "asientos": [{ "id": 5, "fila": 1, "numero": 5, "codigo": "A5", "tipo": "ESTANDAR",
                 "estado": "HABILITADO", "ocupado": false, "precio": 8000 }] }
```

| Campo | Alcance |
|---|---|
| `ocupado` | De esta función: hay entrada en una reserva no cancelada, **o** está bloqueada |
| `estado` | Del asiento, para todas las funciones |

`sesion` es opcional pero **mandala siempre desde la pantalla de compra**: sin ella, apenas
bloqueás una butaca el mapa te la muestra tomada a vos mismo.

### `POST /api/funciones/{id}/bloqueos`

Mientras alguien elige, sus butacas dejan de ofrecerse. Todavía no hay cliente ni ticket.

```json
{ "sesion": "3f9a…", "butacas": ["C5", "C6"] }
→ { "sesion": "3f9a…", "butacas": ["C5"], "rechazadas": ["C6"], "vencenEnSegundos": 180 }
```

- Se manda la **selección entera**, no una butaca suelta: una llamada toma lo nuevo, renueva
  lo que sigue elegido y suelta lo deseleccionado. `[]` suelta todo. Idempotente.
- **Perder una butaca no es error**: responde `200`, no `409`. El front saca las
  `rechazadas` de la selección y avisa.
- Hay que **renovar** antes de `vencenEnSegundos`: volver a llamar con la misma selección.
- `sesion` la genera el navegador (`crypto.randomUUID()` en `sessionStorage`) y **no es una
  credencial**: lo peor que puede hacer una inventada es soltar el bloqueo de otro.
- Butaca inexistente: `400`.

> Si Redis no responde, contesta que todas se consiguieron y `rechazadas` viene vacío. El
> front no hace nada distinto. Que no se venda dos veces lo garantiza la base.

## Reserva y pago

### `POST /api/reservas`

```json
{ "funcionId": 1, "nombre": "…", "email": "…", "sesion": "3f9a…",
  "butacas": { "C5": "GENERAL", "C6": "JUBILADO" } }
```

`butacas` es código → tarifa (`GENERAL`, `MENOR`, `JUBILADO`, `ESTUDIANTE`), por butaca
porque la tarifa es por persona.

`sesion` es la misma de los bloqueos y hay que mandarla: sin ella el propio bloqueo hace
rebotar la reserva. Es opcional porque la boletería no pasa por elegir. Al confirmar suelta
todos los bloqueos de esa sesión.

Valida R4 (ocupada) y R9 (fuera de servicio). Si el email no existe, da de alta el cliente.
Vuelve con `codigo` (8 caracteres, el del QR) y `entradas[].tarifa`.

> El formato viejo `"codigos": ["C5","C6"]` sigue aceptándose como todas `GENERAL`.

| Ruta | Qué hace |
|---|---|
| `GET /api/reservas/{id}` | Con `funcion`, `pelicula`, `sala`, `cliente`, `total`, `codigo`, `ingresadaEn` |
| `GET /api/reservas?email=` | Las de ese cliente. Sin cliente: `200` con lista vacía, no `404` |
| `GET /api/reservas` | **Todas** — es el listado del encargado |
| `POST /api/reservas/{id}/cancelacion` | R6 libera butacas. R13: solo si está `RESERVADA` |

> `total` es el **subtotal**, a precio de lista. El total definitivo no existe hasta el
> cobro: el descuento depende del medio de pago.

### `POST /api/reservas/{id}/pago`

```json
{ "medio": "CREDITO", "codigoAutorizacion": "AUTH-40219" }
→ { "id": 2, "reservaId": 25, "subtotal": 15360, "promocionId": 1,
    "descuento": 7680, "monto": 7680, "medio": "EFECTIVO" }
```

**El monto no viaja**: lo calcula el backend. Valida R5 (solo `RESERVADA`), R11 (código si
el medio lo exige), R17 (no cobra vencida), y un pago por reserva.

`subtotal` es precio de lista, `descuento` lo que sacó la promoción, `monto` lo que entró
en la caja. El descuento se resuelve acá porque recién ahora se conoce el medio de pago.

`GET /api/reservas/{id}/pago` devuelve eso mismo, o `null` si no se cobró — así pregunta el
front si ya está paga.

### `POST /api/reservas/{id}/checkout`

Medios electrónicos: el código de autorización lo devuelve el procesador, no lo tipea nadie.
**El efectivo no pasa por acá** (`400`).

```json
{ "medio": "QR" }
→ { "id": "MP-1234567890", "reservaId": 25, "medio": "QR", "monto": 7680,
    "urlPago": "https://checkout.emulado.local/mp/MP-1234567890",
    "codigoQr": "MP-QR|MP-1234567890" }
```

`monto` **ya viene con descuento**: es lo que el cliente aprueba en la pantalla del
procesador. `codigoQr` es el contenido del QR, no una imagen.

Valida R5, R17 y R19 acá y no al confirmar: mandar a pagar algo que no se puede cobrar
termina en plata a devolver, y la devolución no existe (R13).

`POST /api/checkouts/{id}/confirmacion` — sin cuerpo, es «el cliente pagó». Devuelve el
mismo pago, ya con `codigoAutorizacion`. Confirmarlo dos veces choca contra R5, así que el
doble click no cobra dos veces.

> La pasarela es una **emulación**: no hay credenciales ni red.

---

# Encargado

### `POST /api/sesion`

`{email, password}` → el empleado sin el hash. **Mismo error** para email inexistente y
contraseña equivocada.

`rol` es `ADMINISTRADOR` o `ACOMODADOR`. El acomodador solo valida entradas en la puerta.

> Hoy la sesión vive en `sessionStorage` y los endpoints de admin **no piden credenciales**.
> Si se agrega token, va en `Authorization`.

## Cartelera y salas

| Ruta | Notas |
|---|---|
| `GET /api/peliculas` | Todas, incluso fuera de cartelera |
| `GET /api/peliculas/pendientes` | El buzón. Va **antes** que `/{id}` en el registro de rutas |
| `POST /api/peliculas` | R1 título único, R2 duración > 0, R7 un género, R10 clasificación. Nace `CONFIRMADA` |
| `POST /api/peliculas/importadas` | Igual, pero nace `PENDIENTE` y fuera de cartelera |
| `POST /api/peliculas/{id}/confirmacion` | Pasa a `CONFIRMADA` y entra en cartelera |
| `POST /api/peliculas/{id}/descarte` | Pasa a `DESCARTADA`. `400` si ya tiene funciones |
| `PUT /api/peliculas/{id}` | Parcial. El título se compara contra **las otras** |
| `DELETE /api/peliculas/{id}` | `400` si tiene funciones o una grilla que la programe |
| `GET /api/salas` · `GET /api/salas/{id}` | El detalle trae `asientos` |
| `POST /api/salas` | `{nombre, tipo, butacasPorFila, codigosVip, codigosPareja, codigosAccesibles, minutosLimpieza}`. Limpieza opcional: 15 por defecto, no negativa |
| `DELETE /api/salas/{id}` | `400` si tiene funciones |
| `PUT /api/salas/{salaId}/asientos/{codigo}` | `{"estado":"FUERA_DE_SERVICIO"}` o `HABILITADO` |
| `GET /api/funciones` | Con `pelicula` y `sala` embebidas |
| `POST /api/funciones` | R3 superposición, R8 3D en sala que no soporta |
| `DELETE /api/funciones/{id}` | `400` si tiene reservas activas |

## Arqueo e informes

`GET /api/arqueo?fecha=2026-08-13` — la caja del día:

```json
{ "fecha": "…", "total": 45450, "entradas": 5,
  "porMedio": { "EFECTIVO": {"cantidad":1,"total":24000} },
  "pagos": [{ "id":1, "reservaId":1, "monto":24000, "medio":"EFECTIVO",
              "pelicula": {}, "cliente": {}, "entradas": 3 }] }
```

Estos dos cortan por **función**, no por día — es lo que pide el INCAA. No se derivan del
arqueo: una función se vende a lo largo de varios días.

`GET /api/funciones/{id}/bordero`

```json
{ "funcionId": 3, "pelicula": "Matrix", "sala": "Sala 1", "espectadores": 15,
  "recaudacionBruta": 67500, "descuentos": 5000, "recaudacionNeta": 62500,
  "porTarifa": { "GENERAL": {"cantidad":12,"total":60000} } }
```

Declara lo **cobrado**: una reserva impaga retiene butacas y no vendió nada. Función sin
ventas es un borderó en cero; función inexistente, `404`.

Bruta es a precio de lista, `descuentos` lo que resignó el cine, neta lo que entró.

`POST /api/funciones/{id}/bordero` — emite el archivo para el INCAA en
`informes/bordero-funcion-<id>.txt` y responde `201`. Es `POST` porque **escribe**: pisa el
anterior, porque se sigue vendiendo hasta que la película arranca.

`GET /api/funciones/{id}/informe` — el borderó completo más candy:

```json
{ "boleteria": { "…": "el borderó entero" }, "comprasCandy": 4, "candy": 12000, "total": 74500 }
```

> **El candy de mostrador no entra**, a propósito: solo el que tiene `reservaId`. Quien
> compra un balde en el mostrador puede ir a cualquiera de las cuatro funciones de las
> 22:00. Esa plata se cuenta en `GET /api/candy/arqueo`. Consecuencia: la suma de los
> informes de un día da **menos** que el arqueo de ese día, y la diferencia es el mostrador.

## Programaciones (CU-03b)

La grilla: *«Matrix en la Sala 1, todos los días a las 20:30, del 1 al 15»*. Una alta en vez
de quince. **Genera funciones de verdad**, no las calcula al vuelo.

`POST /api/programaciones/previsualizar` y `POST /api/programaciones` reciben el mismo
cuerpo y devuelven el mismo informe. El primero no escribe.

```json
{ "peliculaId": 1, "salaId": 1, "desde": "2026-09-07", "hasta": "2026-09-13",
  "horaInicio": "20:30", "diasSemana": [], "idioma": "SUBTITULADA",
  "proyeccion": "DOS_D", "precio": 5000 }
```

```json
{ "programacion": { "id": 1, "…": "…", "activa": true },
  "funciones": [{ "inicio": "2026-09-07T20:30:00", "choca": false },
                { "inicio": "2026-09-09T20:30:00", "choca": true,
                  "motivo": "la sala ya tiene la función 1 a las 09/09 21:00" }],
  "generadas": 6, "salteadas": 1 }
```

- **`diasSemana` vacío no restringe**: corre todos los días.
- ⚠️ Es **`idioma`, no `version`**, aunque el enum del dominio se llame `Version`.
- Al previsualizar, `programacion.id` viene en `0`.
- `motivo` solo viaja si `choca`, y dice contra qué se pisa.
- **Al aplicar se valida de nuevo**, no recibe el informe previsualizado: entre mirar y
  confirmar otro pudo programar algo. El front no debe cachear: repinta con lo que vuelve.

Lo que no depende de la fecha falla ya en `/previsualizar`:

| Entrada | Respuesta |
|---|---|
| `TRES_D` en sala 2D | `400` «La sala Sala 1 no puede proyectar en 3D» |
| `desde` posterior a `hasta` | `400` «El rango tiene que empezar antes de terminar» |
| `horaInicio` mal formada | `400` |
| Rango que no cae en ningún `diasSemana` | `400` |

| Ruta | Notas |
|---|---|
| `GET /api/programaciones` | Todas. **Sin** las funciones que generaron |
| `GET /api/programaciones/{id}` | Con `funciones: [{id, inicio}, …]` |
| `POST /api/programaciones/{id}/baja` y `POST /api/programaciones/{id}/alta` | **No hay `DELETE`**. La baja no toca las funciones ya generadas: pueden tener entradas vendidas |

> La asociación se navega en un solo sentido: `FuncionVista` **no** expone de qué grilla
> salió. Para el camino inverso hay que sumar `programacionId` a esa vista.

## Grilla automática

`POST /api/grilla/propuesta` y `POST /api/grilla` — mismo cuerpo. **`precio` es
obligatorio**; el resto tiene default (una semana desde hoy, de 14 a 24, ocho títulos).

```json
{ "desde": "2026-09-01", "dias": 7, "apertura": "14:00", "cierre": "00:00",
  "cuantasPeliculas": 8, "precio": 5000, "idioma": "SUBTITULADA", "proyeccion": "DOS_D" }
```

El precio no tiene default a propósito: cuánto sale la entrada es una decisión comercial.
`cierre: "00:00"` es el final del día.

```json
{ "elenco": [{ "id": 4, "titulo": "…", "puntaje": 8.2, "duracionMinutos": 166, "pases": 12 }],
  "pases": [{ "peliculaId": 4, "salaId": 1, "inicio": "2026-09-01T14:00:00" }],
  "indicadores": { "minutosProgramados": 3320, "minutosDisponibles": 4200,
                   "ocupacion": 0.79, "puntajePromedio": 7.8,
                   "generosCubiertos": 6, "generosTotales": 9 },
  "funcionesCreadas": 0 }
```

`minutosDisponibles` es la ventana menos lo ya programado, así que `ocupacion` mide el hueco
real. `funcionesCreadas` es `0` en `/propuesta` y la cantidad real en el alta.

Solo entran películas **confirmadas**; si no hay ninguna, `400`. Los pases nunca pisan
funciones ya cargadas.

## Promociones (CU-17)

`POST /api/promociones` — un solo pedido para los tres tipos, con las columnas del beneficio
en `null` salvo la que corresponde.

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

**Las listas vacías no restringen.** Las condiciones se evalúan contra el horario de la
**función**, no contra el momento de la compra.

`GET /api/promociones` y `GET /api/promociones/{id}` traen activas e inactivas.
`POST /api/promociones/{id}/baja` y `POST /api/promociones/{id}/alta` — **no hay `DELETE`**: una promoción usada en un
cobro tiene que seguir existiendo para explicar ese monto.

> **No se acumulan**: gana la que más descuenta (R15), y en empate la de menor id. Las
> tarifas reducidas quedan afuera (R16). Al front le llega resuelto.

## Candy (CU-13 a CU-16)

Implementado en el backend; **todavía sin pantalla**.

| Ruta | Notas |
|---|---|
| `GET /api/candy/productos?todos=` | La carta. Sin `todos=true`, solo lo que está a la venta |
| `GET /api/candy/productos/{id}` | Un producto |
| `POST /api/candy/productos` | `{nombre, tipo, precio}` |
| `POST /api/candy/combos` | `{nombre, precio, componentes}` — `componentes` es `{productoId: cantidad}` |
| `PUT /api/candy/productos/{id}/disponibilidad` | Saca o repone de la carta |
| `POST /api/candy/compras` | La venta |
| `GET /api/candy/compras?fecha=&clienteId=` | Con `clienteId` gana el cliente; si no, el día |
| `GET /api/candy/arqueo?fecha=` | `{fecha, total, compras}` — la otra caja, aparte de boletería |

**Producto**

```json
{ "id": 1, "nombre": "Pochoclos grandes", "tipo": "POCHOCLOS", "precio": 4500,
  "disponible": true, "esCombo": false, "componentes": [] }
```

**Venta** — `cantidades` es `{productoId: cantidad}`:

```json
{ "clienteId": 3, "reservaId": 25, "cantidades": { "1": 2, "4": 1 },
  "medio": "EFECTIVO", "codigoAutorizacion": "" }
→ { "id": 8, "clienteId": 3, "reservaId": 25, "fecha": "…", "medio": "EFECTIVO",
    "items": [], "total": 12000, "ahorro": 1500 }
```

`reservaId` es lo que ata la compra a una función — el «¿desea agregar pochoclos?» de
después de comprar la entrada. **Sin él es venta de mostrador**, y esa no entra en el
informe por función.

`ahorro` es lo que el combo descontó contra comprar los productos sueltos.

## Control de acceso (CU-18)

`POST /api/acceso` con `{ "codigo": "K7M2P9XQ" }` → la reserva completa, con las butacas y
la tarifa de cada una: es lo que el acomodador necesita para saber a quién pedirle carnet.

Va por código y no por id porque el código es lo que trae el QR y es la única credencial del
cliente. **Es `POST` y no `GET`** porque marca la entrada como usada: repetirlo da `400`
(R18), igual que un código inexistente o una reserva impaga.

El código tiene 8 caracteres sin `O`, `I`, `0` ni `1`: se tipea a mano cuando el escáner
no lee.

## Importador

| Ruta | Notas |
|---|---|
| `POST /api/importaciones` | Corre y contesta **cuando terminó**. Cuerpo opcional |
| `GET /api/importaciones` | Las últimas 20 |
| `GET /api/importaciones/estado` | Si puede correr. Va **antes** que el listado en las rutas |

```json
{ "paginas": 2 }
→ { "id": 7, "estado": "TERMINADA", "paginas": 2, "nuevas": 9, "salteadas": 20,
    "fallidas": 1, "detalle": "+ [41] Hablan las aves\n✗ Yo, narciso: La duración…" }
```

`paginas` opcional, de 1 a 3 (ausente es una, veinte títulos). `estado` es `EN_CURSO`,
`TERMINADA` o `FALLIDA`. `detalle` es el log: `+` entró al buzón, `✗` la rechazó una regla.
Las que ya estaban no se nombran.

**Tarda diez o quince segundos** y contesta con el resultado final. nginx tiene
`proxy_read_timeout` de 180s para esta ruta; el backend corta a los 120.

**Que TMDB falle no es error de la API**: responde `201` con la corrida en `FALLIDA` y el
motivo en `detalle`.

Los `400`, con el mensaje que se muestra tal cual:

- `Las páginas a importar van de 1 a 3`
- `Ya hay una importación en curso: esperá a que termine`
- `El importador corrió recién: esperá 60 segundos antes de volver a pedirlo`

`GET /api/importaciones/estado` → `{ "disponible": true, "detalle": "Listo para traer
cartelera" }`. Lo pide la pantalla al abrirse. No le pega a TMDB para contestar.
