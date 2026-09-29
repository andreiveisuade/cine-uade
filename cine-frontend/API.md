# Contrato de la API

Base `/api`. Lo consumen dos clientes: la web del cliente (`src/api/api-http.js`) y el panel
de escritorio del encargado (`cine-swing`, `ClienteHttp` y una `Api<Subdominio>` por subdominio),
que usa los mismos nombres de operación. Se prueba en <http://localhost:8080/swagger-ui.html>.

## Convenciones

| | |
|---|---|
| Formato | JSON en request y response. El cuerpo de un pedido es un objeto |
| Fechas | ISO local sin zona: `2026-08-13T20:30:00`; solo fecha `2026-08-13`; hora `20:30`. Años de 1000 a 9999 |
| Enums | Nombre de la constante (`MAS_16`, `TRES_D`); al entrar no distingue mayúsculas ni espacios alrededor. El front traduce |
| Números | Un campo entero no acepta decimales (`1.9`, y tampoco `1.0`) |
| Precios | Número, con los multiplicadores ya aplicados. Un importe que se carga (el `precio` de una función, programación, grilla o producto; el `monto` de una promoción) es mayor a cero, con hasta 2 decimales y hasta un millón (`El precio no puede superar $ 1000000.00`) |
| Auth | HTTP Basic sin sesión: `Authorization: Basic base64(email:contraseña)` de un empleado en cada pedido |

### Respuestas

| Operación | Status | Cuerpo |
|---|---|---|
| Leer, y previsualizar sin escribir (`/previsualizacion`, `/propuesta`) | `200` | El recurso, o la lista (un filtro sin resultados: `[]`). Ninguna ruta contesta el literal `null` |
| Alta | `201` + `Location` | El recurso creado (`Location: /api/salas/7`). Sin `Location`, porque lo creado no tiene `GET` por id: checkout, grilla automática, importación y venta de candy. El cliente apunta a `/api/clientes?email=…` y el pago a `/api/reservas/{id}/pago` |
| Acción o cambio de estado (revisar una película, cancelar, bloquear, entrar, `PATCH`) | `200` | El recurso como quedó |
| Baja | `204` | Vacío |
| Rechazo | `400` · `404` · `409` | `{"error": "…"}` con el texto del rechazo |
| Inesperado | `500` | `{"error": "…"}` genérico; el detalle, al log del servidor |

### Errores

Todo error es `{"error": "…"}` con `Content-Type: application/json`, aunque el `Accept` pida otro
formato, y el texto se muestra tal cual.

| Status | Cuándo | Texto |
|---|---|---|
| `400` | Dato inválido o regla de negocio incumplida. Lo obligatorio que falta se rechaza antes de buscar nada, el primero en el orden de los campos del pedido; un precio, antes que el resto | `Falta el nombre` · `El título no puede tener más de 100 caracteres` |
| `400` | Un enum, una fecha o un valor de la query que no se entiende | `El medio de pago no es válido: CRIPTO` · `La fecha no es válida: usá AAAA-MM-DD` · `La fecha tiene que estar entre los años 1000 y 9999` · `Una de las fechas del pedido no es válida` · `El id de la película tiene que ser un número` · `El id de la película tiene que estar entre -2147483648 y 2147483647` · `El filtro publicada tiene que ser true o false` |
| `400` | Un cuerpo que no se puede leer: sin cuerpo, JSON roto, una raíz que no es objeto, un tipo equivocado, un decimal en un entero o un número que desborda (estos tres nombran el campo) | `Falta el cuerpo del pedido` · `El cuerpo del pedido no es un JSON válido` · `El cuerpo del pedido tiene que ser un objeto JSON` · `El campo precio tiene un valor inválido: abc` |
| `401` · `403` | Ver *Quién puede llamar a qué* | |
| `404` | Un id o un código que no existe, en la ruta o en el cuerpo (`peliculaId`, `salaId`, `funcionId`, `clienteId`, `reservaId`, un producto) | `No existe la película 99` |
| `404` | Una ruta que no existe, o un id de la ruta que no es un número entero | `No existe la ruta /api/funciones/abc` |
| `405` | Un método que la ruta no acepta; el header `Allow` dice cuáles sí | `La ruta no acepta PUT` |
| `406` | Un `Accept` que no admite JSON | `Esta API responde solo JSON` |
| `409` | Nombre, título o email ya usado: película, sala, cliente (también contra los empleados), producto, promoción | `Ya existe una sala con ese nombre` · `Ya existe un usuario con ese email` |
| `409` | Una butaca que ya es de otro (R4) | `La butaca A1 ya está ocupada` · `Alguien tomó una de esas butacas mientras confirmabas la reserva` |
| `409` | Dos pedidos a la vez: una reserva que otro cambió (`@Version`) o un alta que chocó con una restricción de la base | `La reserva cambió mientras se procesaba: volvé a intentarlo` · `Otro pedido cambió estos datos al mismo tiempo: recargá y volvé a intentarlo` |
| `415` | Un cuerpo que no es JSON | `El cuerpo del pedido tiene que ser JSON` |
| `500` | Falla del servidor. Si falla el archivo de un ticket o un recibo, la venta ya quedó guardada: la respuesta no cambia y el error va al log | `No se pudo acceder a los datos` · `Ocurrió un error inesperado en el servidor` |
| `400` · `404` · `503` · `500` | Lo que no llega a un controller (el firewall rechaza `;`, `//` o `%2e` en la ruta; una excepción de un filtro) o lo que Spring rechaza sin un handler propio: lo contesta `/error` según el status. Otro 4xx dice lo del `400`, otro 5xx lo del `500` | `El pedido no es válido` · `No existe lo que se pidió` · `El servidor no está disponible: volvé a intentarlo en un rato` · `Ocurrió un error inesperado en el servidor` |
| `413` | nginx: un cuerpo de más de 1 MB | `El pedido es demasiado grande: el máximo es 1 MB` |
| `502` | nginx: el backend no está levantado | `El servidor no está disponible: volvé a intentarlo en un rato` |
| `504` | nginx: el backend tarda más de 30 s (180 s en `/api/importaciones`) | `El servidor tardó demasiado en contestar: volvé a intentarlo en un rato` |

