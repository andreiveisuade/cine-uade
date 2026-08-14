import * as api from "../api.js";
import { duracion, escapar, etiqueta, hoyISO } from "../ui.js";

/* --------------------------------------------------------------- la agenda */
/*
 * La programación vista como calendario: las horas en el eje vertical y cada función
 * como un bloque ubicado y **dimensionado por su duración real**.
 *
 * El listado de funciones ya existía y es una tabla ordenada por fecha. Sirve para
 * buscar una función puntual y no sirve para nada más: no muestra los huecos, que es
 * justo lo que hay que ver para programar. Un cine no se planifica leyendo filas, se
 * planifica mirando dónde queda lugar.
 *
 * Que el alto salga de la duración no es decoración: es el dato que decide si dos
 * funciones se pisan (R3). La Odisea ocupa casi tres horas de columna y un corto ocupa
 * una franja mínima, y esa diferencia —que en la tabla es un número que hay que leer y
 * comparar— acá se ve de un vistazo.
 */

/**
 * Dos preguntas distintas, y por eso dos modos.
 *
 * «¿Qué doy en la Sala 1 esta semana?» es planificación: una sala a lo largo de los días.
 * «¿Cómo queda el cine el sábado?» es operación: todas las salas de un mismo día.
 *
 * No se pueden juntar en una sola vista. Un calendario apila por hora, y a las 15:00 hay
 * seis funciones simultáneas en seis salas: en una única columna serían seis bloques
 * pisados e ilegibles. Lo que cambia entre los dos modos es solo qué representa cada
 * columna; el resto del dibujo es el mismo.
 */
const MODOS = {
  semana: { etiqueta: "Semana (una sala)", dias: 7 },
  dia: { etiqueta: "Día (todas las salas)", dias: 1 },
};

/** Alto de un minuto. Con esto una película de dos horas mide 132px, que se lee bien. */
const PX_POR_MINUTO = 1.1;

/**
 * Ningún bloque baja de esto aunque dure menos.
 *
 * Rompe a propósito la proporción: el corto de 5 minutos debería medir 5px y ahí no se
 * lee ni el título. Es la única mentira del dibujo y se paga barato, porque abajo de los
 * veinte minutos la diferencia exacta ya no le cambia la decisión a nadie.
 */
const ALTO_MINIMO = 26;

/** Un color por película, estable entre recargas: el mismo título siempre igual. */
const COLORES = [
  "bg-emerald-200 text-emerald-950 dark:bg-emerald-800 dark:text-emerald-50",
  "bg-sky-200 text-sky-950 dark:bg-sky-800 dark:text-sky-50",
  "bg-amber-200 text-amber-950 dark:bg-amber-800 dark:text-amber-50",
  "bg-rose-200 text-rose-950 dark:bg-rose-800 dark:text-rose-50",
  "bg-violet-200 text-violet-950 dark:bg-violet-800 dark:text-violet-50",
  "bg-teal-200 text-teal-950 dark:bg-teal-800 dark:text-teal-50",
  "bg-orange-200 text-orange-950 dark:bg-orange-800 dark:text-orange-50",
  "bg-indigo-200 text-indigo-950 dark:bg-indigo-800 dark:text-indigo-50",
];

const NOMBRE_DIA = ["Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"];

