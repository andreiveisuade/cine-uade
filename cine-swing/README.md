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
mvn test                                        # contra un servidor HTTP falso, sin backend
```

Por defecto apunta a `http://localhost:8080` (nginx del docker, que reenvía `/api`). Otro
servidor: `-Dcine.api.url=http://host:puerto` o la variable `CINE_API_URL`.

Requisitos: JDK 21 y Maven. Tema claro: `-Dcine.tema=claro` (también con `mvn exec:java`).

Demo: `encargado@cine.uade.ar` / `cine2026` ve el menú completo; un acomodador ve solo Puerta.

El borderó (`.txt`) y la declaración jurada (`.csv`) se generan en esta PC y se guardan donde
elija el encargado: el backend solo da los números.

## Cómo está armado

| Paquete | Qué hay |
|---|---|
| `api/` | `ApiHttp`, el equivalente de `api-http.js`: una operación por endpoint de `cine-frontend/API.md`. `ErrorApi` lleva el mensaje del backend. `dto/`, un record por forma de JSON |
| `informes/` | Lo que se escribe en la PC: el borderó en `.txt` (mismo formato que generaba el backend), la declaración jurada en `.csv` y el ticket de candy. Sin Swing, con tests |
| `comun/` | `Tarea` (pedidos fuera del EDT), `Validacion` y `Campos` (formularios), `Fechas` (el único cruce `Date` ↔ `java.time`), `Tabla`, `Etiquetas`, `Formato`, `SelectorDias` y piezas de pantalla |
| `pantallas/` | Login, la ventana con el menú (sus entradas son el enum `Destino`) y una clase por pantalla |

## Formularios: qué valida el cliente

Antes de mandar, cada formulario pasa por `comun/Validacion`, que mira solo **formato y
obligatoriedad**: los campos con `*` no pueden quedar vacíos (un `JDateChooser` vacío cuenta como
faltante), los numéricos no dejan tipear letras (`Campos.soloEntero`, `soloDecimal`,
`soloListaDeEnteros`) y una lista como `8,x,12` se rechaza nombrando la `x` en vez de descartarla.
Si algo falla no se manda nada: el campo queda con el borde de error de FlatLaf, el motivo aparece
junto al formulario y el foco va al primero.

Las **reglas de negocio** (precio mayor a cero, rangos, superposición, R1 a R19) no se anticipan:
las decide el backend y su mensaje se muestra tal cual en el mismo lugar, marcando el campo si el
mensaje lo nombra. Tampoco se decide nada por el nombre de una constante: qué tarifa pide carnet y
qué medio va por checkout salen de `requiereAcreditacion` y `requiereAutorizacion` de los catálogos.

Un 401 fuera del login cierra el panel y vuelve al login. Las credenciales viven en memoria
y se van al cerrar la app.