### Quién puede llamar a qué

| Nivel | Rutas |
|---|---|
| Público | `POST /api/clientes`, `POST /api/reservas`, `POST /api/funciones/{id}/bloqueos`, `POST /api/reservas/codigo/{codigo}/cancelacion`; `GET` de `/api/cartelera`, `/api/peliculas/{id}` (una pendiente o descartada, solo `ADMINISTRADOR`), `/api/peliculas/{id}/funciones`, `/api/funciones/{id}`, `/api/reservas/codigo/{codigo}`, `/api/reservas?email=` (con email), `/api/candy/productos` y `/{id}`, los nueve catálogos; Swagger (`/swagger-ui/**`, `/v3/api-docs/**`) |
| `ACOMODADOR` o `ADMINISTRADOR` | `POST /api/sesion`, `POST /api/acceso` |
| `ADMINISTRADOR` | Todo lo demás, incluidos `GET /api/reservas` sin email, las rutas de reserva por `{id}`, `GET /api/peliculas/pendientes` y `GET /api/declaracion-jurada`. Una ruta nueva nace así |

`401` sin `WWW-Authenticate`: «Hace falta iniciar sesión para esta operación» (sin header, o con
otro esquema que `Basic`) o, con credenciales inválidas o un `Basic` ilegible (rechazados también en
rutas públicas), «Email o contraseña incorrectos». `403`: «Tu rol no tiene permiso para esta
operación».

## Catálogos

| Ruta | Devuelve |
|---|---|
| `GET /api/generos` | `["ACCION", "COMEDIA", …]` |
| `GET /api/clasificaciones` | `[{"nombre":"ATP","edadMinima":0}, …]` |
| `GET /api/tipos-sala` | `[{"nombre":"IMAX","multiplicador":1.6,"soportaTresD":true}, …]`: `DOS_D`, `TRES_D`, `IMAX`, `CUATRO_D` |
| `GET /api/idiomas` | `["DOBLADA","SUBTITULADA"]` |
| `GET /api/proyecciones` | `["DOS_D","TRES_D"]` |
| `GET /api/medios-pago` | `[{"nombre":"EFECTIVO","requiereAutorizacion":false}, …]` |
| `GET /api/tarifas` | `[{"nombre":"JUBILADO","multiplicador":0.5,"requiereAcreditacion":true}, …]` |
| `GET /api/tipos-producto` | `[{"nombre":"POCHOCLOS","esCombo":false}, {"nombre":"BEBIDA","esCombo":false}, {"nombre":"GOLOSINA","esCombo":false}, {"nombre":"COMBO","esCombo":true}]`. Los que no son combo se dan de alta en `POST /api/candy/productos`; el combo, en `POST /api/candy/combos` |
| `GET /api/tipos-promocion` | `[{"nombre":"PORCENTAJE","campos":["porcentaje"]}, {"nombre":"MONTO_FIJO","campos":["monto"]}, {"nombre":"NXM","campos":["lleva","paga"]}]`: los campos de `POST /api/promociones` que pide cada tipo, además de nombre y condiciones |

