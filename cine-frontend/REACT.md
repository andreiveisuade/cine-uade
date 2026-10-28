# React con el código de cine-frontend

Guía para estudiar React con nuestro front como ejemplo. Va de lo básico a lo fino; cada concepto tiene qué es,
dónde lo usamos (archivo y línea), el error típico y la pregunta que puede hacer el profe. Las rutas son relativas a
`cine-frontend/src/`.

## 1. JSX y componentes

**Qué es.** Un componente es una función que recibe datos y devuelve qué dibujar. JSX es la sintaxis parecida a
HTML que Vite compila a llamadas de JavaScript; entre `{}` va cualquier expresión.

**Dónde lo usamos.** `cartelera/TarjetaPelicula.jsx:12`

```jsx
export function TarjetaPelicula({ pelicula, previa }) {
  return (
    <Box component={Link} to={`/pelicula/${pelicula.id}`} display="block" c="inherit" style={{ textDecoration: "none" }}>
      <Box pos="relative" style={{ borderRadius: "var(--mantine-radius-md)", overflow: "hidden", boxShadow: "var(--cine-sombra)" }}>
        <Poster pelicula={pelicula} />
        {previa}
      </Box>
      <Text fw={650} fz="lg" mt="sm" lh={1.2} lineClamp={2}>{pelicula.titulo}</Text>
```

**Error típico.** Poner un `if` dentro del JSX (no es expresión) o devolver dos elementos sueltos sin envolverlos.
Para condicionar usamos `&&` o el ternario, y `<>…</>` para agrupar sin agregar un nodo.

**Pregunta del profe.** ¿JSX es HTML? No: es JavaScript; `className`, `style={{}}` y `onClick` son props, y un
componente con mayúscula es una función nuestra, con minúscula una etiqueta del DOM.

## 2. Props y callbacks (`alVerbo`)

**Qué es.** Las props son los parámetros del componente: bajan del padre al hijo y el hijo no las modifica. Para
que el hijo avise algo, el padre le pasa una función; en el repo se llaman `alVerbo`.

**Dónde lo usamos.** `compra/funcion/Funcion.jsx:48` y `compra/mapa/BotonButaca.jsx:12`

```jsx
          <MapaButacas sala={funcion.sala} asientos={funcion.asientos}
            pintar={(a) => pintarParaComprar(a, butacas)} alElegir={alternar} />
```

```jsx
      onClick={() => alElegir?.(asiento)}
```

**Error típico.** Que el hijo cambie lo que recibió (`asiento.elegido = true`) o decida reglas que son del padre.
Acá la butaca no sabe si está elegida: el color se lo pasa `pintar` y el clic lo resuelve `Funcion`.

**Pregunta del profe.** ¿Cómo sube un dato del hijo al padre? Llamando a una función que el padre le pasó por
prop (`alElegir(asiento)`); el estado vive en el padre.

## 3. Estado con `useState` e inmutabilidad

**Qué es.** `useState` guarda un valor que sobrevive entre renders y, al cambiarlo con el setter, vuelve a dibujar
el componente. React compara por referencia: hay que crear un objeto o arreglo nuevo, no mutar el que hay.

**Dónde lo usamos.** `compra/funcion/useAlternarButaca.js:21` y `compartido/estado/Avisos.jsx:11`

```js
    const nuevas = butacas[asiento.codigo]
      ? sinButacas(butacas, [asiento.codigo])
      : { ...butacas, [asiento.codigo]: "GENERAL" };
    // Se pinta antes del bloqueo para que se vea elegida en el acto.
    setSeleccion({ funcionId: funcion.id, butacas: nuevas });
```

```js
    setAvisos((lista) => [...lista, { id, mensaje, tipo }]);
    setTimeout(() => setAvisos((lista) => lista.filter((a) => a.id !== id)), 3500);
```

**Error típico.** `butacas[codigo] = "GENERAL"; setSeleccion(seleccion)`: es el mismo objeto y no se redibuja.
Otro: usar el valor de afuera en un callback tardío; por eso el `setTimeout` usa la forma `setAvisos((lista) => …)`,
que recibe la lista de ese momento.

