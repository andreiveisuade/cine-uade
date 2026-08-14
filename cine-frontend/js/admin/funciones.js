import * as api from "../api.js";
import { avisar, dia, escapar, etiqueta, hora, precio, error } from "../ui.js";

/* ------------------------------------------------------ programar funciones */

/**
 * @param destacada id de la función a la que saltar, si se llegó desde la agenda. La
 *                  tabla tiene ciento cincuenta filas: aterrizar arriba de todo y que el
 *                  usuario busque a mano la que acaba de clickear sería mandarlo dos
 *                  veces al mismo lugar.
 */
export async function vistaFunciones(contenedor, destacada) {
  const [funciones, peliculas, salas, tipos, idiomas, proyecciones] = await Promise.all([
    api.obtenerFunciones(), api.obtenerPeliculas(), api.obtenerSalas(),
    api.obtenerTiposSala(), api.obtenerIdiomas(), api.obtenerProyecciones(),
  ]);
  const soporta3D = new Map(tipos.map((t) => [t.nombre, t.soportaTresD]));

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Funciones</h1>
    <p class="mb-3 text-sm text-slate-500 dark:text-slate-400">
      ${funciones.length} programadas. Es la lista más larga del panel: una semana de seis salas
      pasa de cien funciones.
    </p>

    <div class="mb-4 flex flex-wrap items-end gap-3">
      <label class="text-sm">
        <span class="text-slate-600 dark:text-slate-300">Película</span>
        <select name="fpelicula" class="mt-1 block rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100">
          <option value="">Todas</option>
          ${peliculas.map((p) => `<option value="${p.id}">${escapar(p.titulo)}</option>`).join("")}
        </select>
      </label>
      <label class="text-sm">
        <span class="text-slate-600 dark:text-slate-300">Sala</span>
        <select name="fsala" class="mt-1 block rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100">
          <option value="">Todas</option>
          ${salas.map((s) => `<option value="${s.id}">${escapar(s.nombre)}</option>`).join("")}
        </select>
      </label>
      <label class="text-sm">
        <span class="text-slate-600 dark:text-slate-300">Desde</span>
        <input name="fdesde" type="date" class="mt-1 block rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100" />
      </label>
      <label class="text-sm">
        <span class="text-slate-600 dark:text-slate-300">Hasta</span>
        <input name="fhasta" type="date" class="mt-1 block rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100" />
      </label>
      <button type="button" id="limpiar" class="rounded border border-slate-400 px-3 py-1.5 text-sm dark:border-slate-600">Limpiar</button>
      <p id="cuenta" class="text-sm text-slate-500 dark:text-slate-400"></p>
    </div>

    <div class="grid gap-4 lg:grid-cols-[1fr_320px]">
      <section class="overflow-x-auto rounded border border-slate-300 bg-white dark:border-slate-700 dark:bg-slate-900">
        <table class="w-full text-sm">
          <thead class="border-b border-slate-300 bg-slate-50 text-left text-xs uppercase text-slate-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-400">
            <tr><th class="p-2">Cuándo</th><th>Película</th><th>Sala</th><th>Formato</th><th class="text-right">Precio</th><th></th></tr>
          </thead>
          <tbody>${filas(funciones)}</tbody>
        </table>
      </section>

      <section class="rounded border border-slate-300 bg-white dark:border-slate-700 dark:bg-slate-900 p-4">
        <h2 class="mb-3 font-semibold">Programar función</h2>
        <form id="alta" class="space-y-3">
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Película</span>
            <select name="peliculaId" class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500">
              ${peliculas.map((p) => `<option value="${p.id}">${escapar(p.titulo)} (${p.duracionMinutos}′)</option>`).join("")}
            </select>
          </label>
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Sala</span>
            <select name="salaId" class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500">
              ${salas.map((s) => `<option value="${s.id}">${escapar(s.nombre)} — ${etiqueta(s.tipo)}</option>`).join("")}
            </select>
          </label>
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Inicio</span>
            <input name="inicio" type="datetime-local" required
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
          </label>
          <div class="grid grid-cols-2 gap-2">
            <label class="block text-sm">
              <span class="text-slate-600 dark:text-slate-300">Idioma</span>
              <select name="idioma" class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500">
                ${idiomas.map((i) => `<option value="${i}">${etiqueta(i)}</option>`).join("")}
              </select>
            </label>
            <label class="block text-sm">
              <span class="text-slate-600 dark:text-slate-300">Proyección</span>
              <select name="proyeccion" class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500">
                ${proyecciones.map((p) => `<option value="${p}">${etiqueta(p)}</option>`).join("")}
              </select>
            </label>
          </div>
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Precio base</span>
            <input name="precio" type="number" min="100" step="100" required
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
          </label>
          <div id="avisos" class="space-y-1 text-xs"></div>
          <button type="submit"
            class="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white dark:bg-white dark:text-slate-900">Programar</button>
        </form>
      </section>
    </div>
  `;

  // --- filtros: el backend los resuelve, acá solo se juntan y se mandan ---
  const cuerpo = contenedor.querySelector("tbody");
  const cuenta = contenedor.querySelector("#cuenta");
  const controles = [...contenedor.querySelectorAll("[name^=f]")]
    .filter((c) => ["fpelicula", "fsala", "fdesde", "fhasta"].includes(c.name));

  async function aplicarFiltros() {
    const v = (nombre) => controles.find((c) => c.name === nombre).value;
    const visibles = await api.obtenerFunciones({
      peliculaId: v("fpelicula"), salaId: v("fsala"), desde: v("fdesde"), hasta: v("fhasta"),
    });
    cuerpo.innerHTML = filas(visibles);
    cuenta.textContent = visibles.length === funciones.length
      ? "" : `mostrando ${visibles.length} de ${funciones.length}`;
  }

  controles.forEach((c) => c.addEventListener("change", aplicarFiltros));
  contenedor.querySelector("#limpiar").addEventListener("click", () => {
    controles.forEach((c) => { c.value = ""; });
    aplicarFiltros();
  });

  const formulario = contenedor.querySelector("#alta");
  const avisos = contenedor.querySelector("#avisos");

  // R8 y R3 se validan en el backend; acá se anticipan para no mandar algo que va a fallar.
  function revisarReglas() {
    const sala = salas.find((s) => s.id === Number(formulario.salaId.value));
    const pelicula = peliculas.find((p) => p.id === Number(formulario.peliculaId.value));
    if (!sala || !pelicula) return;
    const mensajes = [];

    const opcion3D = [...formulario.proyeccion.options].find((o) => o.value === "TRES_D");
    const puede3D = soporta3D.get(sala?.tipo);
    opcion3D.disabled = !puede3D;
    if (!puede3D) {
      if (formulario.proyeccion.value === "TRES_D") formulario.proyeccion.value = "DOS_D";
      mensajes.push(["R8", `${sala.nombre} es ${etiqueta(sala.tipo)} y no puede proyectar en 3D.`]);
    }

    if (formulario.inicio.value && sala && pelicula) {
      const desde = new Date(formulario.inicio.value);
      const hasta = new Date(desde.getTime() + pelicula.duracionMinutos * 60000);
      const pisada = funciones.filter((f) => f.salaId === sala.id).find((f) => {
        const otraDesde = new Date(f.inicio);
        const otraHasta = new Date(otraDesde.getTime() + f.pelicula.duracionMinutos * 60000);
        return desde < otraHasta && otraDesde < hasta;
      });
      if (pisada) {
        mensajes.push(["R3", `Se pisa con ${pisada.pelicula.titulo} de las ${hora(pisada.inicio)}.`]);
      } else {
        mensajes.push(["ok", `Termina ${hora(hasta)} · sala libre en ese rango.`]);
      }
    }

    avisos.innerHTML = mensajes.map(([tipo, texto]) => {
      const clases = tipo === "ok"
        ? "border-emerald-300 bg-emerald-50 text-emerald-900 dark:border-emerald-800 dark:bg-emerald-950 dark:text-emerald-300"
        : "border-amber-300 bg-amber-50 text-amber-900 dark:border-amber-800 dark:bg-amber-950 dark:text-amber-300";
      const prefijo = tipo === "ok" ? "" : `<strong>${tipo}</strong> · `;
      return `<p class="rounded border px-2 py-1 ${clases}">${prefijo}${escapar(texto)}</p>`;
    }).join("");
  }

  formulario.addEventListener("change", revisarReglas);
  formulario.inicio.addEventListener("input", revisarReglas);
  revisarReglas();

  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const datos = new FormData(formulario);
    try {
      await api.programarFuncion({
        peliculaId: datos.get("peliculaId"),
        salaId: datos.get("salaId"),
        inicio: datos.get("inicio"),
        idioma: datos.get("idioma"),
        proyeccion: datos.get("proyeccion"),
        precio: Number(datos.get("precio")),
      });
      avisar("Función programada");
      vistaFunciones(contenedor);
    } catch (e) {
      avisar(e.message, "error");
    }
  });

  contenedor.querySelector("tbody").addEventListener("click", async (evento) => {
    const boton = evento.target.closest("button[data-borrar]");
    if (!boton) return;
    try {
      await api.eliminarFuncion(boton.dataset.borrar);
      avisar("Función borrada");
      vistaFunciones(contenedor);
    } catch (e) {
      avisar(e.message, "error");
    }
  });

  // El salto va al final, con la tabla ya pintada: antes de eso la fila no existe.
  const fila = destacada && contenedor.querySelector(`#funcion-${CSS.escape(String(destacada))}`);
  if (fila) {
    fila.scrollIntoView({ block: "center" });
    fila.classList.add("bg-amber-100", "dark:bg-amber-900/40");
  }
}

