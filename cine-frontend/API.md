# Contrato de la API

Base `/api`. La consumen la web (`src/api/api-http.js`) y Swing (una `Api<Subdominio>` por subdominio), con
los mismos nombres de operación. Se prueba en <http://localhost:8080/swagger-ui.html>.

## Convenciones

| | |
|---|---|
| Formato | JSON. El cuerpo de un pedido es un objeto |
| Fechas | ISO local sin zona: `2026-08-13T20:30:00`, `2026-08-13`, `20:30`. Años de 1000 a 9999 |
| Enums | Nombre de la constante (`MAS_16`, `TRES_D`); los clientes traducen a etiqueta |
| Textos | Nombres, títulos, emails, códigos y enums se comparan sin distinguir mayúsculas ni espacios alrededor |
| Números | Un entero no acepta decimales (`1.0` tampoco) |
| Importes | Un precio o un monto que se carga es mayor a cero, con hasta 2 decimales y hasta $ 1.000.000. Los precios que salen ya traen los multiplicadores |
| Auth | HTTP Basic sin sesión: `Authorization: Basic base64(email:contraseña)` en cada pedido |

### Respuestas

| Operación | Status | Cuerpo |
|---|---|---|
| Leer o previsualizar | `200` | El recurso o la lista (`[]` si no hay nada) |
| Alta | `201` + `Location` | El recurso creado. Sin `Location` cuando lo creado no tiene `GET` por id: checkout, grilla, importación y venta de candy |
| Acción o `PATCH` | `200` | El recurso como quedó |
| Baja | `204` | Vacío |
| Error | `4xx` · `5xx` | `{"error": "…"}` en JSON, que los clientes muestran tal cual |

### Errores

| Status | Cuándo | Ejemplo |
|---|---|---|
| `400` | Dato inválido o regla incumplida. Lo que falta se rechaza antes de buscar nada; un importe, antes que el resto | `Falta el nombre` |
| `400` | Un enum, una fecha o un parámetro que no se entiende | `El medio de pago no es válido: CRIPTO` |
| `400` | Un cuerpo ilegible: ausente, JSON roto, raíz que no es objeto, tipo equivocado o decimal en un entero | `El campo precio tiene un valor inválido: abc` |
| `401` | Sin credenciales, o inválidas (también en rutas públicas) | `Hace falta iniciar sesión para esta operación` · `Email o contraseña incorrectos` |
| `403` | El rol no alcanza | `Tu rol no tiene permiso para esta operación` |
| `404` | Un id o código inexistente, en la ruta o en el cuerpo; una ruta inexistente | `No existe la película 99` |
| `405` · `406` · `415` | Método no aceptado (con `Allow`), `Accept` sin JSON, cuerpo que no es JSON | `La ruta no acepta PUT` |
| `409` | Nombre, título o email ya usado | `Ya existe una sala con ese nombre` |
| `409` | Butaca de otro (R4), o dos pedidos que cambian lo mismo a la vez | `La butaca A1 ya está ocupada` |
| `500` | Falla del servidor, con el detalle solo en el log. Si falla el archivo de un comprobante, la venta ya quedó guardada y responde igual | `Ocurrió un error inesperado en el servidor` |
| `413` · `502` · `504` | nginx: cuerpo de más de 1 MB, backend caído, más de 30 s sin respuesta (180 s en importaciones) | `El servidor no está disponible: volvé a intentarlo en un rato` |

### Quién puede llamar a qué

| Nivel | Rutas |
|---|---|
| Público | `POST` de `/clientes`, `/reservas`, `/funciones/{id}/bloqueos` y `/reservas/codigo/{codigo}/cancelacion`. `GET` de `/cartelera`, `/peliculas/{id}` y `/peliculas/{id}/funciones` (una película pendiente o descartada da `404`), `/funciones/{id}`, `/reservas/codigo/{codigo}`, `/reservas?email=`, `/candy/productos`, los catálogos y Swagger |
| `ACOMODADOR` o `ADMINISTRADOR` | `POST /sesion`, `POST /acceso` |
| `ADMINISTRADOR` | Todo lo demás. Una ruta nueva nace así |

## Catálogos

