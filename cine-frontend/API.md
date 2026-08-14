# Contrato de la API

Lo que el frontend espera del backend. Cada función de `js/api.js` corresponde a un
endpoint de esta lista. `FUENTE = "http"` en `js/api.js` es el modo actual: `js/api-http.js`
llama a estos endpoints con `fetch`. `js/api-mock.js` (modo `"mock"`) sigue existiendo para
trabajar sin backend, y replica el mismo contrato contra datos en memoria.

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
             "butacasPorFila": [8,10,12,12,14], "filas": 5, "capacidadSala": 56,
             "minutosLimpieza": 15 },
   "precioDesde": 8000 }]
```

`precioDesde` es `precio × multiplicador de sala`: la butaca más barata.

`minutosLimpieza` es cuánto tarda esa sala en levantarse entre dos funciones. Cuenta para
R3: una función no puede empezar hasta que termine la anterior **más** esos minutos, así
que programar a las 22:00 algo que termina a las 22:00 da 400. La agenda del admin lo
dibuja como una franja rayada debajo de cada bloque.

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

### `POST /api/clientes`
`{ "nombre": "Andrei Veis", "email": "andrei@uade.edu.ar" }` → el cliente creado (CU-05).
El email es único: si ya existe, 400 con "Ya hay un cliente registrado con ese email".
Registrarse es opcional — reservar da de alta al cliente igual.

### `GET /api/clientes?email=andrei@uade.edu.ar`
El cliente con ese email, o `null` si no existe. Sin distinguir mayúsculas.

### `POST /api/reservas`
```json
{ "funcionId": 1, "nombre": "Andrei Veis", "email": "andrei@uade.edu.ar",
  "butacas": { "C5": "GENERAL", "C6": "JUBILADO" } }
```
`butacas` es código de butaca a **tarifa** de quien la ocupa: `GENERAL`, `MENOR`,
`JUBILADO` o `ESTUDIANTE`. Va por butaca y no por reserva porque la tarifa es por
persona — en una reserva de cuatro puede haber dos generales, un menor y un jubilado.

> El formato viejo `"codigos": ["C5","C6"]` **sigue aceptándose** y equivale a todas
> `GENERAL`. Si vienen los dos, gana `butacas`.

Como la clave es el código de butaca, pedir la misma dos veces es imposible de
expresar: dejó de ser un error a validar. Si el email no existe, da de alta el cliente.
Valida R4 (butaca ocupada) y R9 (fuera de servicio).

La reserva vuelve con `codigo` —el del QR, 8 caracteres— y `entradas[].tarifa`.

### `GET /api/reservas/{id}`
Todo lo que necesita el ticket: la reserva más `funcion`, `pelicula`, `sala`, `cliente`
y `total`. Incluye además `codigo` e `ingresadaEn` (ausente si todavía no entraron).

> `total` es el **subtotal**: la suma de los precios de lista. El total definitivo no
> existe hasta que se cobra, porque el descuento por promoción depende del medio de pago.

### `GET /api/reservas?email=andrei@uade.edu.ar`
Las reservas de ese cliente (CU-09). El cliente no inicia sesión, así que el email es lo
único con lo que puede recuperarlas. Mismos campos que el listado del encargado, más
`pago`. Comparación de email sin distinguir mayúsculas. Si no hay cliente con ese email,
`200` con lista vacía — no `404`.

> Sin el parámetro `email`, la misma ruta devuelve **todas** las reservas: es el listado
> del encargado. Cuando haya autenticación, esa versión debería quedar detrás del token.

## Encargado

### `POST /api/sesion`
`{ "email": "…", "password": "…" }` → el empleado sin el hash.
**El mismo error para email inexistente y contraseña equivocada**, como
`GestorEmpleados`.

`rol` viene en la respuesta y puede ser `ADMINISTRADOR` o `ACOMODADOR`. El acomodador
solo valida entradas en la puerta: no administra la cartelera, así que el front debería
llevarlo directo a esa pantalla y no al panel.

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
| POST | `/api/salas` | `{nombre, tipo, butacasPorFila, codigosVip, codigosPareja, codigosAccesibles, minutosLimpieza}`. `minutosLimpieza` es opcional: ausente son 15, y no puede ser negativo |
| DELETE | `/api/salas/{id}` | 400 si tiene funciones |
| PUT | `/api/salas/{salaId}/asientos/{codigo}` | `{"estado":"FUERA_DE_SERVICIO"}` o `HABILITADO` |
| GET | `/api/funciones` | Todas, con `pelicula` y `sala` embebidas |
| POST | `/api/funciones` | R3 superposición, R8 3D en sala que no soporta |
| DELETE | `/api/funciones/{id}` | 400 si tiene reservas activas |
| GET | `/api/reservas` | Con `funcion`, `pelicula`, `sala`, `cliente`, `total` y `pago` (o `null`) |
| POST | `/api/reservas/{id}/cancelacion` | R6: libera las butacas. R13: solo si está `RESERVADA` |

### `POST /api/reservas/{id}/pago`
```json
{ "medio": "CREDITO", "codigoAutorizacion": "AUTH-40219" }
```
**El monto no viaja**: lo calcula el backend. R5 (solo `RESERVADA`), R11 (código
obligatorio si el medio lo exige), R17 (no se cobra una reserva vencida) y un pago por
reserva.

Devuelve el desglose completo:
```json
{ "id": 2, "reservaId": 25, "subtotal": 15360, "promocionId": 1,
  "descuento": 7680, "monto": 7680, "medio": "EFECTIVO", … }
