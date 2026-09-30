# Pruebas

Cómo se verifica el sistema antes de entregar o de mergear a `main`. Son cinco niveles, del más barato al más caro.
Cada uno ve algo que el anterior no ve, y ninguno reemplaza al otro:

| Nivel | Qué ve | Cuánto tarda | Dónde |
|---|---|---|---|
| 1. Suite del backend | Reglas, API, mapeo contra el schema real, arquitectura | ~1 min | `cine-backend/src/test` |
| 2. Suite de Swing y build de la web | Pantallas de Swing sin backend; que la web compile | ~30 s | `cine-swing/src/test`, `npm run build` |
| 3. Colección de Postman | La API por nginx, contra MySQL real, con los verbos que pide la consigna | ~10 s | `_other/demo/cine-uade.postman_collection.json` |
| 4. Humo de punta a punta | Reglas y topes nuevos pasando por nginx, Docker y MySQL | ~15 s | `_other/demo/humo.sh` |
| 5. Recorrida visual | Que la web y Swing muestren lo que la API responde | ~15 min | Planilla de abajo |

Los niveles 3 a 5 escriben datos: se corren contra una base desechable, nunca contra una que importe (ver
[Base desechable](#base-desechable)).

## 1. Suite del backend

```bash
cd cine-backend && mvn test          # 1149 casos, 0 fallas
mvn test -Dtest=GestorPagosTest      # una clase
```

Levanta el contexto de Spring contra H2 en modo MySQL, creado con el `schema.sql` real: los `UNIQUE`, los
`NOT NULL` y los largos de columna son los mismos que en producción. No hace falta Docker.

| Capa | Clases | Casos | Qué prueba |
|---|---|---|---|
| `model` | 23 | 397 | Entidades, validadores, Value Objects y estados, sin Spring: cada regla con su mensaje exacto |
| `service` | 17 | 378 | Gestores con la base de H2: transacciones, orden de los errores, comprobantes después del commit |
| `controller` | 26 | 303 | La API por HTTP en un puerto libre (`PruebaDeApi`): status, cuerpo `{error}`, seguridad por rol |
| `infrastructure` | 3 | 44 | Importador de TMDB con un catálogo falso, encoder de contraseñas |
| raíz | 4 | 20 | `ArquitecturaTest` (quién importa a quién), `MapeoContraSchemaTest`, migraciones, arranque |
| `dto` y `repository` | 2 | 7 | Presencia en los DTO, `Repositorio.exigir` |

Dos clases base: `PruebaDeIntegracion` (el sistema armado, 22 clases la extienden) y `PruebaDeApi` (además la
API levantada, 19 clases). Los casos con varias entradas de la misma forma son `@ParameterizedTest` con
`@CsvSource`, con una primera columna que nombra el caso.

Tres pruebas cuidan cosas que un test de regla no ve:
- `ArquitecturaTest` lee los `import` y falla si una capa nombra a otra que no tiene debajo.
- `MapeoContraSchemaTest` valida las entidades contra el `schema.sql`, lo mismo que `ddl-auto: validate` hace al
  desplegar.
- `VistasVentasTest` cuenta las consultas con las estadísticas de Hibernate y falla si crecen con las filas (N+1).

No correr dos `mvn test` a la vez sobre el mismo `target/`.

## 2. Suite de Swing y build de la web

```bash
cd cine-swing && mvn test            # 60 casos, contra un servidor HTTP falso, sin backend
cd cine-frontend && npm run build    # compila la web; un error de sintaxis o un import roto corta acá
```

`PantallasTest` arma cada pantalla del menú en modo headless contra un servidor que contesta 401 a todo, y
falla si algún callback tira una excepción en el hilo de Swing. Los demás prueban el transporte (`ClienteHttp`,
las `Api*`), la lectura de los formularios (`Lecturas`, `Validacion`) y los archivos que genera el encargado
(borderó, declaración jurada, ticket de candy).

## Base desechable

Los niveles 3 a 5 necesitan el sistema entero levantado. Para no tocar la base de trabajo, se levanta como un
proyecto de Docker aparte, con su propio volumen:

```bash
cd cine-docker
cp .env.example .env                 # si no existe
docker compose -p cine-prueba up -d --build
./seed/datos-de-ejemplo.sh           # salas, candy y una promo
# ... niveles 3 a 5 ...
docker compose -p cine-prueba down -v   # borra solo la base de prueba
```

Si ya hay contenedores del proyecto `cine-docker` (aunque estén parados), los nombres fijos de `container_name`
chocan. Sin tocar el compose del repo, se agrega un override que los saca:

```yaml
# sin-nombres.yml
services:
  mysql: { container_name: !reset null }
  backend: { container_name: !reset null }
  frontend: { container_name: !reset null }
  adminer: { container_name: !reset null }
```

```bash
docker compose -p cine-prueba -f docker-compose.yml -f sin-nombres.yml up -d --build
```

Levantar desde un volumen vacío prueba además lo que la suite no ve: que MySQL cree el `schema.sql` y que
Hibernate lo valide al arrancar (`ddl-auto: validate`).

## 3. Colección de Postman

```bash
npx newman run _other/demo/cine-uade.postman_collection.json     # 41 pedidos, 41 aserciones
```

O importarla en Postman y correrla en orden («Run collection»). Recorre lo que la consigna pide demostrar:
consultas con `GET`, altas con `POST`, modificación con `PUT` y `PATCH`, bajas con `DELETE`, casos exitosos y
casos con datos inválidos o recursos inexistentes (400, 404, 405, 409). Las fechas de las funciones son relativas
a hoy (un script de la colección las calcula), así la colección no vence.

## 4. Humo de punta a punta

```bash
_other/demo/humo.sh                  # 21 casos; sale con código 1 si alguno falla
BASE=http://otro:8080 _other/demo/humo.sh
```

Arma una película, una función y una reserva, y recorre las reglas y los topes que más fácil se rompen entre
capas:
- 11 butacas, un decimal en un entero, una tarifa inválida, una butaca ya vendida;
- candy para una reserva sin pagar y pagada, efectivo con código, 21 unidades de un producto;
- la Puerta antes del día de la función;
- una función a más de un año o con segundos, un año `+999999999`, una programación de dos años;
- la duración y la limpieza desbordadas, un porcentaje de 99,999;
- el firewall, un cuerpo de 2 MB (el 413 de nginx), una película pendiente y un 401, todos en JSON.

## 5. Recorrida visual

La hace una persona, o un agente con el browser de cmux para la web y computer use para Swing. Cada paso
tiene lo que se espera ver; si algo no coincide, se anota con captura.

### Web (`http://localhost:8080`)

| # | Paso | Se espera |
|---|---|---|
| W1 | Abrir la cartelera | Las películas con funciones, con filtro por género |
| W2 | Entrar a una función | Mapa de butacas con la leyenda; las ocupadas grises y deshabilitadas |
| W3 | Elegir 10 butacas y después una más | La 11 no queda elegida y aparece «Una compra tiene que tener como máximo 10 butacas» |
| W4 | Continuar y confirmar con el formulario vacío | El navegador marca los campos obligatorios y no envía |
| W5 | Confirmar con el email `ana@mal` | «El email tiene que tener la forma usuario@dominio.com», del backend, tal cual |
| W6 | Confirmar con datos válidos | Ticket con código de acceso; el texto dice «2D subtitulada» y «Reservada», nunca `DOS_D` ni `RESERVADA` |
| W7 | Mis reservas con un código que no existe | «No existe ninguna reserva con ese código» |
| W8 | Mis reservas con el código en minúsculas y con espacios | Encuentra la reserva |

### Swing (`java -Dcine.api.url=http://localhost:8080 -jar cine-swing/target/cine-swing.jar`)

| # | Paso | Se espera |
|---|---|---|
| S1 | Ingresar con clave equivocada | «Email o contraseña incorrectos», en línea |
| S2 | Ingresar como `encargado@cine.uade.ar` / `cine2026` | El panel con todo el menú |
| S3 | Películas: alta sin título | El campo marcado en rojo y el pedido no sale |
| S4 | Películas: alta con duración 700 | «La duración tiene que estar entre 1 y 600 minutos», junto al formulario |
| S5 | Salas: alta con limpieza 500 | «La limpieza no puede durar más de 120 minutos» |
| S6 | Funciones: una a más de un año | «La función tiene que empezar dentro del próximo año» |
| S7 | Reservas: cobrar en efectivo | El cobro sale y la reserva pasa a pagada; el éxito va a la barra de estado |
| S8 | Candy: vender para una reserva sin pagar | «La reserva N no está pagada: cobrala antes de agregarle candy» |
| S9 | Promociones: porcentaje 99,999 y después 50,555 | «El porcentaje tiene que estar entre 1 y 99»; con 50,555, «El porcentaje tiene que tener como máximo 2 decimales» |
| S10 | Caja del día | Boletería y candy, cada una con su total |
| S11 | Salir e ingresar como `puerta@cine.uade.ar` | Solo la pantalla de Puerta |
| S12 | Puerta con el código de una reserva de otro día | «La función es el dd/MM: se entra solo ese día» |

### Manual

`cd _other/docs/manual && python3 build.py` y abrir `index.html`: cada figura muestra su diagrama o su captura
(ninguna con texto `data:image` ni con el aviso «Syntax Error» de PlantUML).

## Última corrida

29 y 30/09/2026, rama `fix/verificacion-final`, base desechable `cine-prueba` levantada desde cero.

| Nivel | Resultado |
|---|---|
| 1. Backend | 1161 casos, 0 fallas |
| 2. Swing y web | 60 casos, 0 fallas; la web compila |
| 3. Postman | 41 pedidos, 41 aserciones |
| 4. Humo | 21 de 21 |
| 5. Web | W1 a W8 como se espera (browser de cmux) |
| 5. Swing | S1 a S12 como se espera, a mano; capturas en [`evidencia/`](evidencia/) |

Lo que encontró esta corrida, y ya está arreglado:
- La colección de Postman creaba funciones en 2030, fuera del horizonte de un año nuevo: la creación daba 400 y
  arrastraba al resto. Ahora usa fechas relativas a hoy.
- El ticket de la web y los `.txt` mostraban nombres de constantes (`DOS_D`, `SUBTITULADA`, `RESERVADA`).
- `GET /api/peliculas/{id}/funciones` delataba las películas pendientes con un `[]` donde el detalle da 404.
- Las 13 capturas del manual salían como texto base64, y tres diagramas dibujaban un aviso de PlantUML.
- La auditoría del código: 11 butacas con una ocupada daban 409 en vez del 400 del tope, el orden de los errores
  del candy no era el del alta, código muerto del Observer y mayúsculas sin `Locale.ROOT`.
- Swing: el alta de sala no decía cómo se escriben las filas (el formato estaba solo en un tooltip), el resumen
  decía «1 filas (A–A)», los errores locales terminaban con punto y el ticket de candy partía el renglón de `=`.

Anotado para después, sin arreglar:
- El alta de sala en Swing con un editor visual en vez de tipear listas (en los pendientes del manual).
- Todos los errores de un pedido juntos (patrón Notification): documentado en el manual como decisión.
- El formulario de promociones deja huecos donde van los campos de los otros tipos.
- El subtítulo de Candy dice «sin reserva de por medio» y el formulario ofrece asociar una reserva.

Una reserva sin pagar vence a los 30 minutos (R17), pero el estado cambia recién cuando alguien consulta la
función o intenta cobrarla: una reserva vieja puede figurar RESERVADA y el cobro la rechaza por vencida.