**Pregunta del profe.** ¿Por qué `{ ...butacas }` y no `push`? Porque React detecta el cambio comparando
referencias; un objeto nuevo es la señal de que algo cambió.

## 4. Render y re-render

**Qué es.** Renderizar es llamar a la función del componente. Se repite cuando cambia su estado, cuando el padre se
vuelve a dibujar o cuando cambia un contexto que lee. El render tiene que ser puro: calcular, no hacer cosas.

**Dónde lo usamos.** `compra/funcion/Funcion.jsx:30`: la selección de otra función se descarta derivándola en el
render, no con un efecto.

```jsx
  // Otra función es otra compra: lo elegido para la anterior no se arrastra. Se deriva acá y no con un efecto que
  // la vacíe; el primer clic la reemplaza porque `alternar` escribe la selección entera con este funcionId.
  const butacas = funcion && seleccion.funcionId === funcion.id ? seleccion.butacas : {};
```

**Error típico.** Nos pasó: antes había un `useEffect` que hacía `setSeleccion` al cambiar de función (commit
`b5ad4d7`). Eso dibuja una vez con lo viejo y otra con lo nuevo. Si un valor sale de otros, se calcula en el render.
También nos pasó escribir una ref en el render (`05efd8d`): rompe la pureza.

**Pregunta del profe.** ¿Qué dispara un re-render? Un `set` de estado, un padre que se redibuja o un contexto que
cambia. Cambiar una variable común o una ref no redibuja.

## 5. Listas y `key`

**Qué es.** Para dibujar una lista se hace `map` a elementos. Cada uno lleva una `key` estable y única entre
hermanos, para que React sepa cuál es cuál cuando la lista cambia.

**Dónde lo usamos.** `cartelera/pelicula/ListaFunciones.jsx:10`

```jsx
      {porDia(funciones).map(([clave, delDia]) => (
        <Stack key={clave} gap="xs">
          <Text size="sm" fw={700} tt="uppercase" c="dimmed" style={{ letterSpacing: "0.04em" }}>{dia(delDia[0].inicio)}</Text>
          {delDia.map((f) => <Horario key={f.id} funcion={f} />)}
        </Stack>
      ))}
```

**Error típico.** Usar el índice como `key` en una lista que se reordena o filtra: el estado interno queda pegado
al lugar y no al ítem. Lo usamos solo en `compra/mapa/MapaButacas.jsx:25` (`key={i}`), donde las filas de la sala
son fijas. La `key` también sirve para forzar un remontaje: `app/Rutas.jsx:29` usa `key={seccion}`.

**Pregunta del profe.** ¿Por qué no el índice? Porque si la lista cambia de orden, React reutiliza el componente
equivocado; con el id, cada ítem conserva el suyo.

## 6. Eventos y formularios controlados

**Qué es.** Un input controlado muestra lo que dice el estado y cada tecla actualiza ese estado; el estado es la
única verdad. El `onSubmit` del `<form>` hace `preventDefault()` para que el navegador no recargue la página.

**Dónde lo usamos.** `compra/confirmar/FormularioCliente.jsx:13` y `:30`

```jsx
  function confirmar(evento) {
    evento.preventDefault();
    const vacios = faltantes({
      nombre: { valor: nombre, etiqueta: "tu nombre" },
      email: { valor: email, etiqueta: "tu email" },
    });
```

```jsx
          <TextInput label="Nombre" required value={nombre} error={faltan.nombre}
            onChange={(e) => setNombre(e.currentTarget.value)} />
```

**Error típico.** Olvidar `preventDefault` (la página se recarga y se pierde la selección, que está en memoria) o
poner `value` sin `onChange` (el campo queda de solo lectura). Nos pasó mandar campos en blanco (`ecf58b8`): por eso
`faltantes` y el `trim()`. Validar acá es comodidad; la regla la valida el backend.

**Pregunta del profe.** ¿Controlado o no controlado? Controlado: el valor vive en `useState`, así se puede validar y
armar el pedido sin leer el DOM.

## 7. `useEffect` y su limpieza

**Qué es.** Un efecto sincroniza el componente con algo de afuera (red, timers, `document.title`). Corre después
del render; las dependencias dicen cuándo volver a correr y la función que devuelve limpia lo anterior.