```
`subtotal` es la suma de los precios de lista; `descuento` lo que sacó la promoción que
ganó; `monto` lo que entró en la caja, que es lo que suma el arqueo. **Acá se resuelve el
descuento y no antes**: recién en este momento se conoce el medio de pago, y hay
promociones que dependen de él.

### `GET /api/reservas/{id}/pago`
El pago de esa reserva, o `null` si todavía no se cobró — no es error, es la forma en que
el front pregunta si ya está paga. Mismos campos que el `pago` embebido en `GET /api/reservas`.

### `GET /api/arqueo?fecha=2026-08-13`
```json
{ "fecha": "2026-08-13", "total": 45450, "entradas": 5,
  "porMedio": { "EFECTIVO": {"cantidad":1,"total":24000} },
  "pagos": [{ "id":1, "reservaId":1, "monto":24000, "medio":"EFECTIVO",
              "fecha":"2026-08-13T14:22:00", "codigoAutorizacion":"",
              "pelicula": {…}, "cliente": {…}, "entradas": 3 }] }
```

## Programaciones (CU-03b)

La grilla: *«Matrix en la Sala 1, todos los días a las 20:30, del 1 al 15 de septiembre»*.
Una sola alta en vez de quince. **Genera funciones de verdad**, no las calcula al vuelo:
una función tiene reservas, se cancela y se muda de sala, y nada de eso lo sabe la grilla.

### `POST /api/programaciones/previsualizar` y `POST /api/programaciones`

Los dos reciben **exactamente el mismo cuerpo** y devuelven **exactamente el mismo
informe**. El primero no escribe nada; el segundo guarda la grilla y las funciones que
entran. Esa simetría es el contrato: lo que se ve antes de confirmar es lo que se va a
guardar.

```json
{ "peliculaId": 1, "salaId": 1,
  "desde": "2026-09-07", "hasta": "2026-09-13", "horaInicio": "20:30",
  "diasSemana": [], "idioma": "SUBTITULADA", "proyeccion": "DOS_D", "precio": 5000 }
```

**`diasSemana` vacío no restringe**: corre todos los días del rango, no ninguno. Mismo
criterio que en promociones.

⚠️ **`idioma`, no `version`.** El enum del dominio se llama `Version`, pero toda la API le
dice `idioma` —igual que `POST /api/funciones`— porque «versión» en una interfaz se lee
como versión del software. Y el endpoint es `/previsualizar`, verbo: es la acción de
mirar, no un recurso llamado «previsualización».

Respuesta (`200` al previsualizar, `201` al crear):

```json
{ "programacion": { "id": 1, "peliculaId": 1, "salaId": 1, "desde": "2026-09-07",
                    "hasta": "2026-09-13", "horaInicio": "20:30", "diasSemana": [],
                    "idioma": "SUBTITULADA", "proyeccion": "DOS_D",
                    "precio": 5000.0, "activa": true },
  "funciones": [
    { "inicio": "2026-09-07T20:30:00", "choca": false },
    { "inicio": "2026-09-09T20:30:00", "choca": true,
      "motivo": "la sala ya tiene la función 1 a las 09/09 21:00" }
  ],
  "generadas": 6, "salteadas": 1 }
