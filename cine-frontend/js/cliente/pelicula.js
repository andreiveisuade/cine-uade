import * as api from "../api.js";
import { chip, chipClasificacion, dia, duracion, escapar, etiqueta, hora, imagenPoster, porDia, precio } from "../ui.js";

/* --------------------------------------------------- detalle de una película */

export async function vistaPelicula(contenedor, id) {
  const [pelicula, funciones] = await Promise.all([
    api.obtenerPelicula(id),
    api.obtenerFuncionesDePelicula(id),
  ]);

  const dias = porDia(funciones).map(([, delDia]) => `
    <section class="mb-5">
      <h3 class="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">
        ${escapar(dia(delDia[0].inicio))}
      </h3>
      <div class="grid gap-2 sm:grid-cols-2">
        ${delDia.map((f) => `
          <a href="#/funcion/${f.id}"
             class="flex items-center justify-between rounded border border-slate-300 bg-white px-3 py-2 hover:border-slate-500">
            <div>
              <p class="font-semibold">${hora(f.inicio)}</p>
              <p class="text-sm text-slate-600">
                ${escapar(f.sala.nombre)} · ${etiqueta(f.sala.tipo)}
              </p>
              <div class="mt-1 flex gap-1">
                ${chip(etiqueta(f.proyeccion), "bg-indigo-100 text-indigo-800")}
                ${chip(etiqueta(f.idioma), "bg-amber-100 text-amber-800")}
              </div>
            </div>
            <div class="text-right">
              <p class="text-xs text-slate-500">desde</p>
              <p class="font-semibold">${precio(f.precioDesde)}</p>
            </div>
          </a>
        `).join("")}
      </div>
    </section>
  `).join("");

  contenedor.innerHTML = `
    <a href="#/" class="text-sm text-slate-500 hover:text-slate-900">&larr; Cartelera</a>
    <div class="mt-2 mb-6 flex flex-wrap gap-4">
      ${imagenPoster(pelicula, "h-48 w-32 shrink-0 rounded")}
      <div class="min-w-64 flex-1">
        <h1 class="text-2xl font-bold">${escapar(pelicula.titulo)}</h1>
        <p class="mt-1 text-sm text-slate-500">
          ${[pelicula.anio || null, duracion(pelicula.duracionMinutos),
             pelicula.idiomaOriginal || null].filter(Boolean).map(escapar).join(" · ")}
        </p>
        ${pelicula.director
          ? `<p class="mt-1 text-sm text-slate-600">Dirección: ${escapar(pelicula.director)}</p>`
          : ""}
        <div class="mt-2">${chipClasificacion(pelicula.clasificacion)}</div>
        <div class="mt-2 flex flex-wrap gap-1">
          ${pelicula.generos.map((g) => chip(etiqueta(g))).join("")}
        </div>
        ${pelicula.sinopsis
          ? `<p class="mt-3 max-w-prose text-sm text-slate-700">${escapar(pelicula.sinopsis)}</p>`
          : ""}
      </div>
    </div>
    <h2 class="mb-3 text-lg font-semibold">Funciones</h2>
    ${funciones.length ? dias : '<p class="text-slate-500">No hay funciones programadas.</p>'}
  `;
}