| Ruta | Devuelve |
|---|---|
| `GET /generos` | `["ACCION", "COMEDIA", …]` |
| `GET /clasificaciones` | `[{"nombre":"ATP","edadMinima":0}, …]` |
| `GET /tipos-sala` | `[{"nombre":"IMAX","multiplicador":1.6,"soportaTresD":true}, …]` |
| `GET /idiomas` · `GET /proyecciones` | `["DOBLADA","SUBTITULADA"]` · `["DOS_D","TRES_D"]` |
| `GET /medios-pago` | `[{"nombre":"EFECTIVO","requiereAutorizacion":false}, …]` |
| `GET /tarifas` | `[{"nombre":"JUBILADO","multiplicador":0.5,"requiereAcreditacion":true}, …]` |
| `GET /tipos-producto` | `[{"nombre":"POCHOCLOS","esCombo":false}, …, {"nombre":"COMBO","esCombo":true}]` |
| `GET /tipos-promocion` | `[{"nombre":"NXM","campos":["lleva","paga"]}, …]`: qué campos pide cada tipo |

Los clientes deciden con estos campos, nunca por el nombre de una constante: pedir carnet
(`requiereAcreditacion`), cobro en caja o checkout (`requiereAutorizacion`), qué se da de alta como combo.

---

# Cliente

| Ruta | Qué hace |
|---|---|
| `GET /cartelera?genero=` | Las películas en exhibición |
| `GET /peliculas/{id}` | Una película |
| `GET /peliculas/{id}/funciones` | Sus funciones por `inicio`, con la sala |
| `POST /clientes` | `{nombre, email}`. Email `usuario@dominio.algo`, guardado en minúsculas, único también contra los empleados (`409`). Opcional: reservar da de alta igual |
| `GET /clientes?email=` | `[cliente]` o `[]`. Solo `ADMINISTRADOR` |

```json
{ "id": 1, "titulo": "Matrix", "duracionMinutos": 136, "generos": ["ACCION"],
  "clasificacion": "MAS_16", "posterUrl": "…", "director": "…", "anio": 1999,
  "idiomaOriginal": "Inglés", "sinopsis": "…",
  "enCartelera": true, "estadoRevision": "CONFIRMADA", "puntaje": 8.2, "votos": 26000 }
```

`estadoRevision` (`PENDIENTE`, `CONFIRMADA`, `DESCARTADA`) dice si entró al catálogo; `enCartelera`, si se
está dando. Una función suma `precioDesde` y la sala con `minutosLimpieza`.

## Mapa de butacas

`GET /funciones/{id}?sesion=…`: la función con todas sus butacas.

```json
{ "id": 1, "…": "…", "libres": 50,
  "asientos": [{ "id": 5, "fila": 1, "numero": 5, "codigo": "A5", "tipo": "ESTANDAR",
                 "estado": "HABILITADO", "ocupado": false, "precio": 8000 }] }
```

`ocupado` es de esta función (R4); `estado`, del asiento (R9). Sin `sesion`, los bloqueos propios se ven
ocupados.

`POST /funciones/{id}/bloqueos`: aparta butacas 3 minutos mientras se elige.

```json
{ "sesion": "3f9a…", "butacas": ["C5", "C6"] }
→ { "sesion": "3f9a…", "butacas": ["C5"], "rechazadas": ["C6"], "vencenEnSegundos": 180 }
```

- Va la selección entera: toma, renueva y suelta (`[]` suelta todo). Idempotente.
- Una butaca que ya tiene otro no es error: vuelve en `rechazadas`.
- `400`: butaca inexistente o fuera de servicio (R9), más de 10, función empezada (R19), sin `sesion` o de
  más de 64 caracteres. La `sesion` vuelve sin los espacios de las puntas.

## Reserva

`POST /reservas`

```json
{ "funcionId": 1, "nombre": "…", "email": "…", "sesion": "3f9a…",
  "butacas": { "C5": "GENERAL", "C6": "JUBILADO" } }
```

- `butacas`: código → tarifa, de 1 a 10. Una tarifa vacía es `GENERAL`.
- `sesion`: la de los bloqueos, que se sueltan al confirmar. Opcional en boletería.
- Si el email es nuevo, da de alta al cliente; el de un empleado es `400`.
- Orden de los errores: lo que falta y las tarifas (`400`), la función (`404`, empezada `400`), el cliente,
  cada butaca (inexistente o fuera de servicio: `400`), el pedido entero (más de 10, repetidas: `400`) y
  recién al final la ocupación (`409`). Un pedido que no puede pasar nunca da `400` aunque además tenga una butaca ocupada.
