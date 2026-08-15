# Contrato de la API

Lo que el frontend espera del backend. Cada función de `js/api.js` corresponde a un
endpoint de esta lista: `js/api.js` reexporta `js/api-http.js`, que llama a estos
endpoints con `fetch`.

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
   "idiomaOriginal": "Inglés", "sinopsis": "…", "enCartelera": true,
   "estadoRevision": "CONFIRMADA" }]
```

`estadoRevision` es `PENDIENTE`, `CONFIRMADA` o `DESCARTADA`, y es un eje distinto de
`enCartelera`: este dice si la película **entró al catálogo** —se responde una sola vez— y
`enCartelera` si **se está dando**, que cambia todas las semanas. Lo que trae el importador
nace `PENDIENTE` y no se puede programar hasta confirmarlo.

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

### `GET /api/funciones/{id}?sesion=…`
El endpoint del mapa de butacas. Lo de arriba **más** todas las butacas de la sala, cada
una con su precio ya calculado y si está ocupada **en esa función**:

```json
{ "id": 1, "…": "…",
  "asientos": [
    { "id": 5, "salaId": 1, "fila": 1, "numero": 5, "codigo": "A5",
      "tipo": "ESTANDAR", "estado": "HABILITADO", "ocupado": false, "precio": 8000 }],
  "libres": 50 }
```

`ocupado` se calcula, y por dos motivos: hay una entrada con ese asiento en alguna reserva
**no cancelada** de esta función, **o** alguien la tiene [bloqueada mientras elige](#post-apifuncionesidbloqueos).
`estado` es del asiento y vale para todas las funciones. Los dos tienen que venir
separados: el front los pinta distinto.

`sesion` es **opcional** y cambia una sola cosa: las butacas que esa sesión tiene
bloqueadas no vuelven marcadas como ocupadas para ella misma. Mandarla siempre desde la
pantalla de compra — sin ella, apenas uno bloquea una butaca el mapa se la muestra tomada.

> **Por qué el filtro es del backend y no del navegador.** El front podría pintar su
> propia selección por encima del campo `ocupado`, y sería una segunda definición de
> «ocupado» viviendo en la pantalla. «Ocupado» lo decide `Ocupacion`, que es la misma
> definición que usa la reserva al validar: si se separan, el mapa termina ofreciendo una
> butaca que la reserva rechaza.

### `POST /api/funciones/{id}/bloqueos`
Mientras alguien elige, sus butacas dejan de ofrecerse al resto. Es la etapa anterior a la
reserva: todavía no hay cliente ni ticket.

```json
{ "sesion": "3f9a…", "butacas": ["C5", "C6"] }
```

```json
{ "sesion": "3f9a…", "butacas": ["C5"], "rechazadas": ["C6"], "vencenEnSegundos": 180 }
```

Se manda la **selección entera**, no una butaca suelta: con eso una sola llamada toma lo
nuevo, renueva lo que sigue elegido y suelta lo que se deseleccionó. `"butacas": []`
suelta todo. Es idempotente, así que se puede llamar en cada click sin llevar la cuenta.

- `butacas` en la respuesta son las que quedaron a nombre de esa sesión. `rechazadas`, las
  que se escaparon. **Perder una butaca no es un error**: las demás sí se consiguieron, así
  que responde `200` y no `409`, y el front saca las rechazadas de la selección y avisa.
- `vencenEnSegundos` dice cuánto dura el bloqueo. Hay que **renovarlo** antes de que venza
  —volver a llamar con la misma selección alcanza—: completar el formulario de la
  confirmación tarda más que la ventana.
- `sesion` la genera el navegador (`crypto.randomUUID()`, en `sessionStorage`: cada pestaña
  es una compra distinta) y **no es una credencial**. No hace falta que lo sea: lo peor que
  puede hacer una sesión inventada es soltar el bloqueo de otro, y eso devuelve una butaca a
  la venta sin poder venderla dos veces.
- Una butaca que no existe en la sala sí es `400`, con el mismo mensaje que al reservar.

> **El bloqueo puede no existir.** Vive en Redis, y si Redis no responde el backend
> contesta que todas se consiguieron y el mapa no muestra ninguna bloqueada: el sistema
> vuelve a comportarse como antes de que el bloqueo existiera. El front no tiene que hacer
> nada distinto — es la misma respuesta, con `rechazadas` siempre vacío. Que una butaca no
> se venda dos veces lo sigue garantizando la base, no esto.

### `POST /api/clientes`
`{ "nombre": "Andrei Veis", "email": "andrei@uade.edu.ar" }` → el cliente creado (CU-05).
El email es único: si ya existe, 400 con "Ya hay un cliente registrado con ese email".
Registrarse es opcional — reservar da de alta al cliente igual.

### `GET /api/clientes?email=andrei@uade.edu.ar`
El cliente con ese email, o `null` si no existe. Sin distinguir mayúsculas.

### `POST /api/reservas`
```json
{ "funcionId": 1, "nombre": "Andrei Veis", "email": "andrei@uade.edu.ar",
  "butacas": { "C5": "GENERAL", "C6": "JUBILADO" }, "sesion": "3f9a…" }
