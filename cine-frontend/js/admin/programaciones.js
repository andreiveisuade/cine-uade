import * as api from "../api.js";
import { pantalla } from "../butacas.js";
import { boton, botonSecundario, campo, filaTabla, panel, select, tabla } from "../componentes.js";
import { avisar, escapar } from "../dom.js";
import { DIAS_SEMANA, etiqueta } from "../etiquetas.js";
import { dia, fechaHora, hoyISO, precio } from "../formato.js";

/* ------------------------------------------------------------ programaciones */

/**
 * CU-03b: la grilla. Un cine no carga quince funciones de a una, define
 * «Matrix en la Sala 1, todos los días a las 20:30, del 1 al 15».
 *
 * La pantalla tiene dos botones y un solo formulario, que es el punto: «Previsualizar»
 * muestra fecha por fecha qué va a pasar sin escribir nada, y «Confirmar» aplica. El
 * informe que devuelven los dos es el mismo, así que lo que se ve antes de confirmar es
 * literalmente lo que se va a guardar.
 *
 * El botón de confirmar arranca deshabilitado a propósito: se habilita recién cuando
 * hay una previsualización de esos mismos datos. Cambiar cualquier campo la invalida y
 * vuelve a apagarlo, porque un informe de otra grilla no dice nada de esta.
 */
