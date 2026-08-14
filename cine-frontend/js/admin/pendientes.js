import * as api from "../api.js";
import { boton, botonSecundario, imagenPoster, panel } from "../componentes.js";
import { avisar, escapar } from "../dom.js";
import { chipClasificacion, etiqueta } from "../etiquetas.js";
import { duracion } from "../formato.js";

/* ------------------------------------------------- el buzón de lo importado */

/**
 * Lo que trajo el importador de TMDB y todavía nadie miró.
 *
 * Es una pantalla aparte de Películas y no un filtro más de aquella lista, por lo mismo
 * que estadoRevision es un campo aparte de enCartelera: acá no se está administrando el
 * catálogo, se está decidiendo qué entra. Son dos tareas distintas y una tiene dos
 * botones que la otra no tiene.
 */
export async function vistaPendientes(contenedor) {
  const pendientes = await api.obtenerPeliculasPendientes();

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Por revisar</h1>
    <p class="mb-5 max-w-2xl text-sm text-slate-500 dark:text-slate-400">
      Lo que trajo el importador de TMDB y todavía nadie miró. Hasta que las confirmes no
      se pueden programar ni las ve el cliente. Lo que descartes queda descartado: el
      importador no lo vuelve a proponer.
    </p>
    ${pendientes.length === 0 ? vacio() : lista(pendientes)}
  `;

  contenedor.querySelectorAll("[data-confirmar]").forEach((botonTarjeta) => {
    botonTarjeta.addEventListener("click", () => decidir(contenedor, botonTarjeta,
      api.confirmarPelicula(Number(botonTarjeta.dataset.confirmar)),
      `${botonTarjeta.dataset.titulo} confirmada: ya se puede programar`));
  });

  contenedor.querySelectorAll("[data-descartar]").forEach((botonTarjeta) => {
    botonTarjeta.addEventListener("click", () => decidir(contenedor, botonTarjeta,
      api.descartarPelicula(Number(botonTarjeta.dataset.descartar)),
      `${botonTarjeta.dataset.titulo} descartada`));
  });
}

/**
 * El resultado de decidir es siempre volver a dibujar: la película sale del buzón y el
 * texto de arriba cambia. Redibujar entero es más barato que sacar la tarjeta a mano y
 * mantener la cuenta sincronizada por las suyas.
 *
 * Los dos botones de la tarjeta quedan deshabilitados mientras la llamada viaja: sin eso,
 * dos clics seguidos mandan confirmar y descartar sobre la misma película.
 */
async function decidir(contenedor, botonTarjeta, promesa, mensaje) {
  const tarjeta = botonTarjeta.closest("section");
  tarjeta.querySelectorAll("button").forEach((b) => { b.disabled = true; });
  tarjeta.classList.add("opacity-60");
  try {
    await promesa;
    avisar(mensaje);
    vistaPendientes(contenedor);
  } catch (e) {
    avisar(e.message, "error");
    tarjeta.querySelectorAll("button").forEach((b) => { b.disabled = false; });
    tarjeta.classList.remove("opacity-60");
  }
}

function vacio() {
  return panel(`
      <p class="text-sm text-slate-500 dark:text-slate-400">
        No hay nada esperando. Cuando el importador traiga títulos nuevos van a aparecer acá.
      </p>
  `, "p-8 text-center");
}

function lista(pendientes) {
  return `
    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      ${pendientes.map(tarjeta).join("")}
    </div>
  `;
}

/**
 * Una tarjeta y no una fila de tabla: para decidir hay que ver de qué se trata, y eso es
 * el poster y la sinopsis. En una tabla de siete columnas la sinopsis no entra, y sin
 * sinopsis la decisión se toma leyendo un título suelto.
 */
function tarjeta(pelicula) {
  return panel(`
      <div class="flex gap-3 p-3">
        ${imagenPoster(pelicula, "h-32 w-24 shrink-0 rounded object-cover")}
        <div class="min-w-0">
          <h2 class="font-semibold leading-tight">${escapar(pelicula.titulo)}</h2>
          <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">
            ${pelicula.anio || "—"} · ${duracion(pelicula.duracionMinutos)}
          </p>
          <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">
            ${escapar(pelicula.director) || "Sin director"}
          </p>
          <div class="mt-2 flex flex-wrap items-center gap-x-2 gap-y-1">
            ${chipClasificacion(pelicula.clasificacion)}
            ${pelicula.generos.map((g) => `<span class="text-xs text-slate-500 dark:text-slate-400">${etiqueta(g)}</span>`).join("")}
          </div>
        </div>
      </div>
      <p class="px-3 text-xs leading-relaxed text-slate-600 dark:text-slate-300">
        ${escapar(pelicula.sinopsis) || "Sin sinopsis."}
      </p>
      <div class="mt-auto flex gap-2 p-3">
        ${boton("Confirmar", { tipo: "button", ancho: "flex-1", tamano: "px-3 py-1.5",
          atributos: `data-confirmar="${pelicula.id}" data-titulo="${escapar(pelicula.titulo)}"` })}
        ${botonSecundario("Descartar", {
          atributos: `data-descartar="${pelicula.id}" data-titulo="${escapar(pelicula.titulo)}"` })}
      </div>
  `, "flex flex-col overflow-hidden");
}