```
`butacas` es código de butaca a **tarifa** de quien la ocupa: `GENERAL`, `MENOR`,
`JUBILADO` o `ESTUDIANTE`. Va por butaca y no por reserva porque la tarifa es por
persona — en una reserva de cuatro puede haber dos generales, un menor y un jubilado.

`sesion` es la misma con la que se pidieron los [bloqueos](#post-apifuncionesidbloqueos) y
hay que mandarla: sin ella el propio bloqueo hace rebotar la reserva con «la butaca C5 ya
está ocupada». Es **opcional** porque la boletería confirma sin haber pasado por la etapa
de elegir. Al confirmar, el backend suelta todos los bloqueos de esa sesión en esa función,
incluidos los de las butacas que se miraron y no se compraron.

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
| GET | `/api/peliculas/pendientes` | El buzón: lo que trajo el importador y nadie miró. Va **antes** que `/{id}` en el registro de rutas |
| POST | `/api/peliculas` | R1 título único, R2 duración > 0, R7 un género, R10 clasificación. Nace `CONFIRMADA` |
| POST | `/api/peliculas/importadas` | Mismo cuerpo que el alta, pero nace `PENDIENTE` y fuera de cartelera. Es la puerta del alta importada para un cliente externo; el importador de TMDB ya no la usa, entra por el gestor |
| POST | `/api/peliculas/{id}/confirmacion` | La acepta: pasa a `CONFIRMADA` y queda en cartelera |
| POST | `/api/grilla/propuesta` | Arma la grilla de la semana **sin escribir**: elige el elenco por puntaje y diversidad y lo reparte en las salas |
| POST | `/api/grilla` | La misma grilla, creando las funciones. Recalcula en vez de recibir la propuesta: el algoritmo es determinista |
| POST | `/api/peliculas/{id}/descarte` | La rechaza: pasa a `DESCARTADA`. 400 si ya tiene funciones programadas |
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

### `POST /api/reservas/{id}/checkout`
```json
{ "medio": "QR" }
```
El camino de los medios electrónicos: el código de autorización que pide R11 no lo tipea
nadie, lo devuelve el procesador. **El efectivo no pasa por acá** — 400 con «El pago con
EFECTIVO se cobra en la caja del cine, no por checkout».

```json
{ "id": "MP-1234567890", "reservaId": 25, "medio": "QR", "monto": 7680,
  "urlPago": "https://checkout.emulado.local/mp/MP-1234567890",
  "codigoQr": "MP-QR|MP-1234567890" }
```

`monto` **ya viene con el descuento aplicado**: es el importe que el cliente va a aprobar en
la pantalla del procesador, y si después se cobrara otro no coincidirían. `codigoQr` es el
*contenido* del QR, no una imagen: dibujarlo es del navegador, igual que traducir los enums.

Valida lo mismo que el cobro —R5, R17, R19— y no al confirmar: mandar a pagar una reserva
que no se puede cobrar termina en plata que hay que devolver, y devolución es justo lo que
no existe (R13). Una reserva ya pagada tampoco abre checkout.

> La pasarela es una **emulación**: no hay credenciales ni llamadas de red. El host
> `emulado.local` está elegido para que no se pueda confundir con uno real.

### `POST /api/checkouts/{id}/confirmacion`
Sin cuerpo. Es «el cliente pagó»: la pasarela devuelve el código de autorización y con él
se registra el cobro. Responde el mismo `PagoVista` que `POST /api/reservas/{id}/pago`, ya
con `codigoAutorizacion`. Un checkout inexistente da 400, y confirmarlo dos veces también:
la segunda choca contra R5, así que un doble click no cobra dos veces.

> Qué se está pagando sale del checkout y no de quien confirma, por lo mismo que el monto
> no viaja en el pedido de cobro: si la reserva fueran datos de entrada, se podría autorizar
> un checkout de $16.000 y aplicarlo a otra reserva.
>
> En una integración de verdad esto lo dispara el aviso del procesador. Acá lo dispara el
> front, que es la parte que la emulación no puede fingir: alguien tiene que decir que la
> plata llegó.

### `GET /api/arqueo?fecha=2026-08-13`
```json
{ "fecha": "2026-08-13", "total": 45450, "entradas": 5,
  "porMedio": { "EFECTIVO": {"cantidad":1,"total":24000} },
  "pagos": [{ "id":1, "reservaId":1, "monto":24000, "medio":"EFECTIVO",
              "fecha":"2026-08-13T14:22:00", "codigoAutorizacion":"",
              "pelicula": {…}, "cliente": {…}, "entradas": 3 }] }
