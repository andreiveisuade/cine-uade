# cine-swing

Panel del encargado y Puerta del acomodador, de escritorio. Java 21 y Swing, con FlatLaf, JCalendar y
Jackson. Habla con el backend por HTTP Basic, como cualquier cliente: nunca va directo a MySQL y las reglas
quedan solo en el backend.

## Correrlo

Requiere JDK 21 y el sistema levantado ([`cine-docker`](../cine-docker/README.md)).

```bash
./mvnw compile exec:java                        # sin empaquetar (Windows: .\mvnw.cmd)
./mvnw package && java -jar target/cine-swing.jar  # jar con las dependencias adentro
./mvnw clean test                               # contra un servidor HTTP falso, sin backend ni pantalla
./mvnw -Phumo test                              # humo: las Api reales contra el backend levantado
```

`exec:java` no compila, por eso el `compile`. El wrapper baja Maven solo; en IntelliJ usar la configuración **Panel Swing**.

El humo (`HumoContraBackendTest`) entra como encargado, lista salas, catálogos y reservas, crea y borra una
película con su función, y prueba el login del acomodador y el rechazo de un cliente. Pide el seed de
`datos-de-ejemplo.sh` (usa una sala `DOS_D`) y deja una sola cuenta fija, `humo-swing@cine.uade.ar`: el backend
no da de baja clientes.

El servidor se elige en el ingreso (Local, Producción u Otro… con su URL) y queda recordado en esta compu;
`-Dcine.api.url=...` o `CINE_API_URL` mandan sobre esa elección. Para cambiarlo, Salir y elegir otro.

Render free puede tardar hasta un minuto en despertar; el login espera. Ahí `cine2026` no vale: las claves son
las que se cargaron con `cargar-aiven.sh`. Tema claro con `-Dcine.tema=claro`. Usuarios de demo:
[Cómo levantarlo](../_other/COMO-LEVANTARLO.md#usarlo).

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
