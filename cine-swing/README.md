# cine-swing

El panel del encargado (y la Puerta del acomodador) como app de escritorio: Java 21 y Swing
puro, con FlatLaf oscuro de look and feel, JCalendar para elegir fechas y Jackson para el
JSON. Habla con el backend por HTTP con Basic, igual que la web del cliente pero con login:
nunca va directo a MySQL, las reglas siguen solo en el backend y su error se muestra tal cual.
Es la única interfaz del encargado y del acomodador; la web React quedó solo para la venta al
cliente.

## Correrlo

Con el sistema levantado (`cine-docker`, `docker compose up -d --build`):

```bash
mvn exec:java                                   # sin empaquetar
mvn package && java -jar target/cine-swing.jar  # jar con las dependencias adentro
mvn clean test                                  # 60 tests, contra un servidor HTTP falso, sin backend ni pantalla
```

Por defecto apunta a `http://localhost:8080` (nginx del docker, que reenvía `/api`). Otro
servidor: `-Dcine.api.url=http://host:puerto` o la variable `CINE_API_URL`.

Requisitos: JDK 21 y Maven. Tema claro: `-Dcine.tema=claro` (también con `mvn exec:java`).

Demo: `encargado@cine.uade.ar` / `cine2026` ve el menú completo; `puerta@cine.uade.ar` / `cine2026`
(acomodador) ve solo Puerta. Los dos vienen de `cine-docker/seed/02-admin.sql`: no hay alta de
empleados.

El menú: Películas, Por revisar, Importador, Salas, Funciones, Grilla, Planificador, Agenda,
Reservas, Promociones, Candy, Caja, Declaración jurada y Puerta. Las películas no se cargan
sueltas: las trae el Importador desde TMDB y se confirman en Por revisar. Las reglas que más se
notan al operar (bloqueo de 3 minutos, reservas que vencen a los 30, Puerta solo el día de la
función, candy solo con la reserva pagada, topes) están en el
[README de la raíz](../README.md#lo-que-conviene-saber-al-usarlo).

El borderó (`.txt`) y la declaración jurada (`.csv`) se generan en esta PC y se guardan donde
elija el encargado: el backend solo da los números.

## Cómo está armado

Los subdominios son los del backend (`cartelera`, `salas`, `funciones`, `programaciones`, `ventas`,
`candy`, `promociones`, `informes`, `usuarios`): sabiendo el subdominio se sabe el paquete, en
`api/dto/` y en `pantallas/`.

| Paquete | Qué hay |
|---|---|
| raíz | `Principal` (arranque y el ida y vuelta entre login y panel), `VentanaPrincipal` (el marco con el menú) y `Pantallas`, que crea cada pantalla con solo las `Api` que usa. `PantallasTest` arma todas sin backend |
| `api/` | `ClienteHttp`, la mitad de `api-http.js` que no depende de ninguna ruta: Basic, JSON, sesión vencida y el mensaje de error. Una `Api<Subdominio>` por subdominio con las operaciones de `cine-frontend/API.md` y su mismo nombre (`ApiCartelera`, `ApiSalas`, `ApiVentas`…, más `ApiSesion`, `ApiCatalogos` y `ApiClientes`), todas sobre el mismo cliente y juntas en el record `Apis`. `ErrorApi` lleva el mensaje del backend |
| `api/dto/<subdominio>/` | Un record por forma de JSON, espejando `dto/` del backend (más `catalogos/`) |
| `informes/` | Lo que se escribe en la PC: el borderó en `.txt` (mismo formato que generaba el backend), la declaración jurada en `.csv` y el ticket de candy. Sin Swing, con tests |
| `comun/` | `Tarea` (pedidos fuera del EDT), `Validacion` (marca y muestra errores) con `Lecturas` (el formato, sin Swing) y `Marcas` (el borde rojo), `Campos`, `Formulario`, `BarraFiltros`, `Opciones`, `Fechas` (el único cruce `Date` ↔ `java.time`), `Formato` (horas y fechas para mostrar), `Colores` (los del tema, para que el oscuro no se rompa), `Tabla`, `Pila`, `Etiquetas`, `Mensajes` y piezas de pantalla |
| `pantallas/` | Lo común: `Seccion` (pedir y avisar), `Pantalla` (una `Seccion` con margen y encabezado), `PantallaListado` (los listados con filtros y conteo), `Navegacion` y el enum `Destino` |
| `pantallas/<subdominio>/` | Una clase por pantalla y, al lado, sus paneles y formularios package-private (`FormularioPelicula`, `PanelCobro`, `GrillaAgenda`…). El login está en `usuarios/` y la Puerta del acomodador en `ventas/` |

Al tocarlo: los pedidos van siempre por `comun/Tarea`, fuera del EDT; las fechas por
`comun/Fechas` (al backend viajan en ISO) y se muestran con `comun/Formato`; los colores salen de
`comun/Colores` o del `UIManager`, nunca fijos; la navegación entre pantallas usa `Destino`. Una
operación nueva va en la `Api` de su subdominio, con el nombre de `API.md`, y la recibe solo la
pantalla que la usa.

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
| Error de un campo: formato, obligatorio o un 400 o 404 del backend a un formulario | En línea, junto al formulario, con el campo marcado (`Validacion`) |
| Error de negocio que nombra un campo: un 409 como «Ya existe una sala con ese nombre» | Igual que el anterior: en línea y con ese campo marcado |
| Error global: sin conexión, 5xx, 403, un 409 sin campo (la reserva que otro pedido cambió) o un 400 sin formulario | Diálogo de error con el texto del backend (`Mensajes.error`). El 401 vuelve al login |
| Acción destructiva o sin vuelta atrás (borrar, dar de baja, cobrar, cancelar una reserva) | Confirmación con el verbo en el botón, «Sí, borrar» / «Cancelar» (`Mensajes.confirmar`) |
| Éxito | Barra de estado en verde, que se borra sola (`Mensajes.exito`). Sin diálogo |

nginx también contesta en JSON sus propios errores (cuerpo de más de 1 MB, backend caído o lento),
así que esos se muestran tal cual. Si igual llega un error sin el `{"error": "…"}` (un proxy
intermedio, un cuerpo vacío), `ClienteHttp` no lo muestra: lo escribe en la consola y en pantalla
queda un mensaje según el código. El porqué de la política está en el
[manual](../_other/docs/manual/index.html#swing), en «Panel del encargado (Swing)».