```

## Informes por función

El arqueo cierra la caja de un **día**. Estos dos cortan por **función**, que es lo que
pide el INCAA y lo que responde «cuánto dejó esta función». No se pueden derivar del
arqueo: las entradas de una función se venden a lo largo de varios días.

### `GET /api/funciones/{id}/bordero`
```json
{ "funcionId": 3, "pelicula": "Matrix", "sala": "Sala 1",
  "funcion": "2026-08-20T20:00:00", "generadoEn": "2026-08-20T23:15:00",
  "espectadores": 15, "recaudacionBruta": 67500, "descuentos": 5000,
  "recaudacionNeta": 62500,
  "porTarifa": { "GENERAL": {"cantidad":12,"total":60000},
                 "JUBILADO": {"cantidad":3,"total":7500} } }
```

Declara lo **cobrado**: una reserva sin pagar retiene butacas pero no vendió ninguna
entrada. Una función sin ventas no es un error, es un borderó en cero con `porTarifa`
vacío; una función que no existe es `404`.

Las tres cifras van separadas porque cuentan cosas distintas: `recaudacionBruta` es a
precio de lista —el valor declarado de cada localidad—, `descuentos` lo que resignó el cine
por una promoción suya, y `recaudacionNeta` lo que entró en la caja. Con una sola, la
diferencia entre entradas × precio y lo cobrado no se puede explicar.

`porTarifa` solo trae las tarifas con entradas vendidas, igual que `porMedio` en el arqueo.

### `POST /api/funciones/{id}/bordero`
Emite el archivo que se sube al INCAA, en `informes/bordero-funcion-<id>.txt` del servidor
—mismo mecanismo que los tickets—. Responde `201` con el mismo cuerpo que el `GET`.

> Es `POST` porque **escribe**: consultar el borderó dos veces no es lo mismo que
> declararlo dos veces. Vuelve a emitir el archivo de esa función y pisa el anterior: el
> borderó de una función es uno solo y el que vale es el último, porque las entradas se
> siguen vendiendo hasta que la película arranca.

### `GET /api/funciones/{id}/informe`
```json
{ "boleteria": { "funcionId": 3, "…": "el mismo borderó" },
  "comprasCandy": 4, "candy": 12000, "total": 74500 }
```

`boleteria` es el borderó completo y no una versión recortada, justamente para que los dos
informes no puedan decir números distintos de lo mismo. `total` es `recaudacionNeta` + `candy`.

> **El candy de mostrador no entra, a propósito.** Solo se atribuye a una función el que
> tiene `reservaId` —el «¿desea agregar pochoclos?» de después de comprar la entrada—,
> porque esa reserva es lo único que dice de qué función se trata. Quien compra un balde en
> el mostrador puede estar yendo a cualquiera de las cuatro funciones de las 22:00, o a
> ninguna: repartirlo sería inventar el dato. Esa plata se cuenta donde sí es cierta, en
> `GET /api/candy/arqueo`. La consecuencia hay que tenerla a mano al leer los números: la
> suma de los informes de todas las funciones de un día da **menos** que el arqueo de ese
> día, y la diferencia es el mostrador.

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


## El armado automático de la grilla

### `POST /api/grilla/propuesta` y `POST /api/grilla`

Mismo cuerpo. **`precio` es obligatorio**; el resto tiene default: una semana desde hoy,
de 14 a 24, con ocho títulos. Sin `precio` responde `400` con
`{"error":"Falta el precio de las funciones"}`.

El precio no tiene default a propósito. Los otros siete son convenciones razonables, pero
cuánto sale la entrada es una decisión comercial que el sistema no puede tomar por el cine:
si inventara un número y el encargado no lo mirara, se venderían entradas a un precio que
no decidió nadie.

```json
{ "desde": "2026-09-01", "dias": 7, "apertura": "14:00", "cierre": "00:00",
  "cuantasPeliculas": 8, "precio": 5000, "idioma": "SUBTITULADA", "proyeccion": "DOS_D" }