La web del cliente anticipa con ellos el «traé el carnet» (`requiereAcreditacion`). Swing usa
`requiereAcreditacion` para el «pedir carnet» de Puerta y del cobro, `requiereAutorizacion` para elegir
entre cobro en caja y checkout (que el código haga falta, R11, lo sigue rechazando el backend) y el
`multiplicador` de cada tipo de sala. R8 (`soportaTresD`) no se anticipa: la valida el backend al dar
de alta la función. Ningún cliente decide por el nombre de una constante lo que dice un catálogo.

---

# Cliente

| Ruta | Qué hace |
|---|---|
| `GET /api/cartelera?genero=` | Solo en exhibición. `genero` opcional |
| `GET /api/peliculas/{id}` | Una película. Una pendiente o descartada da `404` sin sesión de `ADMINISTRADOR` |
| `GET /api/peliculas/{id}/funciones` | Sus funciones por `inicio`, con la sala embebida |
| `GET /api/clientes?email=` | Siempre una lista: `[cliente]` o `[]` (también sin email). Sin distinguir mayúsculas ni espacios alrededor. Solo `ADMINISTRADOR` (lo usa Swing) |
| `POST /api/clientes` | `{nombre, email}`. Email con forma `usuario@dominio.algo`, guardado en minúsculas y sin espacios; único también contra los empleados (`409`). Opcional: reservar da de alta igual |

**Película**

```json
{ "id": 1, "titulo": "Matrix", "duracionMinutos": 136, "generos": ["ACCION"],
  "clasificacion": "MAS_16", "posterUrl": "…", "director": "…", "anio": 1999,
  "idiomaOriginal": "Inglés", "sinopsis": "…",
  "enCartelera": true, "estadoRevision": "CONFIRMADA", "puntaje": 8.2, "votos": 26000 }
```

`estadoRevision` (`PENDIENTE`/`CONFIRMADA`/`DESCARTADA`): si entró al catálogo; lo importado
nace `PENDIENTE` y no se programa. `enCartelera`: si se está dando.

**Función**: suma `precioDesde` (precio × multiplicador de sala) y la sala con `minutosLimpieza`,
que cuenta para R3.

## Mapa de butacas

`GET /api/funciones/{id}?sesion=…`: la función con todas las butacas.

```json
{ "id": 1, "…": "…", "libres": 50,
  "asientos": [{ "id": 5, "salaId": 1, "fila": 1, "numero": 5, "codigo": "A5", "tipo": "ESTANDAR",
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
- Perder una butaca es `200` con `rechazadas`, no `409`. Es `400` una butaca inexistente («La butaca Z9 no existe en la sala») o sin código, una fuera de servicio (R9, «La butaca A3 está fuera de servicio»), más de 10 («Una compra tiene que tener como máximo 10 butacas») o la función ya empezada (R19, «La función ya empezó: no se pueden reservar butacas»).
- `sesion` = `crypto.randomUUID()` en `sessionStorage`; no es credencial.
- `sesion` obligatoria («Falta la sesión para bloquear butacas») y de hasta 64 caracteres; se guarda sin los espacios de las puntas.

## Reserva y pago

`POST /api/reservas`

```json
{ "funcionId": 1, "nombre": "…", "email": "…", "sesion": "3f9a…",
  "butacas": { "C5": "GENERAL", "C6": "JUBILADO" } }
