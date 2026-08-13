import * as api from "../api.js";
import { avisar, chip, chipClasificacion, duracion, escapar, etiqueta, imagenPoster, error } from "../ui.js";

/* ---------------------------------------------------------- ABM de películas */

// El mismo formulario sirve para alta y edición: null es alta, un id es edición.
export async function vistaPeliculas(contenedor, editandoId = null) {
  const [peliculas, generos, clasificaciones] = await Promise.all([
    api.obtenerPeliculas(),
    api.obtenerGeneros(),
    api.obtenerClasificaciones(),
  ]);
  const editando = editandoId
    ? peliculas.find((p) => p.id === Number(editandoId))
    : null;
  const valor = (campo) => escapar(editando?.[campo] ?? "");

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Películas</h1>
    <p class="mb-5 text-sm text-slate-500 dark:text-slate-400">
      ${peliculas.length} cargadas · ${peliculas.filter((p) => p.enCartelera).length} publicadas.
      Una película llega a la cartelera cuando tiene funciones por delante; despublicarla
      la baja aunque las tenga.
    </p>

    <div class="grid gap-4 lg:grid-cols-[1fr_340px]">
      <section class="overflow-x-auto rounded border border-slate-300 bg-white dark:border-slate-700 dark:bg-slate-900">
        <table class="w-full text-sm">
          <thead class="border-b border-slate-300 bg-slate-50 text-left text-xs uppercase text-slate-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-400">
            <tr><th class="p-2"></th><th>Título</th><th>Duración</th><th>Edad</th><th>Géneros</th><th>Estado</th><th></th></tr>
          </thead>
          <tbody>
            ${peliculas.map((p) => `
              <tr class="border-b border-slate-200 dark:border-slate-800 ${p.id === editando?.id ? "bg-amber-50 dark:bg-amber-900/20" : ""}">
                <td class="p-2">${imagenPoster(p, "h-12 w-8 rounded")}</td>
                <td>
                  <span class="font-medium">${escapar(p.titulo)}</span>
                  <span class="block text-xs text-slate-500 dark:text-slate-400">
                    ${[p.anio || null, p.director || null].filter(Boolean).map(escapar).join(" · ")}
                  </span>
                </td>
                <td class="whitespace-nowrap">${duracion(p.duracionMinutos)}</td>
                <td>${chipClasificacion(p.clasificacion)}</td>
                <td class="py-1">
                  <div class="flex flex-wrap gap-1">${p.generos.map((g) => chip(etiqueta(g))).join("")}</div>
                </td>
                <td>
                  <button type="button" data-cartelera="${p.id}"
                    title="${p.enCartelera
                      ? "Publicada: aparece en la cartelera si tiene funciones por delante"
                      : "Despublicada: no aparece aunque tenga funciones"}"
                    class="rounded-full px-2 py-0.5 text-xs font-medium ${p.enCartelera
                      ? "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300"
                      : "bg-slate-200 text-slate-600 dark:bg-slate-700 dark:text-slate-300"}">
                    ${p.enCartelera ? "Publicada" : "Despublicada"}
                  </button>
                </td>
                <td class="p-2 text-right whitespace-nowrap">
                  <button type="button" data-editar="${p.id}"
                    class="text-xs text-slate-700 hover:underline dark:text-slate-200">Editar</button>
                  <button type="button" data-borrar="${p.id}"
                    class="ml-2 text-xs text-red-700 hover:underline dark:text-red-400">Borrar</button>
                </td>
              </tr>`).join("")}
          </tbody>
        </table>
      </section>

      <section class="rounded border border-slate-300 bg-white dark:border-slate-700 dark:bg-slate-900 p-4">
        <h2 class="mb-3 font-semibold">
          ${editando ? `Editar ${escapar(editando.titulo)}` : "Nueva película"}
        </h2>
        <form id="alta" class="space-y-3">
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Título</span>
            <input name="titulo" required value="${valor("titulo")}"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
          </label>
          <div class="grid grid-cols-2 gap-2">
            <label class="block text-sm">
              <span class="text-slate-600 dark:text-slate-300">Duración (min)</span>
              <input name="duracion" type="number" min="1" required
                value="${editando ? editando.duracionMinutos : ""}"
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
            </label>
            <label class="block text-sm">
              <span class="text-slate-600 dark:text-slate-300">Año</span>
              <input name="anio" type="number" min="1888" value="${editando?.anio || ""}"
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
            </label>
          </div>
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Clasificación</span>
            <select name="clasificacion" class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500">
              ${clasificaciones.map((c) => `
                <option value="${c.nombre}" ${c.nombre === editando?.clasificacion ? "selected" : ""}>
                  ${etiqueta(c.nombre)}${c.edadMinima ? ` — desde ${c.edadMinima} años` : " — todo público"}
                </option>`).join("")}
            </select>
          </label>
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Dirección</span>
            <input name="director" value="${valor("director")}"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
          </label>
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Idioma original</span>
            <input name="idiomaOriginal" value="${valor("idiomaOriginal")}"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
            <span class="text-xs text-slate-500 dark:text-slate-400">El de la película, no el de la función.</span>
          </label>
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Sinopsis</span>
            <textarea name="sinopsis" rows="3"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500">${valor("sinopsis")}</textarea>
          </label>
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Poster (URL)</span>
            <input name="posterUrl" value="${valor("posterUrl")}" placeholder="https://…"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
            <span class="text-xs text-slate-500 dark:text-slate-400">Opcional. Sin poster se muestra la inicial del título.</span>
          </label>
          <fieldset class="text-sm">
            <legend class="text-slate-600 dark:text-slate-300">Géneros (al menos uno)</legend>
            <div class="mt-1 grid grid-cols-2 gap-1">
              ${generos.map((g) => `
                <label class="flex items-center gap-1 text-xs">
                  <input type="checkbox" name="genero" value="${g}"
                    ${editando?.generos.includes(g) ? "checked" : ""} /> ${etiqueta(g)}
                </label>`).join("")}
            </div>
          </fieldset>
          <label class="flex items-center gap-2 text-sm">
            <input type="checkbox" name="enCartelera" ${!editando || editando.enCartelera ? "checked" : ""} />
            <span class="text-slate-600 dark:text-slate-300">Publicada</span>
          </label>
          <div class="flex gap-2">
            <button type="submit"
              class="flex-1 rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white dark:bg-white dark:text-slate-900">
              ${editando ? "Guardar cambios" : "Agregar"}
            </button>
            ${editando
              ? `<button type="button" id="cancelar"
                   class="rounded border border-slate-400 px-4 py-2 text-sm dark:border-slate-600">Cancelar</button>`
              : ""}
          </div>
        </form>
      </section>
    </div>
  `;

  const formulario = contenedor.querySelector("#alta");
  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const datos = new FormData(formulario);
    const campos = {
      titulo: datos.get("titulo"),
      duracionMinutos: Number(datos.get("duracion")),
      generos: datos.getAll("genero"),
      clasificacion: datos.get("clasificacion"),
      posterUrl: datos.get("posterUrl"),
      director: datos.get("director"),
      anio: datos.get("anio"),
      idiomaOriginal: datos.get("idiomaOriginal"),
      sinopsis: datos.get("sinopsis"),
      enCartelera: datos.get("enCartelera") !== null,
    };
    try {
      if (editando) {
        await api.actualizarPelicula(editando.id, campos);
        avisar("Cambios guardados");
      } else {
        await api.crearPelicula(campos);
        avisar("Película agregada");
      }
      vistaPeliculas(contenedor);
    } catch (e) {
      avisar(e.message, "error");
    }
  });

  contenedor.querySelector("#cancelar")?.addEventListener("click", () => vistaPeliculas(contenedor));

  // El listener va en el tbody, que se reemplaza en cada render: colgarlo del
  // contenedor lo acumularía una vez por refresco.
  contenedor.querySelector("tbody").addEventListener("click", async (evento) => {
    const editar = evento.target.closest("button[data-editar]");
    if (editar) return vistaPeliculas(contenedor, editar.dataset.editar);

    const alternar = evento.target.closest("button[data-cartelera]");
    if (alternar) {
      const pelicula = peliculas.find((p) => p.id === Number(alternar.dataset.cartelera));
      try {
        await api.actualizarPelicula(pelicula.id, { enCartelera: !pelicula.enCartelera });
        avisar(pelicula.enCartelera ? "Despublicada" : "Publicada");
        vistaPeliculas(contenedor, editandoId);
      } catch (e) {
        avisar(e.message, "error");
      }
      return;
    }

    const borrar = evento.target.closest("button[data-borrar]");
    if (!borrar) return;
    try {
      await api.eliminarPelicula(borrar.dataset.borrar);
      avisar("Película borrada");
      vistaPeliculas(contenedor);
    } catch (e) {
      avisar(e.message, "error");
    }
  });
}