export async function vistaAgenda(contenedor, modoPedido, desdePedido, salaPedida) {
  const { modo, desde, salaId } = leerRuta(modoPedido, desdePedido, salaPedida);
  const [funciones, salas] = await Promise.all([api.obtenerFunciones(), api.obtenerSalas()]);

  const sala = salas.find((s) => s.id === salaId) || salas[0];
  const columnas = armarColumnas(modo, desde, salas, sala);
  const visibles = funciones.filter((f) => columnas.some((c) => c.tomaA(f)));
  const franja = franjaHoraria(visibles);

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Agenda</h1>
    <p class="mb-5 text-sm text-slate-500 dark:text-slate-400">
      La programación como la ve quien la arma: cada bloque ocupa el alto de lo que dura.
      Los huecos son dónde entra algo nuevo.
    </p>

    <div class="mb-4 flex flex-wrap items-end gap-3">
      <label class="text-sm">
        <span class="text-slate-600 dark:text-slate-300">Ver</span>
        <select id="modo" class="mt-1 block rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100">
          ${Object.entries(MODOS).map(([clave, m]) => `
            <option value="${clave}" ${clave === modo ? "selected" : ""}>${m.etiqueta}</option>`).join("")}
        </select>
      </label>
      ${modo === "semana" ? `
        <label class="text-sm">
          <span class="text-slate-600 dark:text-slate-300">Sala</span>
          <select id="sala" class="mt-1 block rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100">
            ${salas.map((s) => `
              <option value="${s.id}" ${s.id === sala.id ? "selected" : ""}>${escapar(s.nombre)} — ${escapar(etiqueta(s.tipo))}</option>`).join("")}
          </select>
        </label>` : ""}
      <div class="flex items-center gap-1">
        <button type="button" id="antes" class="rounded border border-slate-400 px-3 py-1.5 text-sm dark:border-slate-600">←</button>
        <button type="button" id="hoy" class="rounded border border-slate-400 px-3 py-1.5 text-sm dark:border-slate-600">Hoy</button>
        <button type="button" id="despues" class="rounded border border-slate-400 px-3 py-1.5 text-sm dark:border-slate-600">→</button>
      </div>
      <p class="text-sm text-slate-500 dark:text-slate-400">
        ${visibles.length} funciones${modo === "semana" ? ` en ${escapar(sala.nombre)}` : ""}
      </p>
    </div>

    ${franja
      ? dibujarGrilla(columnas, visibles, franja)
      : `<p class="rounded border border-slate-300 p-6 text-center text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">
           No hay funciones programadas en este rango.
         </p>`}

    <p class="mt-3 border-t border-slate-200 pt-3 text-xs text-slate-500 dark:border-slate-800 dark:text-slate-400">
      El alto de cada bloque es su duración. Los que duran menos de ${Math.round(ALTO_MINIMO / PX_POR_MINUTO)}
      minutos se dibujan con un alto mínimo para que el título entre: es la única parte del gráfico
      que no está a escala.
    </p>
  `;

  const ir = (cambios) => {
    const proximo = { modo, desde, salaId: sala.id, ...cambios };
    location.hash = `#/agenda/${proximo.modo}/${proximo.desde}/${proximo.salaId}`;
  };

  contenedor.querySelector("#modo").addEventListener("change", (e) => ir({ modo: e.target.value }));
  contenedor.querySelector("#sala")?.addEventListener("change", (e) => ir({ salaId: Number(e.target.value) }));
  contenedor.querySelector("#hoy").addEventListener("click", () => ir({ desde: hoyISO() }));
  contenedor.querySelector("#antes").addEventListener("click", () => ir({ desde: correr(desde, -MODOS[modo].dias) }));
  contenedor.querySelector("#despues").addEventListener("click", () => ir({ desde: correr(desde, MODOS[modo].dias) }));
}

/* ----------------------------------------------------------------- el dibujo */

function dibujarGrilla(columnas, funciones, { inicio, fin }) {
  const alto = (fin - inicio) * PX_POR_MINUTO;
  const horas = [];
  for (let m = inicio; m <= fin; m += 60) {
    horas.push(m);
  }

  return `
    <div class="overflow-x-auto rounded border border-slate-300 dark:border-slate-700">
      <div class="min-w-[720px]">
        <div class="grid border-b border-slate-300 bg-slate-50 text-center text-sm font-medium dark:border-slate-700 dark:bg-slate-900"
             style="grid-template-columns: 56px repeat(${columnas.length}, minmax(0, 1fr))">
          <div></div>
          ${columnas.map((c) => `<div class="border-l border-slate-200 py-2 dark:border-slate-800">${c.titulo}</div>`).join("")}
        </div>
        <div class="grid" style="grid-template-columns: 56px repeat(${columnas.length}, minmax(0, 1fr))">
          <div class="relative" style="height: ${alto}px">
            ${horas.map((m) => `
              <span class="absolute right-1 -translate-y-1/2 text-xs text-slate-400 dark:text-slate-500"
                    style="top: ${(m - inicio) * PX_POR_MINUTO}px">${enHora(m)}</span>`).join("")}
          </div>
          ${columnas.map((columna) => `
            <div class="relative border-l border-slate-200 dark:border-slate-800" style="height: ${alto}px">
              ${horas.map((m) => `
                <div class="absolute inset-x-0 border-t border-slate-100 dark:border-slate-800/70"
                     style="top: ${(m - inicio) * PX_POR_MINUTO}px"></div>`).join("")}
              ${funciones.filter((f) => columna.tomaA(f)).map((f) => bloque(f, inicio, columna)).join("")}
            </div>`).join("")}
        </div>
      </div>
    </div>
  `;
}

function bloque(funcion, inicioFranja, columna) {
  const arranca = minutosDe(funcion.inicio);
  const dura = funcion.pelicula.duracionMinutos;
  const top = (arranca - inicioFranja) * PX_POR_MINUTO;
  const alto = Math.max(dura * PX_POR_MINUTO, ALTO_MINIMO);
  const detalle = `${funcion.pelicula.titulo}\n${enHora(arranca)}–${enHora(arranca + dura)} (${duracion(dura)})`
    + `\n${funcion.sala.nombre} · ${etiqueta(funcion.proyeccion)} · ${etiqueta(funcion.idioma)}`;

  // Un <a> y no un div con onclick: la agenda es para mirar, y desde acá se salta a
  // operar sobre esa función. Siendo un enlace de verdad funciona el clic del medio,
  // el "abrir en pestaña nueva" y el teclado, gratis.
  return `
    <a href="#/funciones/${funcion.id}"
       class="absolute inset-x-0.5 block overflow-hidden rounded px-1.5 py-0.5 text-xs leading-tight
              ring-slate-900/40 hover:ring-2 dark:ring-white/50 ${color(funcion.pelicula.id)}"
       style="top: ${top}px; height: ${alto}px"
       title="${escapar(detalle)} — clic para verla en Funciones">
      <span class="font-semibold">${enHora(arranca)}</span>
      <span class="ml-1">${escapar(funcion.pelicula.titulo)}</span>
      ${alto > 44 ? `<span class="block text-[11px] opacity-70">${escapar(columna.subtitulo(funcion))}</span>` : ""}
    </a>
    ${limpieza(funcion, arranca + dura, inicioFranja)}
  `;
}