**Dónde lo usamos.** `compartido/hooks/useCargar.js:11`: cada pedido tiene su `vigente`; si cambian las deps antes
de que llegue la respuesta, la limpieza la marca vieja y no pisa a la nueva (race condition).

```js
  useEffect(() => {
    let vigente = true;
    pedir().then(
      (datos) => vigente && setRespuesta({ clave, vuelta, datos, error: null }),
      (e) => vigente && setRespuesta({ clave, vuelta, datos: null, error: e.message }),
    );
    return () => {
      vigente = false;
    };
```

Las deps son `[clave, vuelta]` (`:23`): `clave` es `JSON.stringify(dependencias)` y `vuelta` sube con `recargar`.
Otra limpieza: `ticket/pago/useEsperarPago.js:17` hace `clearInterval`.

**Error típico.** Sin limpieza, ir rápido de la película 3 a la 4 puede mostrar la 3 si su respuesta llega última.
Faltar una dependencia deja el efecto con valores viejos; poner un objeto creado en el render lo corre en cada render.
Nos pasó que `useCargar` mostraba los datos del id anterior mientras cargaba (`2b782ee`): ahora la respuesta
guarda su `clave` y `datos` vale `null` si no coincide.

**Pregunta del profe.** ¿Qué resuelve `vigente`? Que una respuesta vieja no pise a la nueva; `fetch` no se cancela,
pero su resultado se ignora.

## 8. Hooks propios

**Qué es.** Una función que empieza con `use` y llama a otros hooks: saca lógica con estado de la pantalla y la
reutiliza. Reglas: se llaman siempre, en el mismo orden, arriba del componente; nunca dentro de `if`, bucles ni
callbacks. El prefijo `use` es lo que le dice al lint que aplique esas reglas.

**Dónde lo usamos.** `compartido/hooks/useTitulo.js:3` (la lista entera está en el README).

```js
export function useTitulo(texto) {
  useEffect(() => {
    const anterior = document.title;
    if (texto) document.title = `${texto} · Cine UADE`;
    return () => {
      document.title = anterior;
    };
  }, [texto]);
}
```

El lint es `eslint.config.js`, solo con `eslint-plugin-react-hooks`; `npm run lint` lo corre.

**Error típico.** Llamar un hook después de un `return` temprano. Por eso en `compra/funcion/Funcion.jsx:31-36`
`useAlternarButaca`, `useRenovarBloqueo` y `useTitulo` van antes del `if (!funcion) return`, y reciben `undefined`
mientras carga.

**Pregunta del profe.** ¿Por qué el orden importa? React identifica cada hook por su posición en la llamada; si un
`if` saltea uno, el siguiente recibe el estado ajeno.

## 9. Context vs props

**Qué es.** Un contexto deja un valor disponible para todo el árbol de abajo sin pasarlo de prop en prop. Lo usamos
para lo que necesitan pantallas lejanas: la sesión, la compra en curso y los avisos.

**Dónde lo usamos.** `cuenta/SesionDelCliente.jsx:8` (`useSesion`); igual en `compra/CompraEnCurso.jsx:7`
(`useCompra`) y `compartido/estado/Avisos.jsx:6` (`useAvisar`).

```jsx
export function SesionDelCliente({ children }) {
  const [usuario, setUsuario] = useState(undefined);

  useEffect(() => {
    api.sesionActual().then(setUsuario, () => setUsuario(null));
  }, []);

  return <Contexto.Provider value={{ usuario, setUsuario }}>{children}</Contexto.Provider>;
}

export const useSesion = () => useContext(Contexto);
```

`undefined` es «preguntando» y `null` «nadie»: `app/layout/EnlacesDeCuenta.jsx:25` no dibuja nada mientras pregunta.

**Error típico.** Meter todo en contexto: cada cambio redibuja a todos los que lo leen. Lo de una pantalla va por
props (`Funcion` → `MapaButacas` → `BotonButaca`). Usar `useSesion()` fuera del provider devuelve `null`.

