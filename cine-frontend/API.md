# Contrato de la API

Base `/api`. La consumen la web (`src/api/api-http.js`) y Swing (una `Api<Subdominio>` por subdominio), con
los mismos nombres de operación. Se prueba en <http://localhost:8080/swagger-ui.html> (en producción Swagger está apagado: con `CINE_SWAGGER=false` ni la UI ni `/v3/api-docs` existen).

## Convenciones

| | |
|---|---|
| Formato | JSON. El cuerpo de un pedido es un objeto |
| Fechas | ISO local sin zona: `2026-08-13T20:30:00`, `2026-08-13`, `20:30`. Años de 1000 a 9999 |
| Enums | Nombre de la constante (`MAS_16`, `TRES_D`); los clientes traducen a etiqueta |
| Textos | Nombres, títulos, emails, códigos y enums se comparan sin distinguir mayúsculas ni espacios alrededor |
| Números | Un entero no acepta decimales (`1.0` tampoco) |
| Importes | Un precio o un monto que se carga es mayor a cero, con hasta 2 decimales y hasta $ 1.000.000. Los precios que salen ya traen los multiplicadores |
| Auth | Empleados (Swing): HTTP Basic en cada pedido, `Authorization: Basic base64(email:contraseña)`. Clientes (web): solo Google (R28), que deja la cookie `JSESSIONID`, HttpOnly y `SameSite=Lax` |

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
| `401` | Sin credenciales o sin sesión, o inválidas (también en rutas públicas) | `Hace falta iniciar sesión para esta operación` · `Email o contraseña incorrectos` |
| `403` | El rol no alcanza | `Tu rol no tiene permiso para esta operación` |
| `404` | Un id o código inexistente, en la ruta o en el cuerpo; una ruta inexistente; la reserva de otro cliente (mismo texto que un código inexistente) | `No existe la película 99` |
| `405` · `406` · `415` | Método no aceptado (con `Allow`), `Accept` sin JSON, cuerpo que no es JSON | `La ruta no acepta PUT` |
| `409` | Nombre, título o email ya usado; un checkout cuyo pago MP no está aprobado; borrar o cambiar algo en uso (sala con funciones, función con reservas, película programada), una reserva que en su estado no admite lo pedido (cobrar la ya cobrada, la cancelada o la vencida; cancelar la cobrada; entrar sin pagar o dos veces; candy sin pagar), una importación en curso | `Ya existe una sala con ese nombre` |
| `409` | Butaca de otro (R4), o dos pedidos que cambian lo mismo a la vez | `La butaca A1 ya está ocupada` |
| `429` | Cinco claves equivocadas seguidas por email o por IP: un minuto bloqueado, sin llegar a verificar la clave. Vale para `POST /sesion` y cualquier ruta con Basic, que son solo de empleados. Un ingreso correcto reinicia el contador del email, no el de la IP | `Demasiados intentos: esperá un minuto y probá de nuevo` |
| `500` | Falla del servidor, con el detalle solo en el log. Si falla el archivo de un comprobante, la venta ya quedó guardada y responde igual | `Ocurrió un error inesperado en el servidor` |
| `413` · `502` · `504` | nginx: cuerpo de más de 1 MB (3 MB en `POST /imagenes`), backend caído, más de 30 s sin respuesta (180 s en importaciones) | `El servidor no está disponible: volvé a intentarlo en un rato` |

### Quién puede llamar a qué

| Nivel | Rutas |
|---|---|
| Público | `POST` de `/reservas` y `/funciones/{id}/bloqueos`. `GET` de `/sesion/google`, `/cartelera`, `/peliculas/{id}` y `/peliculas/{id}/funciones` (una película pendiente o descartada da `404`), `/funciones/{id}`, `/reservas/codigo/{codigo}`, `/checkouts/{id}/retorno`, `/candy/productos`, `/promociones/vigentes`, `/imagenes/{nombre}`, los catálogos y Swagger |
| Cliente (cookie de sesión) | `POST /sesion` (sin Basic: quién tiene la sesión), `DELETE /sesion`, `GET /reservas/mias`, `GET /candy/compras/mias`, `PUT /cuenta`, `DELETE /cuenta`, `POST /reservas/codigo/{codigo}/cancelacion`, `POST /reservas/codigo/{codigo}/checkout`, `PUT /reservas/codigo/{codigo}/candy` y `GET /reservas/codigo/{codigo}/cotizacion`, solo de reservas propias (R21) |
| `ACOMODADOR` o `ADMINISTRADOR` | `POST /sesion`, `POST /acceso`, `POST /candy/retiros` |
| `ADMINISTRADOR` | Todo lo demás. Una ruta nueva nace así |

