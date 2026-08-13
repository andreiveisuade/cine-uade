import * as api from "../api.js";
import { DIAS_SEMANA, avisar, dia, error, escapar, etiqueta, hora, hoyISO, precio } from "../ui.js";

/* -------------------------------------------------------------- promociones */

/** Cómo se lee el beneficio de cada tipo, que es lo que las diferencia. */
function beneficioDe(promocion) {
  if (promocion.tipo === "PORCENTAJE") return `${promocion.porcentaje}% off`;
  if (promocion.tipo === "MONTO_FIJO") return `${precio(promocion.monto)} off`;
  return `${promocion.lleva}x${promocion.paga}`;
}

function condicionesDe(promocion) {
  const partes = [];
  if (promocion.diasSemana?.length) {
    partes.push(promocion.diasSemana.map((d) => etiqueta(d).slice(0, 3)).join(", "));
  }
  if (promocion.horaDesde || promocion.horaHasta) {
    partes.push(`${(promocion.horaDesde || "00:00").slice(0, 5)}–${(promocion.horaHasta || "23:59").slice(0, 5)}`);
  }
  if (promocion.mediosPago?.length) partes.push(promocion.mediosPago.map(etiqueta).join(", "));
  // Sin condiciones no quiere decir "ninguna": quiere decir que corre siempre.
  return partes.length ? partes.join(" · ") : "todos los días, cualquier medio";
}

