import * as api from "../api.js";
import { boton, botonSecundario, campo, chip, filaTabla, imagenPoster, panel, select, tabla } from "../componentes.js";
import { avisar, conEspera, escapar } from "../dom.js";
import { chipClasificacion, etiqueta } from "../etiquetas.js";
import { duracion } from "../formato.js";

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

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Películas</h1>
    <p class="mb-5 text-sm text-slate-500 dark:text-slate-400">
      ${peliculas.length} cargadas · ${peliculas.filter((p) => p.enCartelera).length} publicadas.
      Una película llega a la cartelera cuando tiene funciones por delante; despublicarla
      la baja aunque las tenga.
    </p>

    <div class="mb-4 flex flex-wrap items-end gap-3">
      ${campo({ nombre: "fq", etiqueta: "Buscar", tipo: "search", placeholder: "título", ancho: "block w-64" })}
      ${select({ nombre: "fgenero", etiqueta: "Género", ancho: "block", opciones: `
          <option value="">Todos</option>
          ${generos.map((g) => `<option value="${g}">${etiqueta(g)}</option>`).join("")}` })}
      ${select({ nombre: "fpublicada", etiqueta: "Estado", ancho: "block", opciones: `
          <option value="">Todas</option>
          <option value="true">Publicadas</option>
          <option value="false">Despublicadas</option>` })}
      ${botonSecundario("Limpiar", { atributos: 'id="limpiar"' })}
      <p id="cuenta" class="text-sm text-slate-500 dark:text-slate-400"></p>
    </div>

    <div class="grid gap-4 lg:grid-cols-[1fr_340px]">
      ${panel(tabla(
        '<tr><th class="p-2"></th><th>Título</th><th>Duración</th><th>Edad</th><th>Géneros</th><th>Estado</th><th></th></tr>',
        filas(peliculas, editando?.id)), "overflow-x-auto")}

      ${panel(`
        <h2 class="mb-3 font-semibold">
          ${editando ? `Editar ${escapar(editando.titulo)}` : "Nueva película"}
        </h2>
        <form id="alta" class="space-y-3">
          ${campo({ nombre: "titulo", etiqueta: "Título", requerido: true, valor: editando?.titulo })}
          <div class="grid grid-cols-2 gap-2">
            ${campo({ nombre: "duracion", etiqueta: "Duración (min)", tipo: "number", requerido: true,
              valor: editando ? editando.duracionMinutos : "", extra: 'min="1"' })}
            ${campo({ nombre: "anio", etiqueta: "Año", tipo: "number", valor: editando?.anio || "", extra: 'min="1888"' })}
          </div>
          ${select({ nombre: "clasificacion", etiqueta: "Clasificación", opciones: clasificaciones.map((c) => `
                <option value="${c.nombre}" ${c.nombre === editando?.clasificacion ? "selected" : ""}>
                  ${etiqueta(c.nombre)}${c.edadMinima ? ` — desde ${c.edadMinima} años` : " — todo público"}
                </option>`).join("") })}
          ${campo({ nombre: "director", etiqueta: "Dirección", valor: editando?.director })}
          ${campo({ nombre: "idiomaOriginal", etiqueta: "Idioma original", valor: editando?.idiomaOriginal,
            pista: "El de la película, no el de la función." })}
          ${campo({ nombre: "sinopsis", etiqueta: "Sinopsis", tipo: "textarea", valor: editando?.sinopsis, extra: 'rows="3"' })}
          ${campo({ nombre: "posterUrl", etiqueta: "Poster (URL)", valor: editando?.posterUrl, placeholder: "https://…",
            pista: "Opcional. Sin poster se muestra la inicial del título." })}
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
            ${boton(editando ? "Guardar cambios" : "Agregar", { ancho: "flex-1" })}
            ${editando ? botonSecundario("Cancelar", { tamano: "px-4 py-2", atributos: 'id="cancelar"' }) : ""}
          </div>
        </form>
      `, "p-4")}
    </div>
  `;

  // --- filtros: los resuelve el backend, acá se juntan y se mandan ---
  const fq = contenedor.querySelector("#fq");
  const fgenero = contenedor.querySelector("#fgenero");
  const fpublicada = contenedor.querySelector("#fpublicada");
  const cuenta = contenedor.querySelector("#cuenta");
  const cuerpo = contenedor.querySelector("tbody");

  async function aplicarFiltros() {
    const visibles = await api.obtenerPeliculas({
      q: fq.value, genero: fgenero.value, publicada: fpublicada.value,
    });
    cuerpo.innerHTML = filas(visibles, editando?.id);
    cuenta.textContent = visibles.length === peliculas.length
      ? "" : `mostrando ${visibles.length} de ${peliculas.length}`;
  }

  fq.addEventListener("input", conEspera(aplicarFiltros));
  fgenero.addEventListener("change", aplicarFiltros);
  fpublicada.addEventListener("change", aplicarFiltros);
  contenedor.querySelector("#limpiar").addEventListener("click", () => {
    fq.value = ""; fgenero.value = ""; fpublicada.value = "";
    aplicarFiltros();
    fq.focus();
  });

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

/** Una fila por película. Separada para repintar solo el cuerpo al filtrar. */
function filas(peliculas, editandoId) {
  if (!peliculas.length) {
    return `<tr><td colspan="7" class="p-6 text-center text-sm text-slate-500 dark:text-slate-400">
      Ninguna película coincide con el filtro.
    </td></tr>`;
  }
  return peliculas.map((p) => `
              <tr class="${filaTabla(p.id === editandoId ? "bg-amber-50 dark:bg-amber-900/20" : "")}">
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
              </tr>`).join("");
}