Un cliente con sesión que llama una ruta del encargado recibe `403`, y un empleado que llama `/cuenta`, también. `GET /reservas?email=` ya no existe.

### Seguridad

Límites conocidos: el login con Basic (empleados) y cada pedido de Swing comparten el mismo contador de intentos por email e IP. El cliente no tiene clave en el cine: la verifica Google.

## Catálogos

| Ruta | Devuelve |
|---|---|
| `GET /generos` | `["ACCION", "COMEDIA", …]` |
| `GET /clasificaciones` | `[{"nombre":"ATP","edadMinima":0}, …]` |
| `GET /tipos-sala` | `[{"nombre":"IMAX","multiplicador":1.6,"soportaTresD":true}, …]` |
| `GET /idiomas` · `GET /proyecciones` | `["DOBLADA","SUBTITULADA"]` · `["DOS_D","TRES_D"]` |
| `GET /medios-pago` | `[{"nombre":"EFECTIVO","requiereAutorizacion":false}, …]` |
| `GET /tarifas` | `[{"nombre":"JUBILADO","requiereAcreditacion":true}, …]` |
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
| `GET /peliculas/{id}/funciones` | Sus funciones que todavía no empezaron (R19), por `inicio`, con la sala |
| `GET /clientes?email=` | `[cliente]` o `[]`. Solo `ADMINISTRADOR` |

### El ingreso con Google (R28)

El cliente no tiene clave en el cine. No son rutas de `/api`: el navegador va con la página entera.

| Ruta | Qué hace |
|---|---|
| `GET /api/sesion/google` | Pública. `{"disponible": true}`, o `false` si el servidor no tiene `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET`: la web deshabilita el botón |
| `GET /oauth2/authorization/google` | `302` a Google con `scope=openid email profile` y la vuelta en `redirect_uri`. Sin credenciales, `401` |
| `GET /login/oauth2/code/google` | La vuelta de Google. Bien: abre la sesión (cookie `JSESSIONID`, el id cambia al entrar) y `302` a `/#/ingresar`. Mal: `302` a `/#/ingresar?error=<motivo>` sin sesión |

- Email sin verificar en Google: `Tu cuenta de Google no tiene el email verificado: verificalo y volvé a intentar`.
- Email de un empleado: `Ese email es del personal del cine: entrá desde la aplicación de escritorio` (el panel es Swing, con Basic).
- Si ya hay un cliente con ese email (compró sin cuenta, o tenía clave antes), es él: sus reservas pasan a verse en
  `/reservas/mias` y su nombre queda. Si no, se crea con el nombre de Google.
- Cualquier otra falla (canceló en Google, la vuelta sin el pedido de ida): `No se pudo ingresar con Google: volvé a intentarlo`.
- La sesión tiene solo el rol `CLIENTE`: lo del encargado da `403`.

### La cuenta (cliente con sesión, R23 y R26)

Siempre la del cliente de la cookie: ninguna ruta recibe su email ni su id. Sin sesión, `401`. Las que cambian estado
son `PUT`, `POST` o `DELETE`: con CSRF apagado, `SameSite=Lax` no manda la cookie en esos métodos desde otro sitio.

| Ruta | Qué hace |
|---|---|
| `PUT /cuenta` | `{nombre}` → `200` con el usuario de `POST /sesion`. Mismas reglas que el alta; el email no cambia (si viaja, se ignora) (R23) |
| `DELETE /cuenta` | Sin cuerpo → `204`. Cierra esta sesión (borra la cookie). Reservas, pagos y entradas quedan, también las `RESERVADA` sin pagar, que vencen solas; entrar de nuevo con Google la vuelve a abrir (R26, R28) |

```json
{ "id": 1, "titulo": "Matrix", "duracionMinutos": 136, "generos": ["ACCION"],
  "clasificacion": "MAS_16", "posterUrl": "…", "director": "…", "anio": 1999,
  "idiomaOriginal": "Inglés", "sinopsis": "…",
  "enCartelera": true, "estadoRevision": "CONFIRMADA", "puntaje": 8.2, "votos": 26000,
  "trailerYoutube": "vKQi3bBA1y8" }
```

`estadoRevision` (`PENDIENTE`, `CONFIRMADA`, `DESCARTADA`) dice si entró al catálogo; `enCartelera`, si se
está dando. `trailerYoutube` es el id del video de YouTube o `null`: se embebe en
`https://www.youtube.com/embed/{id}`. Una función suma `precioDesde` y la sala con `minutosLimpieza`.

## Mapa de butacas

