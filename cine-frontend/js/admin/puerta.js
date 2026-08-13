import * as api from "../api.js";
import { escapar, etiqueta, fechaHora } from "../ui.js";

/* -------------------------------------------------------------------- puerta */

/**
 * CU-18: lo que usa el acomodador. Se escanea o se tipea el código de la reserva y se
 * marca la entrada como usada.
 *
 * El foco vuelve al campo después de cada validación porque en la puerta se encadenan
 * una atrás de otra: obligar a hacer clic entre persona y persona sería insufrible.
 */
export async function vistaPuerta(contenedor) {
  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Validar entrada</h1>
    <p class="mb-5 text-sm text-slate-500 dark:text-slate-400">
      Escaneá el código del ticket o tipealo. Cada entrada sirve una sola vez.
    </p>

    <form id="validar" class="flex flex-wrap gap-2">
      <input name="codigo" required autocomplete="off" autofocus placeholder="A1B2C3D4"
        class="w-48 rounded border border-slate-400 px-3 py-2 font-mono text-lg uppercase tracking-widest dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
      <button type="submit" class="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white dark:bg-white dark:text-slate-900">
        Validar
      </button>
    </form>

    <div id="resultado" class="mt-5"></div>
  `;

  const formulario = contenedor.querySelector("#validar");
  const campoCodigo = formulario.querySelector("input[name=codigo]");
  const resultado = contenedor.querySelector("#resultado");

  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const codigo = campoCodigo.value.trim().toUpperCase();
    if (!codigo) return;
    try {
      const reserva = await api.validarEntrada(codigo);
      resultado.innerHTML = entradaValida(reserva);
    } catch (e) {
      // Los tres motivos —código inexistente, sin pagar y ya usada— se muestran igual
      // de fuerte: en la puerta lo único que importa es que no pasa.
      resultado.innerHTML = `
        <div class="rounded border-2 border-red-400 bg-red-50 p-4 dark:border-red-700 dark:bg-red-950">
          <p class="text-lg font-bold text-red-900 dark:text-red-300">NO PASA</p>
          <p class="mt-1 text-sm text-red-900 dark:text-red-300">${escapar(e.message)}</p>
        </div>`;
    }
    campoCodigo.value = "";
    campoCodigo.focus();
  });
}

function entradaValida(reserva) {
  const butacas = reserva.entradas.map((e) => {
    const pideCarnet = e.tarifa && e.tarifa !== "GENERAL";
    return `
      <li class="flex items-center justify-between border-t border-emerald-200 py-1 dark:border-emerald-800">
        <span class="font-mono font-semibold">${escapar(e.codigo)}</span>
        <span class="${pideCarnet ? "font-semibold text-amber-800 dark:text-amber-300" : "text-slate-600 dark:text-slate-300"}">
          ${etiqueta(e.tarifa || "GENERAL")}${pideCarnet ? " · pedir carnet" : ""}
        </span>
      </li>`;
  }).join("");

  return `
    <div class="rounded border-2 border-emerald-500 bg-emerald-50 p-4 dark:border-emerald-700 dark:bg-emerald-950">
      <p class="text-lg font-bold text-emerald-900 dark:text-emerald-300">ADELANTE</p>
      <p class="mt-2 font-semibold">${escapar(reserva.pelicula?.titulo || "")}</p>
      <p class="text-sm text-slate-700 dark:text-slate-200">
        ${escapar(reserva.sala?.nombre || "")} ·
        ${escapar(fechaHora(reserva.funcion?.inicio))}
      </p>
      <ul class="mt-3 text-sm">${butacas}</ul>
      <p class="mt-3 text-xs text-slate-600 dark:text-slate-300">
        ${reserva.entradas.length} persona${reserva.entradas.length === 1 ? "" : "s"} ·
        ingreso registrado ${escapar(fechaHora(reserva.ingresadaEn))}
      </p>
    </div>`;
}