```

- `butacas`: código → tarifa (`GENERAL`, `MENOR`, `JUBILADO`, `ESTUDIANTE`), de 1 a 10. Una tarifa en `null` o en blanco es `GENERAL`; una que no existe, `400` «La tarifa de la butaca B4 no es válida: VIP». La misma butaca dos veces (`a1` y `A1`): `400` «La butaca A1 está repetida en el pedido».
- `sesion`: la de los bloqueos (sin ella tu bloqueo la rechaza; opcional en boletería). Al confirmar los suelta.
- Primero la función: inexistente `404`, empezada `400` (R19). Después el cliente: alta si el email es nuevo; el de un empleado es `400` «Ese email es de un empleado del cine: usá otro para comprar». R9 `400`; R4 `409` si otra compra tiene la butaca. Devuelve `codigo` (QR) y `entradas[].tarifa`.

Toda reserva (`POST`, `GET` por id, por código o en listados) trae además `"cobrable": true|false` y
`"cancelable": true|false`. Los calcula `Reserva` con el mismo método que usa el gestor al cobrar y al
cancelar, así que un `true` no termina en `400` por esas reglas: `cobrable` es `RESERVADA`, no vencida
(R17) y con la función sin empezar (R19); `cancelable` es `RESERVADA` (R13). El medio de pago y el
código de autorización (R11) se siguen validando al cobrar.

| Ruta | Qué hace |
|---|---|
| `GET /api/reservas/codigo/{codigo}` | El ticket del cliente: con `funcion`, `pelicula`, `sala`, `cliente`, `total` (subtotal de lista), `codigo`, `ingresadaEn`. Sin distinguir mayúsculas; inexistente: `404` «No existe ninguna reserva con ese código» |
| `POST /api/reservas/codigo/{codigo}/cancelacion` | El cliente cancela la suya. R6 libera butacas. R13: solo si está `RESERVADA` («La reserva está pagada: solo se puede cancelar una reserva sin cobrar») |
| `GET /api/reservas?email=` | Las de ese cliente, **sin `codigo`**: el email no prueba ser el dueño. Sin distinguir mayúsculas ni espacios alrededor. Sin cliente: `200` con `[]` |
| `GET /api/reservas/{id}`, `POST /api/reservas/{id}/cancelacion` | Lo mismo por id, solo `ADMINISTRADOR`: el id es secuencial y se adivina |
| `GET /api/reservas?estado=&dia=&q=` | Todas (listado del encargado), de la más nueva a la más vieja. Filtros opcionales: `estado` (`RESERVADA`…), `dia` (`AAAA-MM-DD`, día de la función) y `q` (texto en código, butaca, cliente, email o película) |

`POST /api/reservas/{id}/pago`

```json
{ "medio": "CREDITO", "codigoAutorizacion": "AUTH-40219" }
→ { "id": 2, "reservaId": 25, "subtotal": 15360, "promocionId": 1,
    "descuento": 7680, "monto": 7680, "medio": "CREDITO",
    "fecha": "2026-08-13T19:40:00", "codigoAutorizacion": "AUTH-40219" }
```

- El monto no viaja: el descuento depende del medio. `monto` es lo que entra en caja.
- R5 (solo `RESERVADA`: «La reserva está pagada: no se puede cobrar»), R11 (código si el medio lo exige, hasta 50 caracteres; el efectivo no lleva: «El pago en efectivo no lleva código de autorización»), R17 (no cobra vencida), R19 («La función ya empezó: no se puede cobrar la reserva 25»), un pago por reserva.
- Cobrar y cancelar la misma reserva a la vez: gana el primero y el otro recibe `409` (`@Version` en `Reserva`). Reintentarlo da la respuesta de siempre: `400` si la reserva ya no está `RESERVADA`.
- `GET /api/reservas/{id}/pago`: lo mismo, o `404` «La reserva 25 todavía no tiene un pago» si no se cobró (una reserva que no existe dice «No existe la reserva 25»).

`POST /api/reservas/{id}/checkout`: medios electrónicos, el código lo da el procesador. Efectivo: `400` «El pago con efectivo no va por checkout: se cobra en la caja del cine».

```json
{ "medio": "QR" }
→ { "id": "MP-1234567890", "reservaId": 25, "medio": "QR", "monto": 7680,
    "urlPago": "https://checkout.emulado.local/mp/MP-1234567890",
    "codigoQr": "MP-QR|MP-1234567890" }