`GET /funciones/{id}?sesion=…`: la función con todas sus butacas.

```json
{ "id": 1, "…": "…", "libres": 50,
  "asientos": [{ "id": 5, "fila": 1, "numero": 5, "codigo": "A5", "tipo": "ESTANDAR",
                 "estado": "HABILITADO", "ocupado": false, "precio": 8000,
                 "precios": { "GENERAL": 8000, "MENOR": 4800, "JUBILADO": 4000, "ESTUDIANTE": 5600 } }] }
```

`ocupado` es de esta función (R4); `estado`, del asiento (R9). Sin `sesion`, los bloqueos propios se ven
ocupados. `precio` es el de la tarifa general; `precios`, el de cada tarifa con la misma cuenta que la entrada al
reservar: el cliente los muestra y suma, no los recalcula.

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
- Si el email es nuevo, da de alta al cliente; el de un empleado es `400 No se puede comprar con ese email: usá otro`.
- Orden de los errores: lo que falta y las tarifas (`400`), la función (`404`, empezada `400`), el cliente,
  cada butaca (inexistente o fuera de servicio: `400`), el pedido entero (más de 10, repetidas: `400`) y
  recién al final la ocupación (`409`). Un pedido que no puede pasar nunca da `400` aunque además tenga una butaca ocupada.
- Devuelve la reserva con `codigo` (el del QR), `entradas[].tarifa` y `entradas[].requiereAcreditacion` (si la puerta
  pide carnet: sale de la tarifa, los clientes no lo deducen del nombre).

Toda reserva trae `cobrable` y `cancelable`, calculados con las mismas reglas que aplica el backend al cobrar
(R5, R17, R19) y al cancelar (R13, R17), y mientras espera el pago, `venceEn`: hasta cuándo se puede pagar (`creadaEn` + 30 min).
El `estado` es el de ahora: pasado `venceEn` sale `EXPIRADA`, aunque en la base siga `RESERVADA` hasta que alguien
consulte el mapa de la función (R17 no tiene scheduler).

| Ruta | Qué hace |
|---|---|
| `GET /reservas/codigo/{codigo}` | El ticket, con función, película, sala, cliente, `total` y `codigo`. El email del cliente sale enmascarado (`a***@mail.com`): el código alcanza para ver el ticket y no tiene por qué decir de quién es. El ticket lo consulta cada 10 s mientras la reserva espera el pago |
| `GET /reservas/mias` | Cliente con sesión: sus reservas con `codigo`, de la más nueva a la más vieja |
| `POST /reservas/codigo/{codigo}/cancelacion` | Cliente con sesión, solo la suya (R21) y sin cobrar (R13, `409`); libera las butacas (R6). Vencida: `409` `La reserva N venció: sus butacas volvieron a estar disponibles` (R17). Sin sesión `401`; ajena `404` |
| `GET /reservas/{id}` · `POST /reservas/{id}/cancelacion` | Lo mismo por id, solo `ADMINISTRADOR`: el id se adivina |
| `GET /reservas?estado=&dia=&q=` | Listado del encargado, de la más nueva a la más vieja. `q` busca en código, butaca, cliente, email y película |

Toda reserva trae también `candy`: sus compras de candy sin las anuladas, cada una con la forma de
`GET /candy/compras` (`estado` `PENDIENTE` o `COBRADA`). La `PENDIENTE` es la pedida en la web y se cobra con la
reserva; las `COBRADA` son la que se cobró con ella y las del mostrador. `total` sigue siendo solo de las entradas.

## Candy de la compra (R30)

El candy que se pide en la web viaja con la reserva y se cobra en el mismo pago, nunca antes. Si la reserva se
cancela o vence, el pedido queda `ANULADA` y deja de figurar en `candy`.

`PUT /reservas/codigo/{codigo}/candy` (cliente con sesión, solo la suya: sin sesión `401`, ajena `404`)

```json
{ "cantidades": { "4": 1, "1": 2 } }
→ 200, la reserva como en GET /reservas/codigo/{codigo} (email sin enmascarar), con el pedido en candy:
  "candy": [{ "id": 12, "reservaId": 25, "clienteId": 3, "estado": "PENDIENTE", "fecha": "…",
              "codigoAutorizacion": "", "items": [ … ], "total": 12500, "ahorro": 1500 }]
```

- Reemplaza el pedido entero; `{}` lo saca. Sin `cantidades`, `400` `Faltan las cantidades`.
- Los topes del mostrador: de 1 a 20 por producto y solo lo disponible (`400`); producto inexistente `404`.
- Nombre, precio y ahorro se congelan al pedir, como en el mostrador.
- Mientras la reserva se pueda cobrar, con los mismos textos que al cobrar: cancelada o vencida `409` (R5, R17),
  función empezada `400` (R19). Pagada, también: ver «Sumar candy a una entrada ya paga».