- Devuelve la reserva con `codigo` (el del QR) y `entradas[].tarifa`.

Toda reserva trae `cobrable` y `cancelable`, calculados con las mismas reglas que aplica el backend al cobrar
(R5, R17, R19) y al cancelar (R13).

| Ruta | Qué hace |
|---|---|
| `GET /reservas/codigo/{codigo}` | El ticket, con función, película, sala, cliente, `total` y `codigo` |
| `POST /reservas/codigo/{codigo}/cancelacion` | El cliente cancela la suya: solo sin cobrar (R13); libera las butacas (R6) |
| `GET /reservas?email=` | Las de ese cliente, **sin `codigo`**: el email no prueba ser el dueño |
| `GET /reservas/{id}` · `POST /reservas/{id}/cancelacion` | Lo mismo por id, solo `ADMINISTRADOR`: el id se adivina |
| `GET /reservas?estado=&dia=&q=` | Listado del encargado, de la más nueva a la más vieja. `q` busca en código, butaca, cliente, email y película |

## Pago

`POST /reservas/{id}/pago`

```json
{ "medio": "CREDITO", "codigoAutorizacion": "AUTH-40219" }
→ { "id": 2, "reservaId": 25, "subtotal": 15360, "promocionId": 1,
    "descuento": 7680, "monto": 7680, "medio": "CREDITO",
    "fecha": "2026-08-13T19:40:00", "codigoAutorizacion": "AUTH-40219" }
```

- El monto no viaja: el descuento depende del medio. `monto` es lo que entra en caja.
- `400`: ya pagada (R5), sin código si el medio lo exige o con código en efectivo (R11), vencida (R17),
  función empezada (R19).
- Cobrar y cancelar a la vez: gana el primero y el otro recibe `409`.
- `GET /reservas/{id}/pago`: el pago, o `404` si todavía no se cobró.

`POST /reservas/{id}/checkout` `{ "medio": "QR" }`: medios electrónicos, contra una pasarela emulada. Devuelve
`{id, monto, urlPago, codigoQr}`; efectivo es `400`. `POST /checkouts/{id}/confirmacion`, sin cuerpo, lo
autoriza: `201` con el pago. Revalida la reserva.

---

# Encargado

`POST /sesion`, sin cuerpo y con Basic: `200` con `{id, nombre, email, rol}` (`ADMINISTRADOR` o `ACOMODADOR`).
No hay token: quien llama guarda las credenciales y las manda en cada pedido. Clave mala y email inexistente
dan el mismo `401`.

## Cartelera y salas

| Ruta | Notas |
|---|---|
| `GET /peliculas?q=&genero=&publicada=` | Todas. `q` busca en el título; `publicada` es `enCartelera` |
| `GET /peliculas/pendientes` | El buzón del importador |
| `POST /peliculas` | Título único (R1, `409`), duración 1 a 600 (R2), al menos un género (R7), clasificación (R10). `puntaje` 0 a 10, `anio` desde 1895, `posterUrl` con `http(s)://`. Nace `CONFIRMADA` |
| `POST /peliculas/{id}/confirmacion` | `CONFIRMADA` y en cartelera |
| `POST /peliculas/{id}/descarte` | `DESCARTADA`; `400` si tiene funciones |
| `PUT /peliculas/{id}` | Parcial: lo que no viaja queda igual. `enCartelera: true` solo publica una `CONFIRMADA` |
| `DELETE /peliculas/{id}` | `400` si tiene funciones o una programación |
| `GET /salas` · `GET /salas/{id}` | `{id, nombre, tipo, butacasPorFila, filas, capacidadSala, minutosLimpieza}`; el detalle suma `asientos` |
| `POST /salas` | `{nombre, tipo, butacasPorFila, codigosVip, codigosPareja, codigosAccesibles, minutosLimpieza}`. 1 a 26 filas, 1 a 40 butacas por fila; cada código especial existe y está en una sola lista. Limpieza 0 a 120, 15 por defecto. Nombre único (`409`) |
| `PUT /salas/{id}` | `{nombre, tipo, minutosLimpieza}`. Las butacas no se editan; el tipo, tampoco si tiene funciones. Los datos se validan antes que el nombre repetido |
| `DELETE /salas/{id}` | `400` si tiene funciones o una programación |
| `PATCH /salas/{salaId}/asientos/{codigo}` | `{"estado":"FUERA_DE_SERVICIO"}` o `HABILITADO` (R9) |
| `GET /funciones?peliculaId=&salaId=&desde=&hasta=` | Con película y sala, por `inicio`. `hasta` incluye todo el día |
| `POST /funciones` | Película confirmada, sin superposición en la sala (R3), 3D solo en sala que lo soporta (R8), `inicio` futuro (R20), sin segundos y dentro del próximo año |
| `DELETE /funciones/{id}` | `400` si tiene reservas, aun canceladas (R12) |