```

- `monto` con descuento; `codigoQr` es texto. R5, R17 y R19 se validan acá (no hay devolución, R13).
- `POST /api/checkouts/{id}/confirmacion`, sin cuerpo: `201` con el pago y su `codigoAutorizacion`. Revalida la reserva antes de autorizar; repetirlo choca con R5. Inexistente: `404` «No existe el checkout MP-0000000000».
- Pasarela emulada, sin red.

---

# Encargado

`POST /api/sesion`, con `Authorization: Basic base64(email:contraseña)` y sin cuerpo → `200` con
el empleado sin hash (`{id, nombre, email, rol}`, `rol` `ADMINISTRADOR` o `ACOMODADOR`). Las
credenciales las verifica el mismo filtro que en cualquier otra ruta: el login solo confirma que
son válidas y dice de quién son. Un cuerpo que llegue igual se ignora. El email no distingue
mayúsculas ni espacios alrededor. Clave mala o email inexistente: `401` «Email o contraseña
incorrectos», el mismo para los dos. Sin header: `401` «Hace falta iniciar sesión para esta
operación», aunque el cuerpo traiga `{email, password}`. No hay token: tras el `200`, quien llama
guarda `email:contraseña` y lo manda en cada pedido.

## Cartelera y salas

| Ruta | Notas |
|---|---|
| `GET /api/peliculas?q=&genero=&publicada=` | Todas, incluso fuera de cartelera. Filtros opcionales: `q` (texto en el título), `genero` y `publicada` (`true`/`false`, el flag `enCartelera`) |
| `GET /api/peliculas/pendientes` | El buzón. Va antes que `/{id}` en las rutas |
| `POST /api/peliculas` | R1 título único, sin distinguir mayúsculas ni espacios alrededor (`409`), R2 duración de 1 a 600 minutos, R7 al menos un género, R10 clasificación. `puntaje` de 0 a 10 con un decimal, `votos` de 0 a 100.000.000, `anio` entre 1895 y cinco años por delante (`0` = sin dato), `sinopsis` hasta 5000 caracteres, `posterUrl` vacía o con `http://` o `https://`. Nace `CONFIRMADA` |
| `POST /api/peliculas/{id}/confirmacion` | `CONFIRMADA` y en cartelera |
| `POST /api/peliculas/{id}/descarte` | `DESCARTADA`. `400` si tiene funciones |
| `PUT /api/peliculas/{id}` | Parcial: nada obligatorio, lo que no viaja queda igual; lo que viaja se valida como en el alta (`400`, y no se guarda nada). Título único contra las otras (`409`). `enCartelera: true` publica solo una `CONFIRMADA`: «La película Dune no está confirmada: revisala antes de publicarla» o «La película Dune está descartada: no se puede publicar» |
| `DELETE /api/peliculas/{id}` | `400` si tiene funciones o una grilla que la programe |
| `GET /api/salas` · `GET /api/salas/{id}` | `{id, nombre, tipo, butacasPorFila, filas, capacidadSala, minutosLimpieza}`; el detalle trae además `asientos` |
| `POST /api/salas` | `{nombre, tipo, butacasPorFila, codigosVip, codigosPareja, codigosAccesibles, minutosLimpieza}`. De 1 a 26 filas y de 1 a 40 butacas por fila. Códigos especiales sin distinguir mayúsculas ni espacios; uno que no está en la sala («La butaca Z99 no existe en la sala») o que está en dos listas («La butaca A1 está en más de una lista de especiales: dejala en una sola») es `400`. Limpieza opcional, 15 por defecto, de 0 a 120. Nombre único, sin distinguir mayúsculas ni espacios alrededor (`409`) |
| `PUT /api/salas/{id}` | `{nombre, tipo, minutosLimpieza}`. Butacas no editables; sin limpieza conserva la anterior; tipo fijo si tiene funciones (`400`). Los datos se validan antes que el nombre repetido (`409`); renombrarse a sí misma no choca |
| `DELETE /api/salas/{id}` | `400` si tiene funciones o está en una grilla |
| `PATCH /api/salas/{salaId}/asientos/{codigo}` | `{"estado":"FUERA_DE_SERVICIO"}` o `HABILITADO` (R9). Devuelve la sala con sus butacas; una butaca que no está: `404` «No existe la butaca A99» |
| `GET /api/funciones?peliculaId=&salaId=&desde=&hasta=` | Con `pelicula` y `sala` embebidas, por `inicio`. Filtros opcionales; `desde` y `hasta` son días (`AAAA-MM-DD`) y `hasta` incluye todo ese día. `desde` posterior a `hasta`: `400` «El período tiene que empezar antes de terminar» |
| `POST /api/funciones` | Película confirmada, R3 superposición, R8 3D en sala que no soporta, R20 `400` «La función no puede empezar en el pasado» si `inicio` no es posterior al momento actual (ahora mismo ya cuenta como pasado, igual que en R19). `inicio` sin segundos («La hora de la función tiene que ir sin segundos») y dentro del próximo año |
| `DELETE /api/funciones/{id}` | `400` si tiene reservas, aun canceladas (R12: son historial) |

## Arqueo e informes

`GET /api/arqueo?fecha=2026-08-13`: la caja del día (`fecha` obligatoria).

```json
{ "fecha": "…", "total": 45450, "entradas": 5,
  "porMedio": { "EFECTIVO": {"cantidad":1,"total":24000} },
  "pagos": [{ "id":1, "reservaId":1, "monto":24000, "medio":"EFECTIVO",
              "pelicula": {}, "cliente": {}, "entradas": 3 }] }
```

Borderó e informe cortan por **función** (INCAA), no por día; la declaración jurada, por período.

`GET /api/funciones/{id}/bordero`

