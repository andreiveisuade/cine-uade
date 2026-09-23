import { escapar } from "./dom.js";

// etiqueta y pista no se escapan (llevan markup propio); valor sí.
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

export function panel(contenido, extra = "", atributos = "") {
  return `<section ${atributos} class="rounded border border-slate-300 bg-white dark:border-slate-700 dark:bg-slate-900 ${extra}">${contenido}</section>`;
}

export function tabla(filaEncabezado, cuerpo) {
  return `<table class="w-full text-sm">
    <thead class="border-b border-slate-300 bg-slate-50 text-left text-xs uppercase text-slate-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-400">
      ${filaEncabezado}
    </thead>
    <tbody>${cuerpo}</tbody>
  </table>`;
}

export function filaTabla(extra = "") {
  return `border-b border-slate-200 dark:border-slate-800 ${extra}`.trim();
}

export function boton(texto, { tipo = "submit", ancho = "w-full", tamano = "px-4 py-2", atributos = "", clases = "" } = {}) {
  return `<button type="${tipo}" ${atributos}
    class="${ancho} rounded bg-slate-900 ${tamano} text-sm font-medium text-white dark:bg-white dark:text-slate-900 ${clases}">${texto}</button>`;
}

export function botonSecundario(texto, { tipo = "button", tamano = "px-3 py-1.5", atributos = "", clases = "" } = {}) {
  return `<button type="${tipo}" ${atributos}
    class="rounded border border-slate-400 ${tamano} text-sm dark:border-slate-600 ${clases}">${texto}</button>`;
}

export function chip(texto, clases = "bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-200") {
  return `<span class="inline-block rounded-full px-2 py-0.5 text-xs font-medium ${clases}">${escapar(texto)}</span>`;
}

export function spinner(clases = "h-5 w-5") {
  return `<span class="inline-block ${clases} animate-spin rounded-full border-2 border-slate-300 border-t-slate-700 dark:border-slate-600 dark:border-t-slate-200"></span>`;
}

export function cargando(mensaje = "Cargando…") {
  return `<div class="flex items-center justify-center gap-3 py-12 text-slate-500 dark:text-slate-400">
    ${spinner()}<span>${escapar(mensaje)}</span>
  </div>`;
}

export function fantasma(clases) {
  return `<div class="animate-pulse rounded bg-slate-200 dark:bg-slate-700 ${clases}"></div>`;
}

export function error(mensaje) {
  return `<div class="rounded border border-red-300 bg-red-50 p-4 text-red-800 dark:border-red-800 dark:bg-red-950 dark:text-red-300">${escapar(mensaje)}</div>`;
}

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