- Un pedido `PENDIENTE` no tiene `medio` ni `cobradaEn`; al cobrarse los toma del pago.

`GET /reservas/codigo/{codigo}/cotizacion` (cliente con sesión, solo la suya)

```json
→ [{ "medio": "DEBITO", "entradas": 16000, "descuento": 4800, "promocionId": 3,
     "promocion": "Miércoles 30 % OFF", "candy": 5500, "total": 16700 },
   { "medio": "CREDITO", "entradas": 16000, "descuento": 0, "candy": 5500, "total": 21500 }, …]
```

- Un renglón por medio con checkout (los de `requiereAutorizacion` en `GET /medios-pago`), en el orden del catálogo.
- `total` = `entradas` − `descuento` + `candy`: es el `monto` del checkout que se abra con ese medio. La promo
  corre solo sobre las entradas (R15, R16); `promocionId` y `promocion` no vienen si no hay.
- No abre preferencias en Mercado Pago. Misma validación que el checkout: `409` o `400` si no se puede cobrar.

### Sumar candy a una entrada ya paga

Las mismas tres rutas, sin pantalla nueva en el backend: con la reserva `PAGADA` el pedido pendiente se cobra solo,
en un checkout que lleva únicamente el candy.

- `PUT /reservas/codigo/{codigo}/candy` también acepta la reserva pagada, hasta que empieza la función: después,
  `400` `La función ya empezó: el candy se compra en el mostrador`. Cancelada o vencida sigue en `409`.
- `GET /reservas/codigo/{codigo}/cotizacion` de una pagada con candy pendiente da `entradas: 0`, `descuento: 0` y el
  candy en `candy` y `total`. Pagada y sin pedido pendiente: `409` `La reserva está pagada: no se puede cobrar`, como hoy.
- `POST /reservas/codigo/{codigo}/checkout` `{medio}` con esa reserva abre un checkout solo de candy: `monto` es el
  candy, la preferencia de MP lleva un solo ítem y el `id` termina en `-CANDY` (`<reservaId>-<MEDIO>-<azar>-CANDY`).
  Vence en 30 minutos desde que se abre.
- La vuelta (`GET /checkouts/{id}/retorno`) y la conciliación lo cobran igual que uno de entradas: el pedido pasa a
  `COBRADA` con el medio y la autorización de MP, sin `Pago` (el borderó no cambia). Vuelve a `/#/ticket/<codigo>`.
  Si el pedido cambió después de abrirlo, el mismo `409` `El monto cambió después de abrir el pago: …`.
- `POST /checkouts/{id}/confirmacion` (encargado) no confirma uno de candy: `400` `El checkout <id> es del candy: se
  cobra cuando el comprador vuelve de la pasarela`.

### Las compras del cliente (CU-16)

`GET /candy/compras/mias` (cliente con sesión): sus compras de candy cobradas, de la más nueva a la más vieja, con la
forma de `GET /candy/compras`, también las del mostrador a su nombre. Lo pendiente y lo anulado no son compras: se
ven en la reserva. Sin sesión `401`.

## Pago

`POST /reservas/{id}/pago`

```json
{ "medio": "CREDITO", "codigoAutorizacion": "AUTH-40219" }
→ { "id": 2, "reservaId": 25, "subtotal": 15360, "promocionId": 1,
    "descuento": 7680, "monto": 7680, "medio": "CREDITO",
    "fecha": "2026-08-13T19:40:00", "codigoAutorizacion": "AUTH-40219" }
```

- El monto no viaja: el descuento depende del medio. `monto` es lo que entra en caja por las entradas.
- El mismo pago cobra el candy `PENDIENTE` de la reserva, con el mismo medio y la misma autorización (R30): queda
  `COBRADA` con `cobradaEn` y sale su ticket. El borderó y la declaración jurada siguen leyendo solo `monto`.
- `409`: ya pagada, cancelada (R5) o vencida (R17). `400`: sin código si el medio lo exige o con código en
  efectivo (R11), función empezada (R19).
- Cobrar y cancelar a la vez: gana el primero y el otro recibe `409`.
- `Location` apunta a la reserva: `GET /reservas/{id}` trae el pago en `pago` (`null` si no se cobró).

