import * as api from "../api.js";
import { chip, imagenPoster } from "../componentes.js";
import { escapar } from "../dom.js";
import { chipClasificacion, etiqueta } from "../etiquetas.js";
import { duracion } from "../formato.js";

/* ---------------------------------------------------------------- cartelera */

export async function vistaCartelera(contenedor, generoFiltrado) {
  const [peliculas, generos] = await Promise.all([
    api.obtenerCartelera(generoFiltrado),
    api.obtenerGeneros(),
  ]);

  const filtros = [
    `<a href="#/cartelera" class="rounded-full px-3 py-1 text-xs font-medium ${
      generoFiltrado ? "bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-200" : "bg-slate-900 text-white dark:bg-white dark:text-slate-900"}">Todos</a>`,
    ...generos.map((g) => `
      <a href="#/cartelera/${g}" class="rounded-full px-3 py-1 text-xs font-medium ${
        g === generoFiltrado ? "bg-slate-900 text-white dark:bg-white dark:text-slate-900" : "bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-200"}">
        ${etiqueta(g)}
      </a>`),
  ].join("");

  const tarjetas = peliculas.map((p) => `
    <a href="#/pelicula/${p.id}"
       class="flex gap-3 rounded border border-slate-300 bg-white p-3 hover:border-slate-500 dark:border-slate-700 dark:bg-slate-900 dark:hover:border-slate-500">
      ${imagenPoster(p, "h-32 w-[5.5rem] shrink-0 rounded")}
      <div class="min-w-0">
        <h2 class="font-semibold leading-tight">${escapar(p.titulo)}</h2>
        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">${duracion(p.duracionMinutos)}</p>
        <div class="mt-2">${chipClasificacion(p.clasificacion)}</div>
        <div class="mt-2 flex flex-wrap gap-1">
          ${p.generos.map((g) => chip(etiqueta(g))).join("")}
        </div>
      </div>
    </a>
  `).join("");

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Cartelera</h1>
    <p class="mb-3 text-sm text-slate-500 dark:text-slate-400">
      ${peliculas.length} película${peliculas.length === 1 ? "" : "s"}
      ${generoFiltrado ? `de ${etiqueta(generoFiltrado).toLowerCase()}` : "en cartel"}
    </p>
    <div class="mb-5 flex flex-wrap gap-1">${filtros}</div>
    ${peliculas.length
      ? `<div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">${tarjetas}</div>`
      : '<p class="py-8 text-center text-slate-500 dark:text-slate-400">No hay películas de ese género en cartelera.</p>'}
  `;
}
