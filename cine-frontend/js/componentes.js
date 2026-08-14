// Piezas de UI reutilizables: cada una recibe datos por parámetro y devuelve el HTML
// armado, sin guardar estado ni tocar el DOM directamente. Es el molde pensado para
// traducirse después a componentes de React (props adentro, markup afuera): por eso acá
// no entra jQuery ni lógica de negocio, solo presentación.

import { escapar } from "./dom.js";

/* -------------------------------------------------------------------- formulario */

/**
 * Label + input, con el estilo que se repetía en cada input de texto, número y fecha
 * del repo (54 veces, medido con grep, antes de que existiera este archivo). Los
 * checkboxes y radios quedan afuera: su markup ya es chico y no comparte esta clase.
 *
 * `nombre` se usa como `id` y como `name` a la vez, porque las vistas lo leen de las
 * dos formas según el caso: un filtro se busca por `#id` y un campo de formulario viaja
 * por `FormData` usando `name`. Repetirlo en los dos atributos cubre ambos sin que quien
 * llama tenga que saber cuál va a necesitar.
 *
 * `etiqueta` y `pista` las escribe quien programa la pantalla (a veces con markup propio,
 * como un `<span>` de aclaración) y no escapan; `valor` sí, porque ahí es donde puede
 * llegar algo tipeado por un cliente o traído de TMDB.
 */
export function campo({ nombre, etiqueta, tipo = "text", valor, placeholder, requerido = false, extra = "", pista, ancho = "w-full" }) {
  const clases = `mt-1 ${ancho} rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500`;
  const control = tipo === "textarea"
    ? `<textarea id="${nombre}" name="${nombre}" ${requerido ? "required" : ""} ${extra}
         class="${clases}">${valor !== undefined && valor !== null ? escapar(valor) : ""}</textarea>`
    : `<input id="${nombre}" name="${nombre}" type="${tipo}"
         ${valor !== undefined && valor !== null && valor !== "" ? `value="${escapar(valor)}"` : ""}
         ${placeholder ? `placeholder="${escapar(placeholder)}"` : ""} ${requerido ? "required" : ""} ${extra}
         class="${clases}" />`;
  return `
    <label class="block text-sm">
      <span class="text-slate-600 dark:text-slate-300">${etiqueta}</span>
      ${control}
      ${pista ? `<span class="mt-1 block text-xs text-slate-500 dark:text-slate-400">${pista}</span>` : ""}
    </label>`;
}

/**
 * Label + select, mismo estilo que `campo()`. `opciones` es el HTML de los `<option>`
 * ya armado por quien llama: es quien sabe cuál va `selected` y con qué texto, y eso no
 * se puede generalizar sin reinventar un mini-framework de formularios para algo que se
 * usa en veinte pantallas distintas cada una a su manera.
 */
export function select({ nombre, etiqueta, opciones, extra = "", ancho = "w-full" }) {
  return `
    <label class="block text-sm">
      <span class="text-slate-600 dark:text-slate-300">${etiqueta}</span>
      <select id="${nombre}" name="${nombre}" ${extra}
        class="mt-1 ${ancho} rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500">
        ${opciones}
      </select>
    </label>`;
}

/* ------------------------------------------------------------------------ layout */

/** El panel blanco con borde que envuelve cada sección (17 veces). `extra` es lo que
 * cambia de panel a panel: padding, overflow-x-auto. `atributos` es para lo puntual,
 * como el `id` de un panel que se vuelve a pintar solo (`outerHTML`). */
export function panel(contenido, extra = "", atributos = "") {
  return `<section ${atributos} class="rounded border border-slate-300 bg-white dark:border-slate-700 dark:bg-slate-900 ${extra}">${contenido}</section>`;
}

/** Una tabla con el thead gris estándar (8 veces). `filaEncabezado` es el `<tr>` de
 * columnas completo, que cambia de tabla en tabla; lo único fijo es el envoltorio. */