```json
{ "funcionId": 3, "pelicula": "Matrix", "sala": "Sala 1", "funcion": "2026-08-13T20:30:00",
  "generadoEn": "2026-08-14T09:00:00", "espectadores": 15,
  "recaudacionBruta": 67500, "descuentos": 5000, "recaudacionNeta": 62500,
  "porTarifa": { "GENERAL": {"cantidad":12,"total":60000} } }
```

- Solo lo **cobrado**; sin ventas da cero. Bruta a lista, neta lo que entró.
- El archivo del borderó para el INCAA no lo escribe el backend: lo emite el cliente de escritorio del encargado (módulo `cine-swing`) con este `GET`.
- `GET /api/funciones/{id}/informe`: `{ "boleteria": {borderó}, "comprasCandy": 4, "candy": 12000, "total": 74500 }`. Solo candy con `reservaId`: el de mostrador está en `GET /api/candy/arqueo`.

### Declaración jurada del período

`GET /api/declaracion-jurada?desde=2026-08-20&hasta=2026-08-26`, solo `ADMINISTRADOR`.

- Sin `desde` ni `hasta`: la semana cinematográfica anterior (jueves a miércoles) a la de hoy.
- `400` si viene una sola fecha, si una no es `AAAA-MM-DD`, si `desde` es posterior a `hasta` o si el período pasa de 31 días.
- Entran las funciones cuyo **inicio** cae en el período y que tienen entradas cobradas; cada una trae las cifras de su borderó.

```json
{ "exhibidor": {"razonSocial":"Cine UADE S.A.","cuit":"30-71234567-1","numeroExhibidor":"10452"},
  "desde":"2026-08-20", "hasta":"2026-08-26", "generadaEn":"2026-08-27T10:00:00",
  "funciones":[{"funcionId":1,"inicio":"2026-08-20T20:00:00","sala":"Sala 1","pelicula":"Matrix",
                "clasificacion":"MAS_13","idioma":"SUBTITULADA","proyeccion":"DOS_D",
                "espectadores":2,"porTarifa":{"GENERAL":{"cantidad":1,"total":5000},"JUBILADO":{"cantidad":1,"total":2500}},
                "recaudacionBruta":7500,"descuentos":0,"recaudacionNeta":7500}],
  "peliculas":[{"titulo":"Matrix","clasificacion":"MAS_13","funciones":1,"espectadores":2,
                "entradasPorTarifa":{"GENERAL":1,"JUBILADO":1},
                "recaudacionBruta":7500,"descuentos":0,"recaudacionNeta":7500}],
  "total":{"funciones":1,"espectadores":2,"entradasPorTarifa":{"GENERAL":1,"JUBILADO":1},
           "recaudacionBruta":7500,"descuentos":0,"recaudacionNeta":7500} }
```

- `porTarifa` tiene la forma del borderó; `porTarifa` y `entradasPorTarifa` traen solo las tarifas con venta.
- `peliculas` va ordenado por título; `exhibidor` sale de `cine.incaa.*` (`INCAA_RAZON_SOCIAL`, `INCAA_CUIT`, `INCAA_NUMERO_EXHIBIDOR`).
- El CSV que se sube al INCAA no lo genera el backend: lo arma el cliente de escritorio del encargado
  con este JSON (registros etiquetados `EXHIBIDOR`/`FUNCION`/`PELICULA`/`TOTAL`/`DECLARACION`, `;`, UTF-8 con BOM; ver el módulo `cine-swing`).

## Programaciones (CU-03b)

Genera funciones reales. `POST /api/programaciones/previsualizacion` (no escribe) y `POST /api/programaciones`, mismo contrato:

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
- `hasta` opcional: sin él la grilla queda abierta y se extiende sola, de a 14 días.
- Aplicar revalida: repintar con la respuesta.
- R20: los pases que ya pasaron (por ejemplo, hoy a una hora vencida) se saltean: no aparecen en `funciones` ni cuentan en `salteadas`, en la previsualización ni al crear. Tampoco se generan al extender una grilla abierta.
- `400` ya al previsualizar: película sin confirmar, 3D en sala 2D (R8), `desde` > `hasta`, un rango de más de 366 días («El rango no puede cubrir más de 366 días») o que termina a más de un año («El rango tiene que terminar dentro del próximo año»), `horaInicio` mal formada o con segundos, un día en `null`, rango sin ningún `diasSemana`, rango cerrado que ya pasó entero (R20: «Todos los horarios del rango ya pasaron: la grilla no generaría funciones»).
- Orden: primero el precio y el formato de cada campo; después la película y la sala (`404`) y la película sin confirmar; al final el rango, la hora, los días, R8 y R20.