/** Una fila por función. Separada para repintar solo el cuerpo al filtrar. */
function filas(funciones) {
  if (!funciones.length) {
    return `<tr><td colspan="6" class="p-6 text-center text-sm text-slate-500 dark:text-slate-400">
      Ninguna función coincide con el filtro.
    </td></tr>`;
  }
  return funciones.map((f) => `
              <tr id="funcion-${f.id}" class="border-b border-slate-200 dark:border-slate-800">
                <td class="p-2 whitespace-nowrap">
                  ${escapar(dia(f.inicio))} <span class="font-medium">${hora(f.inicio)}</span>
                </td>
                <td>${escapar(f.pelicula.titulo)}</td>
                <td class="whitespace-nowrap">${escapar(f.sala.nombre)}</td>
                <td class="whitespace-nowrap">${etiqueta(f.proyeccion)} · ${etiqueta(f.idioma)}</td>
                <td class="text-right whitespace-nowrap">${precio(f.precio)}</td>
                <td class="p-2 text-right whitespace-nowrap">
                  <a href="#/funcion/${f.id}"
                    class="text-xs font-medium text-slate-900 hover:underline dark:text-slate-100">Informes</a>
                  <button type="button" data-borrar="${f.id}"
                    class="ml-2 text-xs text-red-700 hover:underline dark:text-red-400">Borrar</button>
                </td>
              </tr>`).join("");
}