export async function vistaPromociones(contenedor) {
  const [promociones, mediosPago] = await Promise.all([
    api.obtenerPromociones(),
    api.obtenerMediosPago(),
  ]);

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Promociones</h1>
    <p class="mb-5 text-sm text-slate-500 dark:text-slate-400">
      ${promociones.filter((p) => p.activa).length} activas de ${promociones.length}.
      No se acumulan: en cada cobro se aplica la que más descuenta.
    </p>

    <div class="grid gap-4 lg:grid-cols-[1fr_340px]">
      <section class="overflow-x-auto rounded border border-slate-300 bg-white dark:border-slate-700 dark:bg-slate-900">
        <table class="w-full text-sm">
          <thead class="border-b border-slate-300 bg-slate-50 text-left text-xs uppercase text-slate-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-400">
            <tr><th class="p-2">Nombre</th><th>Beneficio</th><th>Vigencia</th><th>Cuándo</th><th></th></tr>
          </thead>
          <tbody>
            ${promociones.length ? promociones.map((p) => `
              <tr class="border-b border-slate-200 dark:border-slate-800 ${p.activa ? "" : "text-slate-400 dark:text-slate-500"}">
                <td class="p-2 font-medium">${escapar(p.nombre)}</td>
                <td class="whitespace-nowrap font-semibold">${escapar(beneficioDe(p))}</td>
                <td class="whitespace-nowrap text-xs">
                  ${escapar(p.vigenciaDesde)} al ${escapar(p.vigenciaHasta)}
                </td>
                <td class="text-xs">${escapar(condicionesDe(p))}</td>
                <td class="p-2 text-right whitespace-nowrap">
                  <button type="button" data-${p.activa ? "baja" : "alta"}="${p.id}"
                    class="text-xs ${p.activa ? "text-red-700 dark:text-red-400" : "text-emerald-700 dark:text-emerald-400"} hover:underline">
                    ${p.activa ? "Dar de baja" : "Reactivar"}
                  </button>
                </td>
              </tr>`).join("")
              : '<tr><td colspan="5" class="p-6 text-center text-slate-500 dark:text-slate-400">Todavía no hay promociones.</td></tr>'}
          </tbody>
        </table>
        <p class="border-t border-slate-200 p-2 text-xs text-slate-500 dark:border-slate-800 dark:text-slate-400">
          Las promociones no se borran: se dan de baja. Una que ya se usó en un cobro
          tiene que seguir existiendo para poder explicar por qué se cobró ese monto.
        </p>
      </section>

      <section class="rounded border border-slate-300 bg-white dark:border-slate-700 dark:bg-slate-900 p-4">
        <h2 class="mb-3 font-semibold">Nueva promoción</h2>
        <form id="alta" class="space-y-3">
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Nombre</span>
            <input name="nombre" required placeholder="Miércoles 2x1"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
          </label>

          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Tipo</span>
            <select name="tipo" id="tipo" class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500">
              <option value="PORCENTAJE">Porcentaje</option>
              <option value="MONTO_FIJO">Monto fijo</option>
              <option value="NXM">NxM (2x1)</option>
            </select>
          </label>

          <div data-campos="PORCENTAJE">
            <label class="block text-sm">
              <span class="text-slate-600 dark:text-slate-300">Porcentaje de descuento</span>
              <input name="porcentaje" type="number" min="1" max="99" value="30"
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
            </label>
          </div>
          <div data-campos="MONTO_FIJO" class="hidden">
            <label class="block text-sm">
              <span class="text-slate-600 dark:text-slate-300">Monto a descontar</span>
              <input name="monto" type="number" min="1" value="2000"
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
            </label>
          </div>
          <div data-campos="NXM" class="hidden grid-cols-2 gap-2">
            <label class="block text-sm">
              <span class="text-slate-600 dark:text-slate-300">Lleva</span>
              <input name="lleva" type="number" min="2" value="2"
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
            </label>
            <label class="block text-sm">
              <span class="text-slate-600 dark:text-slate-300">Paga</span>
              <input name="paga" type="number" min="1" value="1"
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
            </label>
          </div>

          <div class="grid grid-cols-2 gap-2">
            <label class="block text-sm">
              <span class="text-slate-600 dark:text-slate-300">Desde</span>
              <input name="vigenciaDesde" type="date" required value="${hoyISO()}"
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
            </label>
            <label class="block text-sm">
              <span class="text-slate-600 dark:text-slate-300">Hasta</span>
              <input name="vigenciaHasta" type="date" required
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
            </label>
          </div>

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
            <label class="block text-sm">
              <span class="text-slate-600 dark:text-slate-300">Desde hora</span>
              <input name="horaDesde" type="time"
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
            </label>
            <label class="block text-sm">
              <span class="text-slate-600 dark:text-slate-300">Hasta hora</span>
              <input name="horaHasta" type="time"
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
            </label>
          </div>

          <fieldset class="text-sm">
            <legend class="text-slate-600 dark:text-slate-300">Medios (ninguno = cualquiera)</legend>
            <div class="mt-1 flex flex-wrap gap-x-3 gap-y-1">
              ${mediosPago.map((m) => `
                <label class="flex items-center gap-1 text-xs">
                  <input type="checkbox" name="medio" value="${m.nombre}" /> ${etiqueta(m.nombre)}
                </label>`).join("")}
            </div>
          </fieldset>

          <button type="submit"
            class="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white dark:bg-white dark:text-slate-900">
            Crear promoción
          </button>
          <p id="errorAlta" class="hidden text-sm text-red-700 dark:text-red-400"></p>
        </form>
        <p class="mt-3 border-t border-slate-200 pt-3 text-xs text-slate-500 dark:border-slate-800 dark:text-slate-400">
          Las condiciones se evalúan contra el horario de la <strong>función</strong>, no
          contra el momento de la compra: un 2x1 de los miércoles vale para la función del
          miércoles aunque las entradas se compren el lunes.
        </p>
      </section>
    </div>
  `;

  const formulario = contenedor.querySelector("#alta");
  const errorAlta = contenedor.querySelector("#errorAlta");
  const selectorTipo = contenedor.querySelector("#tipo");

  // El formulario es uno solo y muestra los campos del tipo elegido, igual que la tabla
  // tiene una columna por beneficio y deja en null las que no aplican.
  function mostrarCamposDelTipo() {
    contenedor.querySelectorAll("[data-campos]").forEach((bloque) => {
      const visible = bloque.dataset.campos === selectorTipo.value;
      bloque.classList.toggle("hidden", !visible);
      if (bloque.dataset.campos === "NXM") bloque.classList.toggle("grid", visible);
    });
  }
  selectorTipo.addEventListener("change", mostrarCamposDelTipo);
  mostrarCamposDelTipo();

  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const datos = new FormData(formulario);
    const tipo = datos.get("tipo");
    const marcados = (nombre) => datos.getAll(nombre);
    try {
      await api.crearPromocion({
        nombre: datos.get("nombre"),
        tipo,
        porcentaje: tipo === "PORCENTAJE" ? Number(datos.get("porcentaje")) : null,
        monto: tipo === "MONTO_FIJO" ? Number(datos.get("monto")) : null,
        lleva: tipo === "NXM" ? Number(datos.get("lleva")) : null,
        paga: tipo === "NXM" ? Number(datos.get("paga")) : null,
        vigenciaDesde: datos.get("vigenciaDesde"),
        vigenciaHasta: datos.get("vigenciaHasta"),
        diasSemana: marcados("dia"),
        horaDesde: datos.get("horaDesde") || null,
        horaHasta: datos.get("horaHasta") || null,
        mediosPago: marcados("medio"),
      });
      avisar("Promoción creada");
      vistaPromociones(contenedor);
    } catch (e) {
      errorAlta.textContent = e.message;
      errorAlta.classList.remove("hidden");
    }
  });

  contenedor.addEventListener("click", async (evento) => {
    const baja = evento.target.closest("button[data-baja]");
    const alta = evento.target.closest("button[data-alta]");
    if (!baja && !alta) return;
    try {
      if (baja) await api.darDeBajaPromocion(baja.dataset.baja);
      else await api.darDeAltaPromocion(alta.dataset.alta);
      vistaPromociones(contenedor);
    } catch (e) {
      avisar(e.message, "error");
    }
  });
}