/**
 * La franja rayada que va pegada abajo de cada función: el rato en que la sala se está
 * levantando y no se puede programar nada.
 *
 * <p>Es lo que vuelve visible por qué la próxima función no puede arrancar donde termina
 * la anterior. Sin esto el hueco existe igual —el backend lo rechaza— pero en la pantalla
 * se ve como espacio libre, y el encargado descubre la regla recién cuando el alta falla.
 *
 * <p>No es un bloque propio ni un enlace: no hay nada que abrir. Se deriva de la función
 * de arriba, igual que la agenda entera se deriva de las funciones.
 */
function limpieza(funcion, termina, inicioFranja) {
  const minutos = funcion.sala.minutosLimpieza;
  if (!minutos) return "";

  return `
    <div class="pointer-events-none absolute inset-x-0.5 rounded-b border-t border-dashed border-slate-400/70"
         style="top: ${(termina - inicioFranja) * PX_POR_MINUTO}px; height: ${minutos * PX_POR_MINUTO}px;
                background-image: repeating-linear-gradient(45deg, rgb(100 116 139 / .18) 0 4px, transparent 4px 8px)"
         title="Limpieza de ${escapar(funcion.sala.nombre)}: ${minutos} min, hasta ${enHora(termina + minutos)}"></div>
  `;
}

/* ------------------------------------------------------------------ columnas */

/**
 * Cada columna sabe dos cosas: qué funciones le tocan y cómo se llama. Lo demás del
 * dibujo no necesita saber si está mirando días o salas.
 */
function armarColumnas(modo, desde, salas, sala) {
  if (modo === "dia") {
    return salas.map((s) => ({
      titulo: `${escapar(s.nombre)}<span class="block text-xs font-normal text-slate-500 dark:text-slate-400">${escapar(etiqueta(s.tipo))}</span>`,
      tomaA: (f) => f.sala.id === s.id && f.inicio.slice(0, 10) === desde,
      subtitulo: (f) => etiqueta(f.proyeccion),
    }));
  }
  return Array.from({ length: 7 }, (_, i) => {
    const fecha = correr(desde, i);
    const dia = new Date(`${fecha}T00:00:00`);
    return {
      titulo: `${NOMBRE_DIA[dia.getDay()]}<span class="block text-lg">${dia.getDate()}</span>`,
      tomaA: (f) => f.sala.id === sala.id && f.inicio.slice(0, 10) === fecha,
      subtitulo: (f) => `${etiqueta(f.proyeccion)} · ${etiqueta(f.idioma).toLowerCase()}`,
    };
  });
}

/* -------------------------------------------------------------------- apoyos */

/**
 * De qué hora a qué hora dibujar. Sale de las funciones y no es fijo de 00 a 24: un cine
 * abre a la tarde, y tres cuartos de la grilla vacíos serían tres cuartos de scroll.
 */
function franjaHoraria(funciones) {
  if (!funciones.length) return null;
  const arranques = funciones.map((f) => minutosDe(f.inicio));
  // El final incluye la limpieza: si no, la franja termina justo donde acaba la última
  // película y su rayado queda cortado por el borde de la grilla.
  const finales = funciones.map((f) =>
    minutosDe(f.inicio) + f.pelicula.duracionMinutos + (f.sala.minutosLimpieza || 0));
  return {
    inicio: Math.floor(Math.min(...arranques) / 60) * 60,
    fin: Math.ceil(Math.max(...finales) / 60) * 60,
  };
}

const minutosDe = (iso) => Number(iso.slice(11, 13)) * 60 + Number(iso.slice(14, 16));

const enHora = (m) => `${String(Math.floor(m / 60) % 24).padStart(2, "0")}:${String(m % 60).padStart(2, "0")}`;

const color = (peliculaId) => COLORES[peliculaId % COLORES.length];

function correr(fechaISO, dias) {
  const d = new Date(`${fechaISO}T00:00:00`);
  d.setDate(d.getDate() + dias);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

/**
 * La ruta es #/agenda/modo/desde/sala, y el router entrega cada tramo por separado.
 * Entrar a #/agenda pelado tiene que funcionar igual, así que los tres tienen default y
 * se validan: un hash escrito a mano no puede dejar la pantalla en blanco.
 */
function leerRuta(modo, desde, salaId) {
  return {
    modo: MODOS[modo] ? modo : "semana",
    desde: /^\d{4}-\d{2}-\d{2}$/.test(desde || "") ? desde : hoyISO(),
    salaId: Number(salaId) || 1,
  };
}