| Ruta | Notas |
|---|---|
| `GET /api/programaciones?peliculaId=&salaId=&activa=` | Todas, sin sus funciones. Filtros opcionales |
| `GET /api/programaciones/{id}` | Con `funciones: [{id, inicio}, …]` |
| `PATCH /api/programaciones/{id}` | `{"activa": false}` la da de baja, `true` la reactiva (sin `activa`: `400`). No hay `DELETE`. La baja no toca las funciones generadas |

## Grilla automática

`POST /api/grilla/propuesta` (no crea) y `POST /api/grilla`, mismo cuerpo. Solo `precio` es
obligatorio; default: una semana desde hoy, 14 a 24, ocho títulos, subtitulada en 2D. Un `idioma`
o una `proyeccion` vacíos toman el default.

```json
{ "desde": "2026-09-01", "dias": 7, "apertura": "14:00", "cierre": "00:00",
  "cuantasPeliculas": 8, "precio": 5000, "idioma": "SUBTITULADA", "proyeccion": "DOS_D" }
→ { "elenco": [{ "id": 4, "titulo": "…", "puntaje": 8.2, "duracionMinutos": 166,
                 "generos": ["DRAMA"], "pases": 12 }],
    "pases": [{ "peliculaId": 4, "titulo": "…", "salaId": 1, "sala": "Sala 1",
                "inicio": "2026-09-01T14:00:00", "duracionMinutos": 166 }],
    "indicadores": { "minutosProgramados": 3320, "minutosDisponibles": 4200,
                     "ocupacion": 0.79, "puntajePromedio": 7.8,
                     "generosCubiertos": 6, "generosTotales": 9,
                     "pasesPorGenero": { "DRAMA": 12 } },
    "funcionesCreadas": 0 }
```

`cierre: "00:00"` = fin del día. `minutosDisponibles` descuenta lo ya programado y, en un plan que arranca hoy, cuenta solo desde el primer pase posible (R20); `ocupacion` se mide sobre eso. Solo películas
confirmadas (ninguna: `400`); no pisa funciones existentes ni propone pases que ya pasaron (R20: hoy arranca en el primer intento de media hora posterior a ahora). En 3D reparte y mide solo en las salas que lo proyectan (ninguna: `400` «No hay salas que puedan proyectar en 3D»).

`400` también con `desde` anterior a hoy («La grilla no puede empezar en el pasado») o un fin a más
de un año, `dias` fuera de 1 a 31, `cuantasPeliculas` fuera de 1 a 20, `apertura` con segundos o un
`cierre` que no es posterior a la apertura. El precio se valida primero, y el formato de cada campo
antes que los criterios.

## Promociones (CU-17)

`POST /api/promociones`: `nombre`, `tipo` y las dos puntas de la vigencia son obligatorios; los
campos de beneficio que no aplican van en `null`.

```json
{ "nombre": "Miércoles 2x1", "tipo": "NXM", "lleva": 2, "paga": 1,
  "vigenciaDesde": "2026-08-01", "vigenciaHasta": "2026-12-31",
  "diasSemana": ["WEDNESDAY"], "horaDesde": null, "horaHasta": null, "mediosPago": [] }
```

| `tipo` | Campos | Ejemplo |
|---|---|---|
| `PORCENTAJE` | `porcentaje` (1 a 99, hasta 2 decimales) | 30% off |
| `MONTO_FIJO` | `monto` (como un precio) | $2000 off |
| `NXM` | `lleva` > `paga` ≥ 1; `lleva` hasta 10, el tope de butacas por compra | 2x1 |

- Listas vacías no restringen; se evalúa contra el horario de la **función**. Una hora vacía cuenta como no enviada: sin franja. La franja tiene que empezar antes de terminar (no cruza la medianoche).
- `400` si ya venció («La vigencia ya terminó: el fin tiene que ser hoy o después») o si ninguno de sus `diasSemana` cae en lo que le queda de vigencia. Lo común (nombre, vigencia, franja) se valida antes que lo propio del tipo; un `tipo` que no existe: «El tipo de promoción tiene que ser porcentaje, monto fijo o NxM».
- `GET /api/promociones` y `GET /api/promociones/{id}`: activas e inactivas. `PATCH /api/promociones/{id}` con `{"activa": false}` la da de baja y con `true` la reactiva (sin `activa`: `400`); sin `DELETE`.
- R15: no se acumulan, gana el mayor descuento (empate: menor id). R16: tarifas reducidas afuera.

## Candy (CU-13 a CU-16)

