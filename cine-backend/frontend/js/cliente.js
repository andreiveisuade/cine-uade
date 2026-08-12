// Vistas del cliente: cartelera, detalle de película, mapa de butacas,
// confirmación y ticket. Sin login.

import * as api from "./api.js";
import { iniciarRouter } from "./router.js";
import { chip, duracion, escapar, etiqueta } from "./ui.js";

/* ---------------------------------------------------------------- cartelera */

async function vistaCartelera(contenedor) {
  const peliculas = await api.obtenerCartelera();

  const tarjetas = peliculas.map((p) => `
    <a href="#/pelicula/${p.id}"
       class="block rounded border border-slate-300 bg-white p-4 hover:border-slate-500">
      <h2 class="font-semibold leading-tight">${escapar(p.titulo)}</h2>
      <p class="mt-1 text-sm text-slate-500">${duracion(p.duracionMinutos)}</p>
      <div class="mt-3 flex flex-wrap gap-1">
        ${p.generos.map((g) => chip(etiqueta(g))).join("")}
      </div>
    </a>
  `).join("");

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Cartelera</h1>
    <p class="mb-5 text-sm text-slate-500">${peliculas.length} películas en cartel</p>
    <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">${tarjetas}</div>
  `;
}

/* ------------------------------------------------------------------- router */

iniciarRouter({
  contenedor: document.getElementById("app"),
  inicial: "cartelera",
  rutas: {
    cartelera: vistaCartelera,
  },
});
