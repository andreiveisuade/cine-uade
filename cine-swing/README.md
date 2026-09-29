# cine-swing

El panel del encargado (y la Puerta del acomodador) como app de escritorio: Java 21 y Swing
puro, con FlatLaf oscuro de look and feel y JCalendar para elegir fechas. Habla con el backend por HTTP con Basic, igual que la
web del cliente pero con login: las reglas siguen solo en los gestores, y el error del
backend se muestra tal cual. Es la única interfaz del encargado y del acomodador; la web
React quedó solo para la venta al cliente.

## Correrlo

Con el sistema levantado (`cine-docker`, `docker compose up -d --build`):

```bash
mvn exec:java                                   # sin empaquetar
mvn package && java -jar target/cine-swing.jar  # jar con las dependencias adentro
mvn clean test                                  # 58 tests, contra un servidor HTTP falso, sin backend
```

Por defecto apunta a `http://localhost:8080` (nginx del docker, que reenvía `/api`). Otro
servidor: `-Dcine.api.url=http://host:puerto` o la variable `CINE_API_URL`.

Requisitos: JDK 21 y Maven. Tema claro: `-Dcine.tema=claro` (también con `mvn exec:java`).

Demo: `encargado@cine.uade.ar` / `cine2026` ve el menú completo; `puerta@cine.uade.ar` / `cine2026`
(acomodador) ve solo Puerta.

El borderó (`.txt`) y la declaración jurada (`.csv`) se generan en esta PC y se guardan donde
elija el encargado: el backend solo da los números.

## Cómo está armado

Los subdominios son los del backend (`cartelera`, `salas`, `funciones`, `programaciones`, `ventas`,
`candy`, `promociones`, `informes`, `usuarios`): sabiendo el subdominio se sabe el paquete, en
`api/dto/` y en `pantallas/`.

| Paquete | Qué hay |
|---|---|
| raíz | `Principal` (arranque y el ida y vuelta entre login y panel) y `VentanaPrincipal` (el marco con el menú, que crea cada pantalla con solo las `Api` que usa) |
| `api/` | `ClienteHttp`, la mitad de `api-http.js` que no depende de ninguna ruta: Basic, JSON, sesión vencida y el mensaje de error. Una `Api<Subdominio>` por subdominio con las operaciones de `cine-frontend/API.md` (`ApiCartelera`, `ApiSalas`, `ApiVentas`…), todas sobre el mismo cliente y juntas en el record `Apis`. `ErrorApi` lleva el mensaje del backend |
| `api/dto/<subdominio>/` | Un record por forma de JSON, espejando `dto/` del backend |
| `informes/` | Lo que se escribe en la PC: el borderó en `.txt` (mismo formato que generaba el backend), la declaración jurada en `.csv` y el ticket de candy. Sin Swing, con tests |
| `comun/` | `Tarea` (pedidos fuera del EDT), `Validacion` (marca y muestra errores) con `Lecturas` (el formato, sin Swing) y `Marcas` (el borde rojo), `Campos`, `Formulario`, `BarraFiltros`, `Opciones`, `Fechas` (el único cruce `Date` ↔ `java.time`), `Tabla`, `Pila`, `Etiquetas`, `Formato`, `Mensajes` y piezas de pantalla |
| `pantallas/` | Lo común: `Seccion` (pedir y avisar), `Pantalla` (una `Seccion` con margen y encabezado), `PantallaListado` (los listados con filtros y conteo), `Navegacion` y el enum `Destino` |
| `pantallas/<subdominio>/` | Una clase por pantalla y, al lado, sus paneles y formularios package-private (`FormularioPelicula`, `PanelCobro`, `GrillaAgenda`…). El login está en `usuarios/` y la Puerta del acomodador en `ventas/` |

## Formularios: qué valida el cliente

Antes de mandar, cada formulario pasa por `comun/Validacion`, que mira solo **formato y
obligatoriedad**: los campos con `*` no pueden quedar vacíos (un `JDateChooser` vacío cuenta como
faltante), los numéricos no dejan tipear letras (`Campos.soloEntero`, `soloDecimal`,
`soloListaDeEnteros`) y una lista como `8,x,12` se rechaza nombrando la `x` en vez de descartarla.
Si algo falla no se manda nada: el campo queda con el borde de error de FlatLaf, el motivo aparece
junto al formulario y el foco va al primero.

Las **reglas de negocio** (precio mayor a cero, rangos, superposición, R1 a R20) no se anticipan:
las decide el backend y su mensaje se muestra tal cual en el mismo lugar, marcando el campo si el
mensaje lo nombra. Tampoco se decide nada por el nombre de una constante: qué tarifa pide carnet y
qué medio va por checkout salen de `requiereAcreditacion` y `requiereAutorizacion` de los catálogos,
qué tipos de producto se dan de alta sueltos de `esCombo` de `/api/tipos-producto`, qué campos pide
cada promoción de `campos` de `/api/tipos-promocion`, y Cobrar y Cancelar se habilitan con
`cobrable` y `cancelable` de la reserva, calculados con las mismas reglas que aplica el backend.

Un 401 fuera del login cierra el panel y vuelve al login. Las credenciales viven en memoria
y se van al cerrar la app.

## Dónde va cada mensaje

Todo pasa por `comun/Mensajes`; cambiar la política toca ese archivo.

| Tipo de mensaje | Dónde se muestra |
|---|---|
| Error de un campo: formato, obligatorio o un 400 del backend | En línea, junto al formulario, con el campo marcado (`Validacion`) |
| Error global: sin conexión, 500, 409, 403, o un 400 sin formulario | Diálogo de error con el texto del backend (`Mensajes.error`). El 401 vuelve al login |
| Acción destructiva o sin vuelta atrás (borrar, dar de baja, cobrar, cancelar una reserva) | Confirmación con el verbo en el botón, «Sí, borrar» / «Cancelar» (`Mensajes.confirmar`) |
| Éxito | Barra de estado en verde, que se borra sola (`Mensajes.exito`). Sin diálogo |

Si el servidor contesta un error sin el `{"error": "…"}` del backend (el HTML de un nginx con el
backend reiniciando, un cuerpo vacío), `ClienteHttp` no lo muestra: lo escribe en la consola y en
pantalla queda un mensaje según el código. El porqué de la política está en el manual, en
«Panel del encargado (Swing)».