## Programaciones (CU-03b)

`POST /programaciones/previsualizacion` (no escribe) y `POST /programaciones`, mismo cuerpo:

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

- `diasSemana` vacío es todos. Sin `hasta`, queda abierta y se extiende sola de a 14 días.
- Los horarios que ya pasaron se saltean sin contar (R20); los que chocan, se saltean y cuentan.
- `400`: película sin confirmar, R8, rango de más de 366 días o que termina a más de un año, hora con
  segundos, rango sin ningún día elegido, o todos los horarios ya pasados.
- `GET /programaciones?peliculaId=&salaId=&activa=`, `GET /programaciones/{id}` (con sus funciones) y
  `PATCH /programaciones/{id}` con `{"activa": false|true}`. No hay `DELETE`.

## Grilla automática

`POST /grilla/propuesta` (no crea) y `POST /grilla`, mismo cuerpo. Solo `precio` es obligatorio; por defecto,
una semana desde hoy, de 14 a 24, ocho títulos, subtitulada en 2D.

```json
{ "desde": "2026-09-01", "dias": 7, "apertura": "14:00", "cierre": "00:00",
  "cuantasPeliculas": 8, "precio": 5000, "idioma": "SUBTITULADA", "proyeccion": "DOS_D" }
→ { "elenco": [{ "id": 4, "titulo": "…", "puntaje": 8.2, "pases": 12, "…": "…" }],
    "pases": [{ "peliculaId": 4, "salaId": 1, "inicio": "2026-09-01T14:00:00", "…": "…" }],
    "indicadores": { "minutosProgramados": 3320, "minutosDisponibles": 4200, "ocupacion": 0.79,
                     "puntajePromedio": 7.8, "generosCubiertos": 6, "generosTotales": 9 },
    "funcionesCreadas": 0 }
```

- `cierre: "00:00"` es fin del día. Solo películas confirmadas; no pisa funciones existentes ni propone
  horarios que ya pasaron (R20). En 3D usa solo las salas que lo proyectan.
- `400`: `desde` en el pasado o fin a más de un año, `dias` fuera de 1 a 31, `cuantasPeliculas` fuera de 1 a
  20, cierre que no es posterior a la apertura, ninguna película o sala posible.

## Promociones (CU-17)

`POST /promociones`: `nombre`, `tipo` y la vigencia son obligatorios; lo que no aplica al tipo va en `null`.

```json
{ "nombre": "Miércoles 2x1", "tipo": "NXM", "lleva": 2, "paga": 1,
  "vigenciaDesde": "2026-08-01", "vigenciaHasta": "2026-12-31",
  "diasSemana": ["WEDNESDAY"], "horaDesde": null, "horaHasta": null, "mediosPago": [] }
```

| `tipo` | Campos |
|---|---|
| `PORCENTAJE` | `porcentaje`: 1 a 99, hasta 2 decimales |
| `MONTO_FIJO` | `monto`: un importe |
| `NXM` | `lleva` > `paga` ≥ 1, `lleva` hasta 10 |

- Una lista vacía no restringe. Días y franja se evalúan contra el horario de la función.
- `400`: vigencia vencida, ningún día elegido dentro de la vigencia, franja que no empieza antes de terminar.
  Lo común se valida antes que lo propio del tipo.
- `GET /promociones`, `GET /promociones/{id}` y `PATCH /promociones/{id}` con `{"activa": false|true}`. No hay
  `DELETE`.