**Pregunta del profe.** ¿Por qué no Redux? Son tres datos globales chicos y la verdad está en el backend; contexto
más `useState` alcanza.

## 10. `useEffectEvent`

**Qué es.** Hook de React 19.2 para la lógica que corre dentro de un efecto y tiene que leer el último valor, sin
que ese valor sea dependencia y rearme el efecto.

**Dónde lo usamos.** `compra/useRenovarBloqueo.js:11`

```js
  const sostenerActual = useEffectEvent(async () =>
    seleccion.funcionId === funcionId ? sostenerSeleccion(funcionId, seleccion.butacas) : []);

  useEffect(() => {
    if (!funcionId) return;
    const renovacion = setInterval(async () => {
      const rechazadas = await sostenerActual().catch(() => []);
```

**Error típico.** Poner `seleccion` en las deps: cada clic en una butaca reinicia el intervalo y el minuto vuelve a
cero. Antes lo resolvíamos con una ref escrita en el render (`05efd8d` la sacó: rompe la pureza del render).

**Pregunta del profe.** ¿Por qué no una ref? La ref habría que actualizarla en el render; `useEffectEvent` lee el
valor actual sin eso y el lint lo entiende.

## 11. `useCallback`, `useMemo` y refs

**Qué es.** `useCallback` devuelve la misma función entre renders; `useMemo`, el mismo valor calculado. `useRef`
guarda algo que sobrevive entre renders sin redibujar: un nodo del DOM o un timer.

**Dónde lo usamos.** `useCallback` en `compartido/estado/Avisos.jsx:9` y `compartido/hooks/useCargar.js:25`: son
funciones que otros ponen en deps de efectos. `useMemo` no lo usamos. Refs en `cartelera/usePrevia.js:10`:

```js
  const espera = useRef(null);
  function entrar(pelicula) {
    if (!conMouse || !pelicula.trailerYoutube) return;
    clearTimeout(espera.current);
    espera.current = setTimeout(() => setActual(pelicula.id), ESPERA_PREVIA);
  }
```

Otras: `cartelera/Trailer.jsx:11` y `compra/mapa/useScrollCentrado.js:5` (nodo del DOM), `ticket/pago/useFestejarPago.js:7`.

**Error típico.** Envolver todo en `useMemo`/`useCallback` «por las dudas»: suma código sin medir nada. Y leer o
escribir `ref.current` en el render, porque no redibuja (`compra/confirmar/useReservar.js:16` usa estado por eso).

**Pregunta del profe.** ¿Ref o estado? Si el render lo muestra, estado; si solo lo usan los handlers o efectos (un
timer), ref.

## 12. StrictMode

**Qué es.** En desarrollo, React monta, desmonta y vuelve a montar cada componente, y corre dos veces los efectos.
Sirve para ver efectos que no limpian. En el build de producción no hace nada.

**Dónde lo usamos.** `app/main.jsx:8`

```jsx
  // En dev monta, desmonta y vuelve a montar: deja a la vista los efectos que no limpian.
  <StrictMode>
```

Y el efecto que lo tiene en cuenta, `ticket/pago/useFestejarPago.js:16`:

```js
      // Se marca al lanzar y no al entrar: StrictMode desmonta y vuelve a correr el efecto, y el primero se descarta.
      celebrada.current = codigo;
```

**Error típico.** «En dev se piden dos veces los datos, saco StrictMode». El doble pedido no molesta porque
`useCargar` descarta el primero con `vigente`; si algo se rompe, el efecto está mal.

**Pregunta del profe.** ¿Por qué ves dos pedidos en la red en dev? Por StrictMode; en producción es uno, y el
código tolera los dos porque cada efecto limpia.

## 13. Error boundary

**Qué es.** Un componente que atrapa las excepciones del render de sus hijos y muestra otra cosa en vez de dejar la
página en blanco. Solo existe como clase: no hay hook para `getDerivedStateFromError`.

**Dónde lo usamos.** `app/ErrorDePantalla.jsx:7`, envolviendo `<Rutas />` en `app/AppCliente.jsx:28`.

```jsx
export class ErrorDePantalla extends Component {
  state = { roto: false };

  static getDerivedStateFromError() {
    return { roto: true };
  }
```

