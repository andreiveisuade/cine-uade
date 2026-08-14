import * as api from "../api.js";
import { boton, botonSecundario, campo, fantasma, filaTabla, panel, select, spinner, tabla } from "../componentes.js";
import { avisar, escapar } from "../dom.js";
import { etiqueta } from "../etiquetas.js";
import { dia, duracion, hora, hoyISO } from "../formato.js";

/* --------------------------------------------- armado automático de la grilla */

/**
 * El planificador: qué se da esta semana.
 *
 * Con cuatro títulos, programar la semana era obvio. Con el importador trayendo dieciocho
 * por corrida hay que elegir, y el backend elige optimizando tres cosas a la vez: puntaje,
 * diversidad de géneros y ocupación de las salas.
 *
 * Por eso la pantalla no es un listado de funciones sino los indicadores primero. Sin
 * ellos, «armame la grilla» es un botón que escupe ciento cincuenta filas que nadie puede
 * juzgar; con ellos, el encargado corre la propuesta con seis títulos y con diez, compara
 * ocupación y variedad, y recién ahí aplica. El elenco y los pases por película están por
 * el mismo motivo: son los que explican **por qué** entró cada una.
 *
 * Mismo par que las programaciones: «Previsualizar» no escribe nada y «Aplicar» crea las
 * funciones con los mismos criterios. Como el planificador es determinista, lo que se ve
 * es lo que se va a crear.
 */

/**
 * Los indicadores de la corrida anterior, para poder decir cuánto mejoró o empeoró la
 * nueva. Vive en el módulo y no en la vista porque comparar dos corridas es el uso normal
 * de esta pantalla, y la vista se vuelve a armar en cada previsualización.
 */
let corridaAnterior = null;