- No se acumulan: gana el mayor descuento (R15). Las tarifas reducidas quedan afuera (R16).

## Candy (CU-13 a CU-16)

| Ruta | Notas |
|---|---|
| `GET /candy/productos?todos=` | La carta; sin `todos=true`, solo lo disponible |
| `GET /candy/productos/{id}` | Un producto |
| `POST /candy/productos` | `{nombre, tipo, precio}`; un `COMBO` va por `/combos`. Nombre único (`409`) |
| `POST /candy/combos` | `{nombre, precio, componentes: {productoId: cantidad}}`: dos productos o más, ninguno combo, 1 a 20 de cada uno. Más barato que los componentes sueltos (R14) |
| `PUT /candy/productos/{id}` | `{nombre, precio}`. `400` si un combo que lo trae deja de convenir (R14) |
| `PATCH /candy/productos/{id}` | `{disponible}`. No hay `DELETE`: el producto vive en compras viejas |
| `POST /candy/compras` | La venta, que nace cobrada |
| `GET /candy/compras?fecha=&clienteId=` | Las del cliente, o si no las del día |
| `GET /candy/arqueo?fecha=` | `{fecha, total, compras}`, caja aparte de boletería |

En el alta, el combo y la edición, nombre y precio se validan antes que el nombre repetido.

```json
{ "clienteId": 3, "reservaId": 25, "cantidades": { "1": 2, "4": 1 },
  "medio": "EFECTIVO", "codigoAutorizacion": "" }
→ { "id": 8, "fecha": "…", "medio": "EFECTIVO",
    "items": [{ "productoId": 4, "nombre": "Combo clásico", "cantidad": 1,
                "precioUnitario": 5500, "subtotal": 5500 }],
    "total": 12000, "ahorro": 1500 }
```

- Sin `reservaId` es venta de mostrador. Con `reservaId`, la reserva tiene que estar pagada y ser del cliente.
- Cada cantidad de 1 a 20; lo que salió de la carta no se vende. El código, como al cobrar una reserva (R11).
- El pedido y el medio de pago se validan antes de buscar los productos (`404`).
- `ahorro` es el descuento de los combos, congelado al vender.

## Caja e informes

| Ruta | Qué da |
|---|---|
| `GET /arqueo?fecha=` | La caja de boletería del día: `total`, `entradas`, `porMedio` y `pagos` |
| `GET /funciones/{id}/bordero` | Lo cobrado de una función: espectadores, bruta, descuentos, neta y `porTarifa` |
| `GET /funciones/{id}/informe` | El borderó más el candy asociado a reservas de esa función |
| `GET /declaracion-jurada?desde=&hasta=` | Las funciones con entradas cobradas del período, por función, por película y en total, con los datos del exhibidor |

- El borderó corta por función y la declaración jurada por período (INCAA). Sin fechas, la declaración toma
  la semana cinematográfica anterior (jueves a miércoles); `400` con una sola fecha o más de 31 días.
- Los archivos (borderó `.txt`, declaración `.csv`) los genera Swing con estos datos, no el backend.

## Control de acceso (CU-18)

`POST /acceso` `{ "codigo": "K7M2P9XQ" }`: marca la entrada usada y devuelve la reserva. `400` si está sin
pagar (R18), ya se usó o no es el día de la función; `404` si no existe. El código tiene 8 caracteres, sin
`O`, `I`, `0` ni `1`.

## Importador

| Ruta | Notas |
|---|---|
| `POST /importaciones` | `{ "paginas": 1 }` (1 a 3, veinte títulos cada una). Corre y contesta al terminar |
| `GET /importaciones` | Las últimas 20 |
| `GET /importaciones/estado` | `{disponible, detalle}`, sin consultar TMDB |

```json
→ { "id": 7, "estado": "TERMINADA", "paginas": 2, "nuevas": 9, "salteadas": 20, "fallidas": 1,
    "detalle": "+ [41] Hablan las aves\n✗ Yo, narciso: La duración…" }
```

- Lo nuevo entra al buzón como `PENDIENTE`. Una candidata inválida cuenta en `fallidas` y la corrida sigue.
- TMDB caído: `201` con `FALLIDA`. Una corrida trabada pasa a `FALLIDA` a los 5 minutos.
- `400`: otra importación en curso, o la anterior terminó hace menos de 60 segundos.