`POST /reservas/{id}/checkout` `{ "medio": "QR" }` (encargado) y `POST /reservas/codigo/{codigo}/checkout`
(cliente con sesión, solo la suya: sin sesión `401`, ajena `404`): medios electrónicos, efectivo es `400`. Ambos
van por `GestorPagos.iniciarCheckout`, así que medio y descuento coinciden. `201` con
`{id, reservaId, medio, monto, urlPago, codigoQr}`. `monto` = entradas − descuento + candy pendiente, como la cotización.

- Si el candy (o la promo) cambia después de abrir el checkout, ese checkout ya no se cobra: `409`
  `El monto cambió después de abrir el pago: acercate a la boletería` (en el retorno, como `aviso`). Se abre otro antes de pagar.
- Pasarela: Checkout Pro de Mercado Pago (sandbox) si hay `MP_ACCESS_TOKEN`; si no, la emulada. La preferencia lleva un ítem por las entradas (ya con el descuento) y otro por el candy, si hay. `urlPago` lleva a una preferencia que vence con la reserva (30 min) y solo acepta los tipos de pago del medio pedido: DEBITO `debit_card`, CREDITO `credit_card`, QR `account_money`, TRANSFERENCIA `bank_transfer`; `account_money` se acepta siempre porque MP no deja excluirlo.
  Con la emulada, `urlPago` es relativa al sitio: `/#/pasarela-emulada/<id>?monto=<monto>&medio=<MEDIO>`, una pantalla
  de prueba con Aprobar y Rechazar que vuelve por el retorno con `?status=approved` o `?status=rejected`. `monto` es el total, candy incluido.
- `id` = `<reservaId>-<MEDIO>-<azar>` (el `external_reference` de MP). Un id con otro formato o que MP no conoce da `404`.
- `POST /checkouts/{id}/confirmacion`, sin cuerpo, lo autoriza: `201` con el pago y revalida la reserva. Con MP
  consulta `GET /v1/payments/search`; sin un pago `approved` da `409` con el motivo (rechazado, pendiente, sin pago).
- `GET /checkouts/{id}/retorno` (público): la back_url de MP. De la query que agrega MP solo mira `status`: con
  `rejected` no cobra y avisa «Mercado Pago rechazó el pago: probá de nuevo con otro medio»; con cualquier otro (o sin
  query) consulta el pago y cobra si está aprobado (recargar no cobra dos veces). `approved` no alcanza: con MP de
  verdad lo decide `/v1/payments/search`, y solo la emulada aprueba sin preguntar. `302` a `/#/ticket/<codigo>` o
  `/#/ticket/<codigo>?aviso=<texto>` si no se pudo cobrar; `404` si el checkout no existe.
  El front abre MP en la misma pestaña y, al volver al ticket (o con «Ya pagué»), llama esta misma ruta con
  `redirect: "manual"`: en localhost MP no redirige al sitio y así no espera a la conciliación.
- Un `approved` cuenta solo si coincide monto, moneda (ARS) y tipo de pago del medio; si no, `409` en `/confirmacion` y aviso
  «Mercado Pago registró un pago que no coincide con la reserva: acercate a la boletería» en el retorno.
- Sin webhook (localhost): una tarea del backend (`ConciliacionPagos`, cada 60 s) busca los aprobados de los últimos 30 min
  y cobra los que el comprador no volvió a confirmar. Un pago `pending` se cobra cuando MP lo aprueba.

### Rutas de la web

| Ruta | Qué muestra |
|---|---|
| `#/` | Hero con destacadas y la cartelera |
| `#/nosotros` | Integrantes y descripción del cine |
| `#/ingresar[?error=]` | Botón «Ingresar con Google» (deshabilitado si `GET /sesion/google` dice que no). Al volver con sesión sigue la compra de la que vino (guardada en `sessionStorage`) o va a `#/mis-reservas`; `error` es el motivo del rechazo |
| `#/mi-cuenta` | Con sesión: nombre y dar de baja (con confirmación) |
| `#/mis-reservas` | Con sesión: `GET /reservas/mias`, con Cancelar y Pagar online. Sin sesión: búsqueda por código |
| `#/ticket/<codigo>[?aviso=]` | Público, sin cancelar. El QR, «Descargar entrada» y «Mostrar en la puerta», solo con la reserva pagada y sin usar; sin pagar dice hasta qué hora se paga (`venceEn`). `aviso` es el motivo si el pago de MP no se cobró |

---

# Encargado

`POST /sesion`, sin cuerpo y con Basic: `200` con `{id, nombre, email, rol, empleado}` (`ADMINISTRADOR` o `ACOMODADOR`;
un cliente no tiene clave y da `401`). `empleado` (boolean) dice si entra al panel: los clientes lo leen en vez de
deducirlo del nombre del rol. Clave mala y email inexistente dan el mismo `401`.