export function tabla(filaEncabezado, cuerpo) {
  return `<table class="w-full text-sm">
    <thead class="border-b border-slate-300 bg-slate-50 text-left text-xs uppercase text-slate-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-400">
      ${filaEncabezado}
    </thead>
    <tbody>${cuerpo}</tbody>
  </table>`;
}

/** La clase de una fila de tabla (10 veces), con lugar para lo puntual de cada fila:
 * resaltar una edición en curso, un cursor-pointer si la fila es clicable. */
export function filaTabla(extra = "") {
  return `border-b border-slate-200 dark:border-slate-800 ${extra}`.trim();
}

/* ----------------------------------------------------------------------- botones */

/** El botón de acción principal: fondo sólido (18 veces). */
export function boton(texto, { tipo = "submit", ancho = "w-full", tamano = "px-4 py-2", atributos = "", clases = "" } = {}) {
  return `<button type="${tipo}" ${atributos}
    class="${ancho} rounded bg-slate-900 ${tamano} text-sm font-medium text-white dark:bg-white dark:text-slate-900 ${clases}">${texto}</button>`;
}

/** El botón de acción secundaria: solo borde, sin relleno. */
export function botonSecundario(texto, { tipo = "button", tamano = "px-3 py-1.5", atributos = "", clases = "" } = {}) {
  return `<button type="${tipo}" ${atributos}
    class="rounded border border-slate-400 ${tamano} text-sm dark:border-slate-600 ${clases}">${texto}</button>`;
}

/* ------------------------------------------------------------------- piezas chicas */

export function chip(texto, clases = "bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-200") {
  return `<span class="inline-block rounded-full px-2 py-0.5 text-xs font-medium ${clases}">${escapar(texto)}</span>`;
}

/**
 * El círculo que gira. Vive acá y no en cada vista porque es el mismo en todas, y porque
 * es lo único que separa «está tardando» de «se colgó»: quieta, una espera de más de un
 * segundo se lee como una pantalla rota, y lo que hace el usuario es volver a apretar.
 */
export function spinner(clases = "h-5 w-5") {
  return `<span class="inline-block ${clases} animate-spin rounded-full border-2 border-slate-300 border-t-slate-700 dark:border-slate-600 dark:border-t-slate-200"></span>`;
}

export function cargando(mensaje = "Cargando…") {
  return `<div class="flex items-center justify-center gap-3 py-12 text-slate-500 dark:text-slate-400">
    ${spinner()}<span>${escapar(mensaje)}</span>
  </div>`;
}

/**
 * Un bloque gris que late, del tamaño de lo que se está por dibujar.
 *
 * Sirve donde se conoce la forma del resultado, y ahí es mejor que un cartel centrado por
 * dos motivos: dice **dónde** va a aparecer cada cosa —así la pantalla no salta cuando
 * llegan los datos— y deja ver de entrada que lo que viene son cuatro números y una tabla,
 * no una lista infinita.
 */
export function fantasma(clases) {
  return `<div class="animate-pulse rounded bg-slate-200 dark:bg-slate-700 ${clases}"></div>`;
}

export function error(mensaje) {
  return `<div class="rounded border border-red-300 bg-red-50 p-4 text-red-800 dark:border-red-800 dark:bg-red-950 dark:text-red-300">${escapar(mensaje)}</div>`;
}

/**
 * posterUrl es un String que puede venir vacío o con una URL que no carga: la inicial
 * del título queda siempre debajo y la imagen la tapa solo si llega bien.
 */
export function imagenPoster(pelicula, clases) {
  const inicial = escapar(pelicula.titulo.charAt(0).toUpperCase());
  const imagen = pelicula.posterUrl
    ? `<img src="${escapar(pelicula.posterUrl)}" alt="" loading="lazy"
        class="absolute inset-0 h-full w-full object-cover" onerror="this.remove()" />`
    : "";
  return `<div class="relative overflow-hidden bg-slate-300 dark:bg-slate-700 ${clases}">
    <span class="flex h-full w-full items-center justify-center text-2xl font-bold text-slate-500 dark:text-slate-400">${inicial}</span>
    ${imagen}
  </div>`;
}