**Error típico.** Esperar que atrape un error de un `fetch` o de un `onClick`: no, solo el render. Los errores de
red los muestra `EsperaOError` y los de handlers, `avisar`. Y no resetearlo: el botón hace `setState({ roto: false })`
(`app/ErrorDePantalla.jsx:25`), si no la pantalla de destino seguiría tapada.

**Pregunta del profe.** ¿Por qué una clase si todo lo demás son funciones? Es la única API de React para atrapar
errores de render.

## 14. React Router: `HashRouter`, `useParams`, `useNavigate`

**Qué es.** El router elige qué pantalla mostrar según la URL sin recargar. `HashRouter` pone la ruta después del
`#`; `useParams` lee los `:parametros`; `useNavigate` cambia de pantalla desde código.

**Dónde lo usamos.** `app/main.jsx:11`, `app/Rutas.jsx:33`, `compra/funcion/Funcion.jsx:22` y
`compra/confirmar/useReservar.js:33`.

```jsx
          <Route path="pelicula/:id" element={<Pelicula />} />
          <Route path="funcion/:id" element={<Funcion />} />
```

```jsx
  const { id } = useParams();
  const navegar = useNavigate();
```

```js
      navegar(`/ticket/${reserva.codigo}`, { state: { reserva } });
```

**Error típico.** `<a href>` en vez de `<Link>`: recarga la página entera y pierde la compra en memoria. Y que la
pantalla no se remonte al cambiar `:id`: el estado viejo sigue ahí, por eso `useCargar` compara la `clave`.

**Pregunta del profe.** ¿Por qué `HashRouter`? nginx sirve un solo `index.html`; con `#` el servidor nunca ve la
ruta y recargar `#/ticket/ABC` no da 404.

## 15. Mantine: componentes y tema

**Qué es.** Biblioteca de componentes (botones, inputs, grillas) con props de estilo (`p`, `mt`, `c`) y un tema
central con colores, tipografía y espaciados que todos usan.

**Dónde lo usamos.** `app/tema.js:9`, montado en `app/Proveedores.jsx:17` con `MantineProvider`.

```js
export const tema = createTheme({
  colors: { ambar, dark },
  primaryColor: "ambar",
  primaryShade: { light: 5, dark: 4 },
  autoContrast: true,
```

Los tokens propios (`--cine-seccion`, `--cine-superficie`) están en `app/tema.css`.

**Error típico.** Colores y tamaños sueltos en cada componente: cambiar la marca obliga a tocar cien archivos. Acá
se usan `gap="lg"` o `var(--cine-borde)`.

**Pregunta del profe.** ¿Cómo cambia el modo oscuro? `MantineProvider` con `defaultColorScheme="auto"` y la
preferencia guardada en `localStorage` (`cine-tema`); los componentes leen variables CSS que cambian solas.

## 16. Motion: `LazyMotion`, `m`, `reducedMotion`

**Qué es.** Biblioteca de animaciones. `LazyMotion` baja las funciones de animación en un archivo aparte; `m.div`
es la versión liviana de `motion.div` que las usa; `MotionConfig reducedMotion="user"` respeta la preferencia del
sistema.

**Dónde lo usamos.** `app/Proveedores.jsx:10` y `:19`

```jsx
const animaciones = () => import("./animaciones.js").then((m) => m.default);
```

```jsx
      <LazyMotion features={animaciones} strict>
        <MotionConfig reducedMotion="user">
```

Lo usan `app/Rutas.jsx:28` (`AnimatePresence` entre pantallas) y `compra/mapa/MapaButacas.jsx:23` (las filas entran
de a una).

**Error típico.** Usar `motion.div` con `LazyMotion strict`: tira error, porque cargaría todo de entrada. Y animar
sin respetar «reducir movimiento».

**Pregunta del profe.** ¿Para qué `LazyMotion`? Para que el primer pintado no espere el código de animación; llega
después en otro chunk.

## 17. Tests con Vitest y Testing Library

**Qué es.** Vitest corre los tests; Testing Library monta el componente en un DOM falso (jsdom) y lo usa como
una persona: busca por texto o rol, hace clic y mira qué aparece. La API se reemplaza con `vi.mock`.