```

Al **previsualizar**, `programacion.id` viene en `0`: esa grilla existe solo en memoria.

`motivo` viaja únicamente cuando `choca` es `true`, y dice **contra qué** se pisa. Es lo
que hace que el informe se pueda accionar: el administrador ve que el miércoles ya hay
algo a las 21:00 y decide si mueve la grilla o la deja así.

`generadas` y `salteadas` son cuentas que el front podría hacer solo, pero son justo lo
que se muestra arriba de todo —*«12 funciones, 3 se saltearon»*— y contarlas del lado del
servidor evita que cada pantalla las cuente a su manera.

> **Al aplicar se valida de nuevo.** `POST /api/programaciones` no recibe el informe
> previsualizado: lo recalcula. Entre que el administrador mira y confirma, otro pudo
> haber programado algo en esa sala. Por eso el front no debe cachear el informe ni
> asumir que confirmar da el mismo resultado — tiene que repintar con lo que devuelve.

Lo que **no depende de la fecha** —que existan película y sala, R8 y el precio— falla con
`400` ya en `/previsualizar`, no recién al confirmar: si la sala no proyecta en 3D, no hay
ninguna fecha del rango en la que sí.

| Entrada | Respuesta |
|---|---|
| `proyeccion: "TRES_D"` en una sala 2D | `400` «La sala Sala 1 no puede proyectar en 3D» |
| `desde` posterior a `hasta` | `400` «El rango tiene que empezar antes de terminar» |
| `horaInicio` mal formada | `400` «la hora de la función tiene que ser una hora válida» |
| Rango que no cae en ningún `diasSemana` | `400` — se daría de alta una grilla que no generaría nada |

### `GET /api/programaciones`
Todas las grillas, activas e inactivas. **Sin** las funciones que generaron: traerlas sería
una consulta por fila para una pantalla donde no se leen.

### `GET /api/programaciones/{id}`
La grilla con un campo `funciones` extra: `[{"id": 2, "inicio": "2026-09-07T20:30:00"}, …]`.

### `POST /api/programaciones/{id}/baja` y `/alta`
Desactiva o reactiva. **No hay `DELETE` a propósito**, y la baja **no toca las funciones ya
generadas**: pueden tener entradas vendidas. Solo evita que la grilla genere nuevas.

> **La asociación se navega en un solo sentido.** El backend guarda de qué grilla salió
> cada función —`funcion.programacion_id`, en `null` cuando la cargó el administrador a
> mano con `POST /api/funciones` (CU-03), que sigue existiendo— pero **`FuncionVista` no
> expone ese campo**: `GET /api/funciones` no dice a qué grilla pertenece cada una. Para
> ir de la grilla a sus funciones está `GET /api/programaciones/{id}`; para el camino
> inverso, hoy no hay endpoint. Si el front alguna vez necesita marcar en el listado de
> funciones cuáles vienen de una grilla, hay que sumar `programacionId` a esa vista.

## Promociones (CU-17)

El ABM del administrador. Es lo que justifica que la promoción sea una entidad y no tres
constantes en el backend: si el cine no las puede cargar, no hacía falta modelarla.

### `POST /api/promociones`
Un solo pedido para los tres tipos, con las columnas del beneficio en `null` salvo la que
corresponde. Es la misma forma que tiene la tabla, y evita tres endpoints que se
diferencian en un campo.

```json
{ "nombre": "Miércoles 2x1", "tipo": "NXM", "lleva": 2, "paga": 1,
  "vigenciaDesde": "2026-08-01", "vigenciaHasta": "2026-12-31",
  "diasSemana": ["WEDNESDAY"], "horaDesde": null, "horaHasta": null,
  "mediosPago": [] }
```

| `tipo` | Campos que usa | Ejemplo |
|---|---|---|
| `PORCENTAJE` | `porcentaje` (1 a 99) | 30% off |
| `MONTO_FIJO` | `monto` | $2000 off |
| `NXM` | `lleva`, `paga` (`lleva` > `paga`) | 2x1 |

**Las listas vacías no restringen**: sin `diasSemana` corre todos los días, sin
`mediosPago` con cualquiera. `horaDesde`/`horaHasta` en `null` es todo el día.

Las condiciones se evalúan contra el horario de la **función**, no contra el momento de la
compra: un 2x1 de los miércoles vale para la función del miércoles, aunque las entradas se
compren el lunes.

### `GET /api/promociones` y `GET /api/promociones/{id}`
La carta completa, activas e inactivas.

### `POST /api/promociones/{id}/baja` y `/alta`
Desactiva o reactiva. **No hay `DELETE` a propósito**: una promoción usada en un cobro
tiene que seguir existiendo para poder explicar por qué se cobró ese monto.

> **Cómo se elige cuál se aplica.** No se acumulan: se evalúan todas las que corren para
> esa función y ese medio de pago, y gana la que más descuenta (R15). En un empate, la de
> menor id. Y las entradas de tarifa reducida quedan afuera del cálculo (R16): un jubilado
> ya tiene su precio especial y no entra además al 2x1. El front no calcula nada de esto,
> le llega resuelto en la respuesta del pago.

## Control de acceso (CU-18)

### `POST /api/acceso`
```json
{ "codigo": "K7M2P9XQ" }
```
Lo que llama el acomodador al escanear el QR. Devuelve la `ReservaVista` completa, con las
butacas y la tarifa de cada una, que es lo que necesita para saber a quién pedirle carnet.

Va por código y no por id porque el código es lo que trae el QR y, como el cliente no
inicia sesión, es su única credencial: con el id se entraría probando números.

**Es `POST` y no `GET`** porque no es una consulta: marca la entrada como usada. Repetirlo
falla con `400` (R18), igual que un código inexistente o una reserva sin pagar.

El código tiene 8 caracteres de un alfabeto sin `O`, `I`, `0` ni `1`, porque se tipea a
mano cuando el escáner no lee y esos cuatro se confunden entre sí.