- Empleado: no hay token ni sesión; quien llama guarda las credenciales y las manda en cada pedido (Swing).
- Cliente: la vuelta de Google setea `JSESSIONID` (HttpOnly, `SameSite=Lax`) y los pedidos siguientes la mandan
  (`credentials: 'same-origin'`, el default de fetch). Sin CSRF: `SameSite=Lax` no envía la cookie en un `POST` o `DELETE` de otro sitio.
- `POST /sesion` sin Basic devuelve al cliente de la cookie (`rol` `CLIENTE`, `empleado` `false`), o `401`: así la web sabe quién está.
- `DELETE /sesion` → `204`: invalida la sesión y borra la cookie.

## Cartelera y salas

| Ruta | Notas |
|---|---|
| `GET /peliculas?q=&genero=&publicada=` | Todas. `q` busca en el título; `publicada` es `enCartelera` |
| `GET /peliculas/pendientes` | El buzón del importador |
| `POST /peliculas` | Título único (R1, `409`), duración 1 a 600 (R2), al menos un género (R7), clasificación (R10). `puntaje` 0 a 10, `anio` desde 1895, `posterUrl` con `http(s)://`. `trailerYoutube` acepta el id o una URL de YouTube (`watch?v=`, `youtu.be/`, `/embed/`) y guarda el id; otra cosa, `400 El trailer tiene que ser un video de YouTube`. Nace `CONFIRMADA` |
| `POST /peliculas/{id}/confirmacion` | `CONFIRMADA` y en cartelera |
| `POST /peliculas/{id}/descarte` | `DESCARTADA`; `409` si tiene funciones |
| `PUT /peliculas/{id}` | Parcial: lo que no viaja queda igual. `enCartelera: true` solo publica una `CONFIRMADA`. `trailerYoutube: ""` saca el trailer |
| `DELETE /peliculas/{id}` | `409` si tiene funciones o una programación |
| `GET /salas` · `GET /salas/{id}` | `{id, nombre, tipo, butacasPorFila, filas, capacidadSala, minutosLimpieza}`; el detalle suma `asientos` |
| `POST /salas` | `{nombre, tipo, butacasPorFila, codigosVip, codigosPareja, codigosAccesibles, minutosLimpieza}`. 1 a 26 filas, 1 a 40 butacas por fila; cada código especial existe y está en una sola lista. Limpieza 0 a 120, 15 por defecto. Nombre único (`409`) |
| `PUT /salas/{id}` | `{nombre, tipo, minutosLimpieza}`. Las butacas no se editan; el tipo, tampoco si tiene funciones (`409`). Los datos se validan antes que el nombre repetido |
| `DELETE /salas/{id}` | `409` si tiene funciones o una programación |
| `PATCH /salas/{salaId}/asientos/{codigo}` | `{"estado":"FUERA_DE_SERVICIO"}` o `HABILITADO` (R9) |
| `GET /funciones?peliculaId=&salaId=&desde=&hasta=` | Con película y sala, por `inicio`. `hasta` incluye todo el día |
| `POST /funciones` | Película confirmada, sin superposición en la sala (R3), 3D solo en sala que lo soporta (R8), `inicio` futuro (R20), sin segundos y dentro del próximo año. Dos pedidos a la vez sobre la misma sala y hora: el segundo da `409 Ya hay una función en esa sala a esa hora` |
| `DELETE /funciones/{id}` | `409` si tiene reservas, aun canceladas (R12) |

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
`imagenUrl` es opcional (ver [Imágenes](#imágenes)).

```json
{ "nombre": "Miércoles 2x1", "tipo": "NXM", "lleva": 2, "paga": 1,
  "vigenciaDesde": "2026-08-01", "vigenciaHasta": "2026-12-31",
  "diasSemana": ["WEDNESDAY"], "horaDesde": null, "horaHasta": null, "mediosPago": [],
  "imagenUrl": "/api/imagenes/0f8fad5b-d9cb-469f-a165-70867728950e.webp" }
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
  `DELETE`. `imagenUrl` sale solo si tiene imagen.
- `PUT /promociones/{id}`: mismo cuerpo, validaciones y mensajes que el alta; reemplaza todo (sin `imagenUrl` le
  saca la imagen) menos `activa`. `404` si no existe; `400` `El tipo de una promoción no se puede cambiar`; `409`
  con el nombre de otra. Una promo ya usada en cobros se edita igual: cada pago congeló su descuento.
- No se acumulan: gana el mayor descuento (R15). Las tarifas reducidas quedan afuera (R16).

`GET /promociones/vigentes` (público): las activas que no terminaron, también las que empiezan más adelante,
ordenadas por inicio. Sin `id` ni `activa`; `beneficio` lo arma cada tipo y se muestra tal cual. Que aplique a una
compra lo decide el cobro, no este listado.

```json
[{ "nombre": "Miércoles 2x1", "tipo": "NXM", "beneficio": "2x1",
   "vigenciaDesde": "2026-08-01", "vigenciaHasta": "2026-12-31", "diasSemana": ["WEDNESDAY"],
   "mediosPago": [], "tarifasExcluidas": ["MENOR", "JUBILADO", "ESTUDIANTE"],
   "imagenUrl": "/api/imagenes/0f8fad5b-d9cb-469f-a165-70867728950e.webp" }]
```

`beneficio`: `"2x1"`, `"30 % OFF"`, `"$500 OFF"`. `horaDesde`/`horaHasta` solo si tiene franja; `imagenUrl`, solo
si tiene imagen.

## Candy (CU-13 a CU-16)

| Ruta | Notas |
|---|---|
| `GET /candy/productos?todos=` | La carta; sin `todos=true`, solo lo disponible. Cada uno trae `precioSuelto` y `ahorro` (en un suelto, su precio y `0`) e `imagenUrl` (`null` sin imagen) |
| `GET /candy/productos/{id}` | Un producto |
| `POST /candy/productos` | `{nombre, tipo, precio, imagenUrl}`; un `COMBO` va por `/combos`. Nombre único (`409`) |
| `POST /candy/combos` | `{nombre, precio, componentes: {productoId: cantidad}, imagenUrl}`: dos productos o más, ninguno combo, 1 a 20 de cada uno. Más barato que los componentes sueltos (R14) |
| `PUT /candy/productos/{id}` | `{nombre, precio, imagenUrl}`; sin `imagenUrl` le saca la imagen. `400` si un combo que lo trae deja de convenir (R14) |
| `PATCH /candy/productos/{id}` | `{disponible}`. No hay `DELETE`: el producto vive en compras viejas |
| `POST /candy/compras` | La venta, que nace cobrada |
| `GET /candy/compras?fecha=&clienteId=` | Las del cliente en cualquier estado, o si no las cobradas ese día (por `cobradaEn`) |
| `GET /candy/arqueo?fecha=` | `{fecha, total, compras}`, caja aparte de boletería: las cobradas ese día, también las que se cobraron con una reserva |

En el alta, el combo y la edición, nombre y precio se validan antes que el nombre repetido.

```json
{ "clienteId": 3, "reservaId": 25, "cantidades": { "1": 2, "4": 1 },
  "medio": "EFECTIVO", "codigoAutorizacion": "" }
→ { "id": 8, "estado": "COBRADA", "fecha": "…", "cobradaEn": "…", "medio": "EFECTIVO",
    "items": [{ "productoId": 4, "nombre": "Combo clásico", "cantidad": 1,
                "precioUnitario": 5500, "subtotal": 5500 }],
    "total": 12000, "ahorro": 1500 }
```

- Sin `reservaId` es venta de mostrador. Con `reservaId`, la reserva tiene que estar pagada (`409`) y ser del cliente.
- Cada cantidad de 1 a 20; lo que salió de la carta no se vende. El código, como al cobrar una reserva (R11).
- El pedido y el medio de pago se validan antes de buscar los productos (`404`).
- `ahorro` es el descuento de los combos, congelado al vender.
- `estado`: `PENDIENTE` (pedida en la web, sin cobrar), `COBRADA` o `ANULADA` (su reserva se canceló o venció). La
  de mostrador nace `COBRADA`, con `cobradaEn` igual a `fecha`. Lo pendiente no tiene `medio` ni `cobradaEn`.

## Imágenes

Las fotos de promociones y productos. Primero se sube el archivo y después su `url` va como `imagenUrl`.

| Ruta | Notas |
|---|---|
| `POST /imagenes` | Multipart, campo `archivo`. JPG, PNG o WEBP de hasta 2 MB, reconocido por su contenido y no por la extensión, que la pone el backend. → `201 {"url": "/api/imagenes/<uuid>.<ext>"}` con `Location` |
| `GET /imagenes/{nombre}` | Público. Los bytes con su `Content-Type` y `Cache-Control: max-age=31536000, public, immutable`: cada subida es un uuid nuevo y no cambia. `404` `No existe la imagen X` si no está o el nombre no tiene la forma `<uuid>.<ext>` |

- `400`: `Falta la imagen` (sin `archivo` o vacío), `La imagen tiene que ser JPG, PNG o WEBP`,
  `La imagen tiene que pesar 2 MB como máximo`.
- `imagenUrl` en promociones y productos: vacía o `null` es sin imagen; si no, tiene que ser una `url` de
  `POST /imagenes` (`400` `La imagen tiene que ser una subida al sistema`). No hace falta que el archivo exista.
- Viven en la tabla `imagen` (MEDIUMBLOB, por nombre): un dump de la base se las lleva.

## Caja e informes

| Ruta | Qué da |
|---|---|
| `GET /arqueo?fecha=` | La caja de boletería del día: `total`, `entradas`, `porMedio` y `pagos` |
| `GET /funciones/{id}/bordero` | Lo cobrado de una función: espectadores, bruta, descuentos, neta y `porTarifa` |
| `GET /funciones/{id}/informe` | El borderó más el candy cobrado asociado a reservas de esa función |
| `GET /declaracion-jurada?desde=&hasta=` | Las funciones con entradas cobradas del período, por función, por película y en total, con los datos del exhibidor |

- El borderó corta por función y la declaración jurada por período (INCAA). Sin fechas, la declaración toma
  la semana cinematográfica anterior (jueves a miércoles); `400` con una sola fecha o más de 31 días.
- Los archivos (borderó `.txt`, declaración `.csv`) los genera Swing con estos datos, no el backend.

## Control de acceso (CU-18)

`POST /acceso` `{ "codigo": "K7M2P9XQ" }`: marca la entrada usada y devuelve la reserva. `409` si está sin
pagar (R18) o ya se usó; `400` si no es el día de la función; `404` si no existe. El código tiene 8 caracteres, sin
`O`, `I`, `0` ni `1`, y se busca sin espacios ni distinción de mayúsculas (`k7m2 p9xq` encuentra `K7M2P9XQ`).

### Retiro del candy

`POST /candy/retiros` `{ "codigo": "K7M2P9XQ" }`: el candy escanea el mismo QR de la entrada y marca como retirado
todo lo cobrado de esa reserva que todavía no se retiró (lo de la web y lo del mostrador para la reserva). `200` con
esas compras, cada una con `retiradaEn`. `409` `La entrada no tiene candy para retirar` si no tiene candy cobrado, y
`El candy de esa entrada ya se retiró el 21/10 19:50` si ya se entregó todo; `404` si el código no existe. El código
se busca como en `/acceso`. Toda compra de candy trae `retiradaEn` cuando ya se retiró (antes no viene).

## Importador

| Ruta | Notas |
|---|---|
| `POST /importaciones` | `{ "paginas": 1 }` (1 a 3, veinte títulos cada una). Corre y contesta al terminar |
| `GET /importaciones` | Las últimas 20 |
| `GET /importaciones/estado` | `{disponible, detalle}`, sin consultar TMDB |

```json
→ { "id": 7, "estado": "TERMINADA", "paginas": 2, "nuevas": 9, "salteadas": 20, "fallidas": 1,
    "detalle": "+ [41] Hablan las aves\n= Cars\n✗ Yo, narciso\tLa duración…",
    "peliculas": [
      { "resultado": "NUEVA", "peliculaId": 41, "titulo": "Hablan las aves", "motivo": null,
        "estadoRevision": "PENDIENTE" },
      { "resultado": "YA_ESTABA", "peliculaId": null, "titulo": "Cars", "motivo": null, "estadoRevision": null },
      { "resultado": "RECHAZADA", "peliculaId": null, "titulo": "Yo, narciso", "motivo": "La duración…",
        "estadoRevision": null } ] }
```

- `peliculas` es una por título que trajo TMDB, en el orden en que llegó: `NUEVA` (entró al buzón), `YA_ESTABA`
  (cuenta en `salteadas`) o `RECHAZADA` (cuenta en `fallidas`, con su `motivo`). `estadoRevision` es el de hoy,
  solo en las nuevas; `null` si después se borró. Los clientes leen esta lista, no `detalle`: en una `FALLIDA`,
  `detalle` es el motivo de la falla y `peliculas` viene vacía.

- Cada película trae su trailer de TMDB: un `Trailer` de YouTube, el oficial y más nuevo; en castellano y si no hay, en inglés.
- Una que ya estaba y no tiene trailer lo recibe en la corrida (cuenta en `salteadas`); el que ya tiene, aunque lo haya corregido el encargado, no se toca.
- Lo nuevo entra al buzón como `PENDIENTE`. Una candidata inválida cuenta en `fallidas` y la corrida sigue.
- TMDB caído: `201` con `FALLIDA`. Una corrida trabada pasa a `FALLIDA` a los 5 minutos.
- `409`: otra importación en curso. `400`: la anterior terminó hace menos de 60 segundos.
