# cine-swing

Panel del encargado y Puerta del acomodador, de escritorio. Java 21 y Swing, con FlatLaf, JCalendar y
Jackson. Habla con el backend por HTTP Basic, como cualquier cliente: nunca va directo a MySQL y las reglas
quedan solo en el backend.

## Correrlo

Requiere JDK 21, Maven y el sistema levantado ([`cine-docker`](../cine-docker/README.md)).

```bash
mvn exec:java                                   # sin empaquetar
mvn package && java -jar target/cine-swing.jar  # jar con las dependencias adentro
mvn clean test                                  # contra un servidor HTTP falso, sin backend ni pantalla
```

Apunta a `http://localhost:8080`; otro servidor con `-Dcine.api.url=...` o `CINE_API_URL`. Tema claro con
`-Dcine.tema=claro`. `encargado@cine.uade.ar` / `cine2026` ve todo; `puerta@cine.uade.ar` / `cine2026`,
solo Puerta.

## Paquetes

Los subdominios son los del backend: sabiendo el subdominio se sabe el paquete.

| Paquete | Qué hay |
|---|---|
| raíz | `Principal` (arranque y login), `VentanaPrincipal` (el marco) y `Pantallas`, que arma cada pantalla con solo las `Api` que usa |
| `api/` | `ClienteHttp` (Basic, JSON, 401 y el `{error}`) y una `Api<Subdominio>` por subdominio, con los nombres de [`API.md`](../cine-frontend/API.md) |
| `api/dto/<subdominio>/` | Un record por forma de JSON, espejando `dto/` del backend |
| `informes/` | Borderó `.txt`, declaración jurada `.csv` y ticket de candy: se generan en esta PC, el backend solo da los números |
| `comun/` | Piezas compartidas: `Tarea`, `Validacion`, `Lecturas`, `Fechas`, `Formato`, `Colores`, `Mensajes`… |
| `pantallas/` | `Seccion`, `Pantalla`, `PantallaListado` (template method de los listados), `Navegacion` y `Destino` |
| `pantallas/<subdominio>/` | Una clase por pantalla, con sus paneles y formularios al lado |

## Al tocarlo

- Pedidos siempre por `comun/Tarea`, fuera del EDT.
- Fechas por `comun/Fechas` (al backend viajan en ISO) y se muestran con `comun/Formato`.
- Colores de `comun/Colores` o del `UIManager`, nunca fijos: si no, el tema oscuro se rompe.
- El cliente valida solo formato y obligatoriedad (`comun/Validacion`). Las reglas de negocio las decide el
  backend y su mensaje se muestra tal cual. Nada se decide por el nombre de una constante: se usan los campos
  que manda el backend (`requiereAcreditacion`, `requiereAutorizacion`, `cobrable`…).
- Los mensajes pasan por `comun/Mensajes`: error de un campo en línea, diálogo solo para errores globales y
  confirmaciones, éxito en la barra de estado. El porqué, en el [manual](../_other/docs/manual/index.html#swing-mensajes).
- Una operación nueva va en la `Api` de su subdominio, con el nombre de `API.md`.
