import * as api from "../api.js";
import { ir } from "../router.js";
import { avisar, chipEstado, dia, escapar, etiqueta, fechaHora, hora, precio, error } from "../ui.js";
import { clienteRecordado } from "./compra.js";

/* --------------------------------------------------------------- mis reservas */

// El cliente no inicia sesión: recupera sus reservas con el email que dejó al comprar.
export async function vistaMisReservas(contenedor, emailBuscado) {
  // Sin email en la URL, se usa el del navegador: quien ya compró no vuelve a tipearlo.
  const email = emailBuscado
    ? decodeURIComponent(emailBuscado)
    : (clienteRecordado()?.email || "");
  const reservas = email ? await api.obtenerReservasDe(email) : null;

  const tarjetas = (reservas || []).map((r) => `
    <article class="rounded border border-slate-300 bg-white p-4 ${r.estado === "CANCELADA" ? "opacity-60" : ""}">
      <div class="flex flex-wrap items-start justify-between gap-2">
        <div>
          <h2 class="font-semibold">${escapar(r.pelicula?.titulo || "—")}</h2>
          <p class="text-sm text-slate-600">
            ${r.funcion
              ? `${escapar(dia(r.funcion.inicio))} ${hora(r.funcion.inicio)} ·
                 ${escapar(r.sala.nombre)} (${etiqueta(r.sala.tipo)}) ·
                 ${etiqueta(r.funcion.proyeccion)} · ${etiqueta(r.funcion.idioma)}`
              : ""}
          </p>
        </div>
        <div class="text-right">
          ${chipEstado(r.estado)}
          <p class="mt-1 font-semibold">${precio(r.total)}</p>
        </div>
      </div>
      <p class="mt-2 text-sm">
        <span class="text-slate-500">Butacas:</span>
        <span class="font-mono">${r.entradas.map((e) => e.codigo).join(", ")}</span>
      </p>
      ${r.pago
        ? `<p class="mt-1 text-xs text-slate-500">
             Pagada con ${etiqueta(r.pago.medio)} el ${escapar(fechaHora(r.pago.fecha))}</p>`
        : ""}
      <div class="mt-3 flex gap-2">
        <a href="#/ticket/${r.id}" class="rounded border border-slate-400 px-3 py-1 text-sm">Ver ticket</a>
        ${r.estado === "RESERVADA"
          ? `<button type="button" data-cancelar="${r.id}"
               class="rounded border border-red-300 px-3 py-1 text-sm text-red-700">Cancelar</button>`
          : ""}
      </div>
    </article>
  `).join("");

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Mis reservas</h1>
    <p class="mb-5 text-sm text-slate-500">
      Buscá con el email que dejaste al comprar.
    </p>

    <form id="buscar" class="mb-5 flex flex-wrap gap-2">
      <input name="email" type="email" required value="${escapar(email)}"
        placeholder="tu@email.com"
        class="min-w-64 flex-1 rounded border border-slate-400 px-2 py-1.5" />
      <button type="submit" class="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white">
        Buscar
      </button>
    </form>

    ${reservas === null
      ? ""
      : reservas.length
        ? `<div class="space-y-3">${tarjetas}</div>`
        : `<p class="py-8 text-center text-slate-500">
             No hay reservas a nombre de ${escapar(email)}.</p>`}
  `;

  contenedor.querySelector("#buscar").addEventListener("submit", (evento) => {
    evento.preventDefault();
    const valor = new FormData(evento.target).get("email").trim();
    ir(`#/mis-reservas/${encodeURIComponent(valor)}`);
  });

  contenedor.addEventListener("click", async function alCancelar(evento) {
    const boton = evento.target.closest("button[data-cancelar]");
    if (!boton) return;
    contenedor.removeEventListener("click", alCancelar);
    try {
      await api.cancelarReserva(boton.dataset.cancelar);
      avisar("Reserva cancelada, las butacas quedaron libres");
    } catch (e) {
      avisar(e.message, "error");
    }
    vistaMisReservas(contenedor, emailBuscado);
  });
}