```

`cierre: "00:00"` se lee como el final del día, no como su principio.

La respuesta trae las tres cosas con las que se juzga una grilla: a quiénes eligió, qué
hace con ellos y qué tal salió.

```json
{ "elenco": [{ "id": 4, "titulo": "Duna: Parte Dos", "puntaje": 8.2,
               "duracionMinutos": 166, "generos": ["CIENCIA_FICCION","ACCION"], "pases": 12 }],
  "pases": [{ "peliculaId": 4, "titulo": "Duna: Parte Dos", "salaId": 1, "sala": "Sala 1",
              "inicio": "2026-09-01T14:00:00", "duracionMinutos": 166 }],
  "indicadores": { "minutosProgramados": 3320, "minutosDisponibles": 4200,
                   "ocupacion": 0.79, "puntajePromedio": 7.8,
                   "generosCubiertos": 6, "generosTotales": 9,
                   "pasesPorGenero": { "ACCION": 12, "COMEDIA": 5 } },
  "funcionesCreadas": 0 }
```

`minutosDisponibles` es el tiempo de sala que la propuesta **podía** usar: la ventana
entera menos lo que ya estaba programado. Por eso `ocupacion` mide cuánto del hueco real
se llena, y no baja solo porque la semana ya tenga funciones cargadas. Las funciones que
empiezan fuera de la ventana horaria no descuentan.

`funcionesCreadas` es `0` en `/propuesta` y la cantidad real en el alta: el front necesita
distinguir «así quedaría» de «así quedó», y contar los pases no alcanza porque son el
mismo número en los dos casos.

Solo entran películas **confirmadas**: lo que espera en el buzón no se puede programar. Si
no hay ninguna, `400` pidiendo que revisen el buzón. Los pases nunca pisan funciones ya
cargadas —la propuesta consulta R3 con la limpieza incluida— así que se puede correr sobre
una semana que ya tiene algo programado.

## El importador de cartelera

Traer de TMDB lo que está hoy en cartelera en Argentina, a pedido del encargado. Lo que
entra cae en el buzón (`estadoRevision: PENDIENTE`), no en el catálogo.

| Método | Ruta | Notas |
|---|---|---|
| POST | `/api/importaciones` | Corre una importación y contesta **cuando terminó**. Cuerpo opcional |
| GET | `/api/importaciones` | Las últimas 20, de la más nueva a la más vieja |
| GET | `/api/importaciones/estado` | Si el importador puede correr. Va **antes** que el listado en el registro de rutas |

### `POST /api/importaciones`

```json
{ "paginas": 2 }
```

`paginas` es opcional —ausente es una, de veinte títulos— y va de 1 a 3. El cuerpo entero
también es opcional: un POST sin cuerpo es un pedido válido.

```json
{ "id": 7, "estado": "TERMINADA", "paginas": 2,
  "pedidaEn": "2026-08-14T17:00:54", "terminoEn": "2026-08-14T17:01:01",
  "nuevas": 9, "salteadas": 20, "fallidas": 1,
  "detalle": "+ [41] Hablan las aves\n✗ Yo, narciso: La duración debe ser mayor a cero" }
```

`estado` es `EN_CURSO`, `TERMINADA` o `FALLIDA`. `detalle` es el log de la corrida —o el
motivo si falló— y puede ser `null`: es texto para leer, no un dato para consultar. Cada
línea es una película: `+` la que entró al buzón, con su id, y `✗` la que el alta rechazó,
con el mensaje de la regla que la rechazó. Las que ya estaban en el catálogo no se
nombran: son veinte líneas de ruido y ya se cuentan en `salteadas`.

**Tarda lo que tarda la corrida**, diez o quince segundos, y contesta con el resultado
final. No hay `202` con un id para ir a preguntar después: eso obligaría al front a
repreguntar cada dos segundos, o sea treinta pedidos para enterarse de algo que este
puede contar de una. nginx tiene un `proxy_read_timeout` de 180s para esta ruta —el resto
de `/api` sigue en 30— y el backend corta a los 120.

**Que TMDB falle no es un error de la API**: responde `201` igual, con la corrida
en `FALLIDA` y el motivo en `detalle`. Una corrida fallida pudo haber cargado algunas
películas antes de cortarse; el buzón es siempre la verdad de qué entró.

Los `400`, con el mensaje que se muestra tal cual:

- `Las páginas a importar van de 1 a 3`
- `Ya hay una importación en curso: esperá a que termine`
- `El importador corrió recién: esperá 60 segundos antes de volver a pedirlo` — la
  protección contra el doble clic. La cartelera no cambia en veinte segundos y cada
  corrida son sesenta llamadas a TMDB, que tiene cuota.

### `GET /api/importaciones/estado`

```json
{ "disponible": true, "detalle": "Listo para traer cartelera" }
```

Lo pide la pantalla al abrirse, una sola vez, para avisar antes de que alguien apriete el
botón y espere en vano. `detalle` viene redactado para mostrarse: hoy lo único que puede
faltar es el token de TMDB, y el mensaje dice dónde cargarlo. No se le pega a TMDB para
contestar esto —sería gastar cuota cada vez que se abre la pantalla.

> Estos endpoints no piden credenciales, como el resto de los del encargado.