**Dónde lo usamos.** `compra/funcion/Funcion.test.jsx:8` y `:22`; `test/renderConApp.jsx:11` monta los mismos
providers que la app, con `MemoryRouter` en vez de `HashRouter`.

```jsx
vi.mock("@/api/api-http.js");
```

```jsx
beforeEach(() => {
  api.sesionActual.mockRejectedValue(new Error("401"));
  api.obtenerFuncion.mockResolvedValue(FUNCION);
  renderConApp(<Funcion />, { ruta: "/funcion/:id", en: "/funcion/5" });
});
```

**Error típico.** Usar `getBy` para algo que llega después de un pedido (falla porque todavía no está): para eso
`findBy` espera. Y testear detalles internos (el estado) en vez de lo que se ve.

**Pregunta del profe.** ¿Cómo testean sin backend? `vi.mock` reemplaza `api-http.js` entero y cada test decide qué
contesta; `renderConApp` da router, sesión, compra y avisos.

## Recorrido de una compra

```
Cartelera ──Link──> Pelicula ──Link──> Funcion ──navegar──> Confirmar ──navegar(state)──> Ticket
 #/                 #/pelicula/7       #/funcion/5          #/confirmar/5                  #/ticket/ABC
```

| Paso | Componente | Estado | Efecto o hook |
|---|---|---|---|
| Elegir película | `Cartelera`, `TarjetaPelicula` (`<Link>`) | `genero` de `useParams` | `useCargar` pide cartelera y géneros |
| Elegir función | `Pelicula`, `ListaFunciones`, `Horario` | `id` de `useParams` | `useCargar` pide película y funciones |
| Elegir butacas | `Funcion`, `MapaButacas`, `BotonButaca` | `seleccion` en `CompraEnCurso` | `useAlternarButaca` bloquea en el backend; `useRenovarBloqueo` renueva cada minuto |
| Datos y reserva | `Confirmar`, `FormularioCliente` | `nombre`, `email`; `enviando`, `error`, `reservada` de `useReservar` | `useRenovarBloqueo` sigue; `useReservar` hace el POST y vacía la selección |
| La entrada | `Ticket`, `EntradaTicket` | reserva de `location.state` o pedida por código | `useEsperarPago` consulta cada 10 s; `useFestejarPago` tira confetti |

Si alguien ganó una butaca: al tocarla vuelve `rechazadas` y se suelta; al reservar vuelve 409 y `useReservar` lleva
de nuevo al mapa. Si se recarga en `Confirmar`, la selección (en memoria) se perdió y `<Navigate>` vuelve al mapa.

## Machete

- Componente = función que devuelve JSX; props bajan, callbacks `alVerbo` suben.
- Estado: `useState`, nunca mutar; objeto o arreglo nuevo. Forma `set(x => …)` si depende del anterior.
- Re-render: cambia estado, padre o contexto. Lo derivable se calcula en el render, no con un efecto.
- `key` estable (id), no índice si la lista cambia. `key` distinta = remontar.
- Formularios controlados: `value` + `onChange`; `preventDefault` en el submit.
- `useEffect` = sincronizar con afuera; deps = cuándo; limpieza = qué deshacer. `vigente` evita la race.
- Hooks: prefijo `use`, siempre en el mismo orden, nunca en `if`; ESLint `react-hooks` lo controla.
- Context: sesión, compra y avisos; lo de una pantalla, por props.
- `useEffectEvent`: el intervalo lee la selección actual sin reiniciarse.
- Ref: lo que no se dibuja (timer, nodo del DOM). `useMemo` no se usa.
- StrictMode: doble montaje en dev; si algo se rompe, el efecto no limpia.
- Error boundary: clase, solo errores de render; `ErrorDePantalla` envuelve las rutas.
- Router: `HashRouter` por nginx; `useParams` lee, `useNavigate` mueve, `<Link>` sin recargar.
- Mantine tema + Motion con `LazyMotion` y `reducedMotion="user"`.
- Tests: Vitest + Testing Library, `vi.mock` de la API, `renderConApp`, `findBy` para lo asíncrono.