export async function vistaPlanificador(contenedor) {
  const [idiomas, proyecciones] = await Promise.all([
    api.obtenerIdiomas(), api.obtenerProyecciones(),
  ]);

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Planificador de la semana</h1>
    <p class="mb-5 max-w-3xl text-sm text-slate-500 dark:text-slate-400">
      Elige el elenco con un criterio que mira <strong>puntaje y géneros a la vez</strong>
      —la primera es la mejor a secas, y de ahí en adelante cada una suma un bono por los
      géneros que todavía faltan— y después reparte los pases entre las salas de forma
      proporcional al puntaje: la mejor de la semana se lleva cuatro o cinco funciones
      diarias y la última, una. No pisa funciones ya cargadas.
    </p>

    <div class="grid gap-4 lg:grid-cols-[320px_1fr] lg:items-start">
      ${panel(`
        <h2 class="mb-3 font-semibold">Criterios</h2>
        <form id="criterios" class="space-y-3">
          <div class="grid grid-cols-2 gap-2">
            ${campo({ nombre: "desde", etiqueta: "Desde", tipo: "date", valor: hoyISO(), requerido: true })}
            ${campo({ nombre: "dias", etiqueta: "Días", tipo: "number", valor: 7, requerido: true, extra: 'min="1" max="31"' })}
          </div>

          <div class="grid grid-cols-2 gap-2">
            ${campo({ nombre: "apertura", etiqueta: "Apertura", tipo: "time", valor: "14:00", requerido: true })}
            ${campo({ nombre: "cierre", etiqueta: "Cierre", tipo: "time", valor: "00:00", requerido: true })}
          </div>
          <p class="text-xs text-slate-500 dark:text-slate-400">
            El cierre a las 00:00 se lee como el final del día. Es hasta cuándo tiene que
            <em>haber terminado</em> la última función, no cuándo puede empezar.
          </p>

          ${campo({ nombre: "cuantasPeliculas", etiqueta: "Cuántas películas", tipo: "number", valor: 8, requerido: true,
            extra: 'min="1" max="30"',
            pista: "Cuántos títulos distintos entran en la semana. Solo se eligen entre las confirmadas: lo que espera en el buzón no se puede programar." })}

          ${campo({ nombre: "precio", etiqueta: "Precio base", tipo: "number", valor: 5000, requerido: true, extra: 'min="100" step="100"' })}

          <div class="grid grid-cols-2 gap-2">
            ${select({ nombre: "idioma", etiqueta: "Idioma",
              opciones: idiomas.map((i) => `<option value="${i}">${etiqueta(i)}</option>`).join("") })}
            ${select({ nombre: "proyeccion", etiqueta: "Proyección",
              opciones: proyecciones.map((p) => `<option value="${p}">${etiqueta(p)}</option>`).join("") })}
          </div>

          <div class="grid grid-cols-2 gap-2 pt-1">
            ${botonSecundario("Previsualizar", { tamano: "px-4 py-2", clases: "font-medium", atributos: 'id="previsualizar"' })}
            ${boton("Aplicar", { atributos: 'id="aplicar" disabled',
              clases: "disabled:bg-slate-300 dark:disabled:bg-slate-700 dark:disabled:text-slate-400" })}
          </div>
          <p id="errorCriterios" class="hidden text-sm text-red-700 dark:text-red-400"></p>
        </form>

        <p class="mt-3 border-t border-slate-200 pt-3 text-xs text-slate-500 dark:border-slate-800 dark:text-slate-400">
          Previsualizar no escribe nada. Al aplicar, el servidor <strong>vuelve a
          calcular</strong> la propuesta con estos mismos criterios: no recibe la que
          estás viendo, así que si alguien programó algo en el medio, lo respeta.
        </p>
      `, "p-4")}

      <section id="propuesta"></section>
    </div>
  `;

  const formulario = contenedor.querySelector("#criterios");
  const errorCriterios = contenedor.querySelector("#errorCriterios");
  const propuesta = contenedor.querySelector("#propuesta");
  const botonAplicar = contenedor.querySelector("#aplicar");
  const botonPrevisualizar = contenedor.querySelector("#previsualizar");

  const leerCriterios = () => Object.fromEntries(new FormData(formulario));

  /** Una propuesta vale solo para los criterios con los que se pidió. */
  function invalidarPropuesta() {
    botonAplicar.disabled = true;
    botonAplicar.textContent = "Aplicar";
    propuesta.innerHTML = "";
  }

  formulario.addEventListener("input", invalidarPropuesta);
  formulario.addEventListener("change", invalidarPropuesta);

  /**
   * Armar una semana de seis salas le lleva varios segundos al backend: consulta R3 hueco
   * por hueco. Sin decirlo en pantalla parece colgada, y lo que hace el encargado es
   * volver a apretar el botón — que son otros tantos segundos de servidor.
   */
  async function correr(pedir, aplicada) {
    if (!formulario.reportValidity()) return;
    errorCriterios.classList.add("hidden");
    const criterios = leerCriterios();
    const botonActivo = aplicada ? botonAplicar : botonPrevisualizar;
    botonActivo.disabled = true;
    botonActivo.innerHTML = `<span class="inline-flex items-center justify-center gap-2">
      ${spinner("h-4 w-4")}${aplicada ? "Creando…" : "Calculando…"}</span>`;
    propuesta.innerHTML = esqueletoPropuesta(aplicada);

    try {
      const grilla = await pedir(criterios);
      // Si tocó un criterio mientras calculaba, lo que llegó ya no describe lo que está en
      // pantalla: pintarlo sería mostrar una grilla que no es la de estos criterios.
      if (JSON.stringify(leerCriterios()) !== JSON.stringify(criterios)) return;

      propuesta.innerHTML = dibujarPropuesta(grilla, corridaAnterior);
      corridaAnterior = { titulos: Number(criterios.cuantasPeliculas), indicadores: grilla.indicadores };
      // funcionesCreadas viene en 0 al previsualizar y con el número real en el alta: es
      // lo único que distingue «así quedaría» de «así quedó», porque los pases son los
      // mismos en los dos casos.
      botonAplicar.disabled = aplicada || !grilla.pases.length;
      botonAplicar.textContent = aplicada ? "Aplicada" : `Crear ${grilla.pases.length} funciones`;
      if (aplicada) avisar(`Se crearon ${grilla.funcionesCreadas} funciones`);
    } catch (e) {
      invalidarPropuesta();
      errorCriterios.textContent = e.message;
      errorCriterios.classList.remove("hidden");
    } finally {
      botonPrevisualizar.disabled = false;
      botonPrevisualizar.textContent = "Previsualizar";
    }
  }

  botonPrevisualizar.addEventListener("click", () => correr(api.proponerGrilla, false));

  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    correr(api.armarGrilla, true);
  });
}

/* -------------------------------------------------------- mientras calcula */

/**
 * Lo que se ve mientras el servidor arma la grilla: el mismo andamiaje que el resultado,
 * en gris.
 *
 * El mensaje no promete tiempos, promete **qué** está pasando: hasta que el backend dejó
 * de preguntar la disponibilidad hueco por hueco, armar una semana tardaba veinte
 * segundos; hoy tarda medio. Lo que no cambia es que es un ida y vuelta al servidor, y que
 * puede volver a alargarse con más salas o más días.
 *
 * Contar las dos etapas del algoritmo mientras corren es, además, la única parte de la
 * espera que le sirve a quien mira: para cuando aparecen los números, ya sabe de dónde
 * salieron.
 */
function esqueletoPropuesta(aplicada) {
  return `
    <div class="space-y-4">
      <div class="rounded border border-slate-300 bg-slate-50 p-3 dark:border-slate-700 dark:bg-slate-800">
        <p class="flex items-center gap-2 font-semibold">
          ${spinner()} ${aplicada ? "Creando las funciones…" : "Armando la grilla…"}
        </p>
        <p class="mt-1 text-xs text-slate-600 dark:text-slate-300">
          ${aplicada
            ? "Cada pase de la propuesta se programa como una función de verdad."
            : `Primero elige el elenco por puntaje y géneros; después llena cada sala día por
               día, preguntando en cada horario si está libre.`}
        </p>
      </div>

      <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        ${[0, 1, 2, 3].map(() => panel(`
            ${fantasma("h-3 w-24")}
            ${fantasma("mt-2 h-7 w-16")}
            ${fantasma("mt-2 h-3 w-full")}
          `, "p-3")).join("")}
      </div>

      ${panel(`
        ${fantasma("h-4 w-40")}
        <div class="mt-3 space-y-2">
          ${[0, 1, 2, 3, 4, 5].map(() => fantasma("h-6 w-full")).join("")}
        </div>
      `, "p-4")}
    </div>`;
}

/* ------------------------------------------------------------- el resultado */

const porcentaje = (fraccion) => `${Math.round(fraccion * 100)}%`;

const conDecimal = (numero) => numero.toFixed(1).replace(".", ",");

/**
 * Cuánto cambió un indicador contra la corrida anterior. Es la mitad del valor de la
 * pantalla: el número solo no dice si 79% de ocupación está bien, pero «79%, cuatro
 * puntos más que con seis títulos» sí.
 */
function variacion(actual, anterior, formato) {
  if (anterior === null || anterior === undefined) return "";
  const delta = actual - anterior;
  if (Math.abs(delta) < 0.0001) return `<span class="text-xs text-slate-400 dark:text-slate-500">igual que la corrida anterior</span>`;
  const color = delta > 0 ? "text-emerald-700 dark:text-emerald-400" : "text-amber-700 dark:text-amber-400";
  return `<span class="text-xs ${color}">${delta > 0 ? "▲" : "▼"} ${escapar(formato(Math.abs(delta)))} vs. la corrida anterior</span>`;
}

function tarjeta(titulo, valor, detalle, delta) {
  return panel(`
      <p class="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">${escapar(titulo)}</p>
      <p class="text-2xl font-bold">${escapar(valor)}</p>
      <p class="text-xs text-slate-500 dark:text-slate-400">${detalle}</p>
      ${delta || ""}
  `, "p-3");
}

function dibujarIndicadores(indicadores, pases, anterior) {
  const previos = anterior?.indicadores;
  return `
    <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      ${tarjeta("Ocupación de las salas", porcentaje(indicadores.ocupacion),
        `${indicadores.minutosProgramados.toLocaleString("es-AR")} de
         ${indicadores.minutosDisponibles.toLocaleString("es-AR")} minutos <strong>libres</strong>,
         que son la ventana menos lo que ya estaba programado`,
        variacion(indicadores.ocupacion, previos?.ocupacion, porcentaje))}
      ${tarjeta("Puntaje promedio", conDecimal(indicadores.puntajePromedio),
        "por pase, así una película con más funciones pesa más",
        variacion(indicadores.puntajePromedio, previos?.puntajePromedio, conDecimal))}
      ${tarjeta("Géneros cubiertos", `${indicadores.generosCubiertos} de ${indicadores.generosTotales}`,
        "cuántos géneros del catálogo aparecen en la semana",
        variacion(indicadores.generosCubiertos, previos?.generosCubiertos, (n) => `${n}`))}
      ${tarjeta("Pases", String(pases), "funciones que arma la propuesta", "")}
    </div>`;
}

/**
 * Los pases por género, como barras. Es donde se ve el problema que el planificador
 * existe para evitar: una grilla con cuatro películas de acción y nada para el resto.
 */
function dibujarGeneros(pasesPorGenero) {
  const entradas = Object.entries(pasesPorGenero).sort((a, b) => b[1] - a[1]);
  if (!entradas.length) return "";
  const maximo = entradas[0][1];
  return panel(`
      <h3 class="mb-3 font-semibold">Pases por género</h3>
      <div class="space-y-1">
        ${entradas.map(([genero, cuantos]) => `
          <div class="flex items-center gap-2 text-sm">
            <span class="w-32 shrink-0 text-slate-600 dark:text-slate-300">${escapar(etiqueta(genero))}</span>
            <span class="h-3 rounded bg-slate-700 dark:bg-slate-300" style="width: ${(cuantos / maximo) * 70}%"></span>
            <span class="text-xs text-slate-500 dark:text-slate-400">${cuantos}</span>
          </div>`).join("")}
      </div>
      <p class="mt-3 text-xs text-slate-500 dark:text-slate-400">
        Una película cuenta en todos sus géneros, así que la suma es mayor que la cantidad
        de pases.
      </p>
  `, "p-4");
}

/** El elenco elegido: por qué entró cada una y cuánto se lleva. */
function dibujarElenco(elenco) {
  return panel(`
      <h3 class="p-4 pb-2 font-semibold">Elenco de la semana (${elenco.length})</h3>
      ${tabla(
        '<tr><th class="p-2">Película</th><th class="px-3 text-right">Puntaje</th><th class="px-3">Duración</th><th>Géneros</th><th class="text-right">Pases</th></tr>',
        elenco.map((p) => `
            <tr class="${filaTabla()}">
              <td class="p-2 font-medium">${escapar(p.titulo)}</td>
              <td class="px-3 text-right font-medium">${escapar(conDecimal(p.puntaje))}</td>
              <td class="whitespace-nowrap px-3 text-slate-500 dark:text-slate-400">${escapar(duracion(p.duracionMinutos))}</td>
              <td class="text-xs text-slate-500 dark:text-slate-400">${p.generos.map((g) => escapar(etiqueta(g))).join(" · ")}</td>
              <td class="p-2 text-right font-medium">${p.pases}</td>
            </tr>`).join(""))}
      <p class="border-t border-slate-200 p-3 text-xs text-slate-500 dark:border-slate-800 dark:text-slate-400">
        La primera entró por puntaje; las siguientes, por lo que <strong>agregan</strong> a
        lo ya elegido — por eso puede entrar una comedia de 7,0 antes que la cuarta de
        acción de 8,5. Los pases se reparten proporcionalmente al puntaje.
      </p>
  `, "overflow-x-auto");
}

/**
 * La grilla propuesta, agrupada por día y sala. Es la vista de la cartelera de la semana:
 * plana serían ciento cincuenta filas ordenadas por hora, donde no se ve ni qué pasa en
 * una sala ni qué se da un día.
 */
function dibujarPases(pases) {
  const dias = new Map();
  for (const pase of pases) {
    const clave = pase.inicio.slice(0, 10);
    if (!dias.has(clave)) dias.set(clave, new Map());
    const salas = dias.get(clave);
    if (!salas.has(pase.sala)) salas.set(pase.sala, []);
    salas.get(pase.sala).push(pase);
  }

  return panel(`
      <h3 class="mb-3 font-semibold">La semana, sala por sala</h3>
      <div class="space-y-2">
        ${[...dias.entries()].map(([fecha, salas], indice) => `
          <details ${indice === 0 ? "open" : ""} class="rounded border border-slate-200 dark:border-slate-800">
            <summary class="cursor-pointer bg-slate-50 px-3 py-2 text-sm font-medium dark:bg-slate-800">
              ${escapar(dia(`${fecha}T00:00:00`))}
              <span class="font-normal text-slate-500 dark:text-slate-400">
                · ${[...salas.values()].reduce((suma, p) => suma + p.length, 0)} pases
              </span>
            </summary>
            <div class="space-y-2 p-3">
              ${[...salas.entries()].map(([sala, deLaSala]) => `
                <div class="flex flex-wrap items-baseline gap-x-2 gap-y-1">
                  <span class="w-16 shrink-0 text-xs font-medium text-slate-500 dark:text-slate-400">${escapar(sala)}</span>
                  ${deLaSala.map((p) => `
                    <span class="rounded bg-slate-100 px-2 py-0.5 text-xs dark:bg-slate-800">
                      <span class="font-medium">${hora(p.inicio)}</span> ${escapar(p.titulo)}
                    </span>`).join("")}
                </div>`).join("")}
            </div>
          </details>`).join("")}
      </div>
  `, "p-4");
}

function dibujarPropuesta(grilla, anterior) {
  const aplicada = grilla.funcionesCreadas > 0;
  if (!grilla.pases.length) {
    return `<div class="rounded border border-amber-300 bg-amber-50 p-4 text-sm text-amber-900 dark:border-amber-800 dark:bg-amber-950 dark:text-amber-300">
      Con estos criterios no entra ninguna función: revisá la ventana horaria o los días.
    </div>`;
  }

  return `
    <div class="space-y-4">
      <div class="rounded border ${aplicada
        ? "border-emerald-300 bg-emerald-50 dark:border-emerald-800 dark:bg-emerald-950"
        : "border-slate-300 bg-slate-50 dark:border-slate-700 dark:bg-slate-800"} p-3">
        <p class="font-semibold">
          ${aplicada
            ? `Se crearon ${grilla.funcionesCreadas} funciones`
            : `Así quedaría la semana: ${grilla.pases.length} funciones`}
        </p>
        <p class="text-xs text-slate-600 dark:text-slate-300">
          ${aplicada
            ? "Las funciones ya están cargadas y se pueden ver en Funciones y en la Agenda."
            : "Todavía no se escribió nada. Cambiá los criterios y volvé a previsualizar para comparar."}
        </p>
      </div>
      ${dibujarIndicadores(grilla.indicadores, grilla.pases.length, anterior)}
      ${dibujarElenco(grilla.elenco)}
      ${dibujarGeneros(grilla.indicadores.pasesPorGenero)}
      ${dibujarPases(grilla.pases)}
    </div>`;
}
