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
| `informes/` | Lo que se escribe en la PC: el borderó en `.txt` (mismo formato que generaba el backend) y la declaración jurada en `.csv`. Sin Swing, con tests |
| `comun/` | `Tarea` (pedidos fuera del EDT), `Fechas` (el único cruce `Date` ↔ `java.time`), `Tabla`, `Etiquetas`, `Formato` y piezas de pantalla |
| `pantallas/` | Login, la ventana con el menú y una clase por pantalla |

Un 401 fuera del login cierra el panel y vuelve al login. Las credenciales viven en memoria
y se van al cerrar la app.