| Ruta | Notas |
|---|---|
| `GET /api/candy/productos?todos=` | La carta; sin `todos=true`, solo lo disponible |
| `GET /api/candy/productos/{id}` | Un producto |
| `POST /api/candy/productos` | `{nombre, tipo, precio}`. Un `COMBO` va por `/combos` (`400`). Nombre único, sin distinguir mayúsculas ni espacios alrededor (`409`) |
| `POST /api/candy/combos` | `{nombre, precio, componentes: {productoId: cantidad}}`: dos productos distintos o más, ninguno combo, de 1 a 20 de cada uno. R14: más barato que sus componentes sueltos |
| `PUT /api/candy/productos/{id}` | `{nombre, precio}`; tipo fijo. R14: `400` si un combo afectado deja de ser más barato que sus componentes |
| `PATCH /api/candy/productos/{id}` | `{disponible}`. Sin `DELETE`: vive en compras viejas |
| `POST /api/candy/compras` | La venta, que nace cobrada |
| `GET /api/candy/compras?fecha=&clienteId=` | Con `clienteId` gana el cliente; si no, el día (`fecha` obligatoria) |
| `GET /api/candy/arqueo?fecha=` | `{fecha, total, compras}`, caja aparte de boletería |

```json
{ "id": 1, "nombre": "Pochoclos grandes", "tipo": "POCHOCLOS", "precio": 4500,
  "disponible": true, "esCombo": false, "componentes": [] }
```

```json
{ "clienteId": 3, "reservaId": 25, "cantidades": { "1": 2, "4": 1 },
  "medio": "EFECTIVO", "codigoAutorizacion": "" }
→ { "id": 8, "clienteId": 3, "reservaId": 25, "fecha": "…", "medio": "EFECTIVO",
    "codigoAutorizacion": "",
    "items": [{ "productoId": 4, "nombre": "Combo clásico", "cantidad": 1,
                "precioUnitario": 5500, "subtotal": 5500 }],
    "total": 12000, "ahorro": 1500 }
```

Sin `reservaId` es venta de mostrador. Con `reservaId`, la reserva tiene que estar pagada («La
reserva 25 no está pagada: cobrala antes de agregarle candy») y un `clienteId` que venga, ser el de
la reserva («La reserva 25 es de otro cliente: revisá la reserva o el cliente»). Cada cantidad, de 1
a 20 («La cantidad de Pochoclos grandes tiene que ser como máximo 20»); lo que se sacó de la carta
no se vende. El código de autorización, como en el cobro de una reserva (R11). `ahorro`: descuento
de los combos, congelado al vender (editar el combo después no cambia una compra vieja).

## Control de acceso (CU-18)

`POST /api/acceso` `{ "codigo": "K7M2P9XQ" }` → la reserva con butacas y tarifas. Marca la
entrada usada. `400`: impaga (R18, «La reserva está sin pagar: solo se ingresa con una reserva
pagada»), ya usada («Esa entrada ya se usó el 20/08 19:42»), otro día que el de la función («La
función es el 20/08: se entra solo ese día»), sin `codigo` o en blanco. Inexistente: `404`. 8
caracteres sin `O`, `I`, `0`, `1`, sin distinguir mayúsculas.

## Importador

| Ruta | Notas |
|---|---|
| `POST /api/importaciones` | Corre y contesta al terminar (10-15 s). Cuerpo opcional |
| `GET /api/importaciones` | Las últimas 20 |
| `GET /api/importaciones/estado` | `{ "disponible": true, "detalle": "Listo para traer cartelera" }`. No consulta TMDB. Va antes que el listado |

```json
{ "paginas": 2 }
→ { "id": 7, "estado": "TERMINADA", "paginas": 2, "pedidaEn": "2026-08-14T10:00:00",
    "terminoEn": "2026-08-14T10:00:12", "nuevas": 9, "salteadas": 20,
    "fallidas": 1, "detalle": "+ [41] Hablan las aves\n✗ Yo, narciso: La duración…" }
```

- `paginas` 1 a 3 (default 1, veinte títulos). `estado`: `EN_CURSO`, `TERMINADA`, `FALLIDA`. `detalle`: `+` al buzón, `✗` rechazada.
- Una candidata que no se puede guardar cuenta en `fallidas` y la corrida sigue; si fue un error de la base, su línea dice `No se pudo guardar: el motivo quedó en el log del servidor`. Una corrida que sigue `EN_CURSO` a los 5 minutos pasa a `FALLIDA` y deja libre el importador.
- TMDB caído: `201` con `FALLIDA` y motivo en `detalle`.
- `400`: `Las páginas a importar tienen que estar entre 1 y 3` · `Ya hay una importación en curso: esperá a que termine` · `El importador corrió recién: esperá 60 segundos antes de volver a pedirlo`.