export async function vistaProgramaciones(contenedor) {
  const [programaciones, peliculas, salas, idiomas, proyecciones] = await Promise.all([
    api.obtenerProgramaciones(), api.obtenerPeliculas(), api.obtenerSalas(),
    api.obtenerIdiomas(), api.obtenerProyecciones(),
  ]);

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Grilla de funciones</h1>
    <p class="mb-5 text-sm text-slate-500 dark:text-slate-400">
      Una grilla genera las funciones del rango de una sola vez. Las que chocan con algo
      ya programado en esa sala se saltean, y el informe dice cuáles.
    </p>

    <div class="mb-4 flex flex-wrap items-end gap-3">
      ${select({ nombre: "fpelicula", etiqueta: "Película", ancho: "block", opciones: `
          <option value="">Todas</option>
          ${peliculas.map((p) => `<option value="${p.id}">${escapar(p.titulo)}</option>`).join("")}` })}
      ${select({ nombre: "fsala", etiqueta: "Sala", ancho: "block", opciones: `
          <option value="">Todas</option>
          ${salas.map((s) => `<option value="${s.id}">${escapar(s.nombre)}</option>`).join("")}` })}
      ${select({ nombre: "factiva", etiqueta: "Estado", ancho: "block", opciones: `
          <option value="">Todas</option>
          <option value="true">Activas</option>
          <option value="false">Dadas de baja</option>` })}
      ${botonSecundario("Limpiar", { atributos: 'id="limpiarFiltros"' })}
      <p id="cuenta" class="text-sm text-slate-500 dark:text-slate-400"></p>
    </div>

    <div class="grid gap-4 lg:grid-cols-[1fr_340px]">
      ${panel(`
        ${tabla('<tr><th class="p-2">Película</th><th>Sala</th><th>Cuándo</th><th>Días</th><th class="text-right">Precio</th><th></th></tr>',
          filas(programaciones, peliculas, salas))}
        <p class="border-t border-slate-200 p-2 text-xs text-slate-500 dark:border-slate-800 dark:text-slate-400">
          Dar de baja una grilla <strong>no borra las funciones que ya generó</strong>:
          pueden tener entradas vendidas. Solo evita que genere nuevas. Hacé clic en una
          fila para ver qué funciones creó.
        </p>
        <div id="detalle"></div>
      `, "overflow-x-auto")}

      ${panel(`
        <h2 class="mb-3 font-semibold">Nueva grilla</h2>
        <form id="alta" class="space-y-3">
          ${select({ nombre: "peliculaId", etiqueta: "Película",
            opciones: peliculas.map((p) => `<option value="${p.id}">${escapar(p.titulo)} (${p.duracionMinutos}′)</option>`).join("") })}
          ${select({ nombre: "salaId", etiqueta: "Sala",
            opciones: salas.map((s) => `<option value="${s.id}">${escapar(s.nombre)} — ${etiqueta(s.tipo)}</option>`).join("") })}

          <div class="grid grid-cols-2 gap-2">
            ${campo({ nombre: "desde", etiqueta: "Desde", tipo: "date", valor: hoyISO(), requerido: true })}
            ${campo({ nombre: "hasta", etiqueta: 'Hasta <span class="text-slate-400 dark:text-slate-500">(vacío = sin fin)</span>', tipo: "date" })}
          </div>

          ${campo({ nombre: "horaInicio", etiqueta: "Hora de la función", tipo: "time", valor: "20:30", requerido: true })}

          <fieldset class="text-sm">
            <legend class="text-slate-600 dark:text-slate-300">Días (ninguno = todos)</legend>
            <div class="mt-1 flex flex-wrap gap-x-3 gap-y-1">
              ${DIAS_SEMANA.map((d) => `
                <label class="flex items-center gap-1 text-xs">
                  <input type="checkbox" name="dia" value="${d}" /> ${etiqueta(d).slice(0, 3)}
                </label>`).join("")}
            </div>
          </fieldset>

          <div class="grid grid-cols-2 gap-2">
            ${select({ nombre: "idioma", etiqueta: "Idioma",
              opciones: idiomas.map((i) => `<option value="${i}">${etiqueta(i)}</option>`).join("") })}
            ${select({ nombre: "proyeccion", etiqueta: "Proyección",
              opciones: proyecciones.map((p) => `<option value="${p}">${etiqueta(p)}</option>`).join("") })}
          </div>

          ${campo({ nombre: "precio", etiqueta: "Precio base", tipo: "number", valor: 5000, requerido: true, extra: 'min="100" step="100"' })}

          <div class="grid grid-cols-2 gap-2">
            ${botonSecundario("Previsualizar", { tamano: "px-4 py-2", clases: "font-medium", atributos: 'id="previsualizar"' })}
            ${boton("Confirmar", { atributos: 'id="confirmar" disabled',
              clases: "disabled:bg-slate-300 dark:disabled:bg-slate-700 dark:disabled:text-slate-400" })}
          </div>
          <p id="errorAlta" class="hidden text-sm text-red-700 dark:text-red-400"></p>
        </form>

        <div id="informe" class="mt-3"></div>

        <p class="mt-3 border-t border-slate-200 pt-3 text-xs text-slate-500 dark:border-slate-800 dark:text-slate-400">
          Al confirmar, el servidor <strong>vuelve a revisar</strong> cada fecha: entre que
          mirás el informe y confirmás, otro puede haber programado algo en esa sala.
        </p>
      `, "p-4")}
    </div>
  `;

  // --- filtros: los resuelve el backend ---
  const fpelicula = contenedor.querySelector("#fpelicula");
  const fsala = contenedor.querySelector("#fsala");
  const factiva = contenedor.querySelector("#factiva");
  const cuenta = contenedor.querySelector("#cuenta");

  async function aplicarFiltros() {
    const visibles = await api.obtenerProgramaciones({
      peliculaId: fpelicula.value, salaId: fsala.value, activa: factiva.value,
    });
    contenedor.querySelector("tbody").innerHTML = filas(visibles, peliculas, salas);
    cuenta.textContent = visibles.length === programaciones.length
      ? "" : `mostrando ${visibles.length} de ${programaciones.length}`;
  }

  [fpelicula, fsala, factiva].forEach((c) => c.addEventListener("change", aplicarFiltros));
  contenedor.querySelector("#limpiarFiltros").addEventListener("click", () => {
    fpelicula.value = ""; fsala.value = ""; factiva.value = "";
    aplicarFiltros();
  });

  const formulario = contenedor.querySelector("#alta");
  const errorAlta = contenedor.querySelector("#errorAlta");
  const informe = contenedor.querySelector("#informe");
  const botonConfirmar = contenedor.querySelector("#confirmar");

  function leerFormulario() {
    const datos = new FormData(formulario);
    return {
      peliculaId: datos.get("peliculaId"),
      salaId: datos.get("salaId"),
      desde: datos.get("desde"),
      // Vacío viaja como null: es una grilla abierta, no una fecha que falta.
      hasta: datos.get("hasta") || null,
      horaInicio: datos.get("horaInicio"),
      diasSemana: datos.getAll("dia"),
      idioma: datos.get("idioma"),
      proyeccion: datos.get("proyeccion"),
      precio: Number(datos.get("precio")),
    };
  }

  /** Una previsualización vale solo para los datos con los que se pidió. */
  function invalidarPrevisualizacion() {
    botonConfirmar.disabled = true;
    informe.innerHTML = "";
  }

  function mostrarError(mensaje) {
    errorAlta.textContent = mensaje;
    errorAlta.classList.remove("hidden");
  }

  formulario.addEventListener("input", invalidarPrevisualizacion);
  formulario.addEventListener("change", invalidarPrevisualizacion);

  contenedor.querySelector("#previsualizar").addEventListener("click", async () => {
    if (!formulario.reportValidity()) return;
    errorAlta.classList.add("hidden");
    try {
      const plan = await api.previsualizarProgramacion(leerFormulario());
      informe.innerHTML = dibujarInforme(plan, false);
      botonConfirmar.disabled = plan.generadas === 0;
    } catch (e) {
      invalidarPrevisualizacion();
      mostrarError(e.message);
    }
  });

  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    errorAlta.classList.add("hidden");
    try {
      const plan = await api.crearProgramacion(leerFormulario());
      avisar(`Grilla creada: ${plan.generadas} funciones`
        + (plan.salteadas ? `, ${plan.salteadas} salteadas` : ""));
      await vistaProgramaciones(contenedor);
      // El informe del alta sobrevive al repintado: es donde se lee qué quedó afuera.
      contenedor.querySelector("#informe").innerHTML = dibujarInforme(plan, true);
    } catch (e) {
      mostrarError(e.message);
    }
  });

  contenedor.addEventListener("click", async (evento) => {
    const baja = evento.target.closest("button[data-baja]");
    const alta = evento.target.closest("button[data-alta]");
    if (baja || alta) {
      try {
        if (baja) await api.darDeBajaProgramacion(baja.dataset.baja);
        else await api.darDeAltaProgramacion(alta.dataset.alta);
        vistaProgramaciones(contenedor);
      } catch (e) {
        avisar(e.message, "error");
      }
      return;
    }

    const fila = evento.target.closest("tr[data-ver]");
    if (!fila) return;
    try {
      const grilla = await api.obtenerProgramacion(fila.dataset.ver);
      contenedor.querySelector("#detalle").innerHTML = dibujarFuncionesGeneradas(grilla);
    } catch (e) {
      avisar(e.message, "error");
    }
  });
}

const tituloDe = (peliculas, id) =>
  peliculas.find((p) => p.id === id)?.titulo || `Película ${id}`;

const nombreDeSala = (salas, id) =>
  salas.find((s) => s.id === id)?.nombre || `Sala ${id}`;

/** Sin días no quiere decir ninguno: quiere decir todos los del rango. */
function diasDeLaGrilla(grilla) {
  return grilla.diasSemana?.length
    ? grilla.diasSemana.map((d) => etiqueta(d).slice(0, 3)).join(", ")
    : "todos";
}

/**
 * El informe, fecha por fecha. Es el mismo dibujo antes y después de confirmar porque
 * es el mismo dato: lo único que cambia es el encabezado.
 */
function dibujarInforme(plan, aplicado) {
  const titulo = aplicado
    ? `Se generaron ${plan.generadas} funciones`
    : `Se van a generar ${plan.generadas} funciones`;
  return `
    <div class="rounded border ${aplicado ? "border-emerald-300 bg-emerald-50 dark:border-emerald-800 dark:bg-emerald-950" : "border-slate-300 bg-slate-50 dark:border-slate-700 dark:bg-slate-800"} p-3">
      <p class="mb-2 text-sm font-semibold">
        ${escapar(titulo)}${plan.salteadas ? `, ${plan.salteadas} se ${aplicado ? "saltearon" : "saltean"}` : ""}
      </p>
      <ul class="max-h-64 space-y-1 overflow-y-auto text-xs">
        ${plan.funciones.map((f) => (f.choca
          ? `<li class="rounded border border-amber-300 bg-amber-50 px-2 py-1 text-amber-900 dark:border-amber-800 dark:bg-amber-950 dark:text-amber-300">
               <strong>${escapar(fechaHora(f.inicio))}</strong> · ${escapar(f.motivo || "se pisa con otra función")}
             </li>`
          : `<li class="px-2 py-1 text-slate-600 dark:text-slate-300">${escapar(fechaHora(f.inicio))}</li>`)).join("")}
      </ul>
    </div>`;
}

function dibujarFuncionesGeneradas(grilla) {
  if (!grilla.funciones?.length) {
    return `<p class="border-t border-slate-200 p-3 text-sm text-slate-500 dark:border-slate-800 dark:text-slate-400">
      Esta grilla no generó ninguna función: todas sus fechas chocaban con algo ya programado.
    </p>`;
  }
  return `
    <div class="border-t border-slate-200 p-3 dark:border-slate-800">
      <p class="mb-2 text-sm font-semibold">
        Funciones de la grilla ${grilla.id} (${grilla.funciones.length})
      </p>
      <div class="flex flex-wrap gap-1 text-xs">
        ${grilla.funciones.map((f) =>
          `<span class="rounded bg-slate-100 px-2 py-0.5 dark:bg-slate-700">${escapar(fechaHora(f.inicio))}</span>`).join("")}
      </div>
    </div>`;
}

/** Una fila por grilla. Separada para repintar solo el cuerpo al filtrar. */
function filas(programaciones, peliculas, salas) {
  if (!programaciones.length) {
    return `<tr><td colspan="6" class="p-6 text-center text-slate-500 dark:text-slate-400">
      Ninguna grilla coincide con el filtro.
    </td></tr>`;
  }
  return programaciones.map((p) => `
              <tr class="${filaTabla(`cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800 ${p.activa ? "" : "text-slate-400 dark:text-slate-500"}`)}"
                  data-ver="${p.id}">
                <td class="p-2 font-medium">${escapar(tituloDe(peliculas, p.peliculaId))}</td>
                <td class="whitespace-nowrap">${escapar(nombreDeSala(salas, p.salaId))}</td>
                <td class="whitespace-nowrap text-xs">
                  ${escapar(p.desde)} ${p.hasta ? `al ${escapar(p.hasta)}` : "<span class=\"font-medium\">en adelante</span>"} · <span class="font-medium">${escapar(p.horaInicio.slice(0, 5))}</span>
                  ${p.hasta ? "" : `<span class="block text-slate-500 dark:text-slate-400">generada hasta ${escapar(p.generadaHasta || "—")}</span>`}
                </td>
                <td class="text-xs">${escapar(diasDeLaGrilla(p))}</td>
                <td class="text-right whitespace-nowrap">${precio(p.precio)}</td>
                <td class="p-2 text-right whitespace-nowrap">
                  <button type="button" data-${p.activa ? "baja" : "alta"}="${p.id}"
                    class="text-xs ${p.activa ? "text-red-700 dark:text-red-400" : "text-emerald-700 dark:text-emerald-400"} hover:underline">
                    ${p.activa ? "Dar de baja" : "Reactivar"}
                  </button>
                </td>
              </tr>`).join("");
}
