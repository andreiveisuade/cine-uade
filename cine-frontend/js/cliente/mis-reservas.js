import * as api from "../api.js";
import { ir } from "../router.js";
import { boton, panel } from "../componentes.js";
import { avisar, escapar } from "../dom.js";
import { chipEstado, etiqueta } from "../etiquetas.js";
import { dia, fechaHora, hora, precio } from "../formato.js";
import { clienteRecordado } from "./compra.js";

/* --------------------------------------------------------------- mis reservas */

// El cliente no inicia sesión: recupera sus reservas con el email que dejó al comprar.
export async function vistaMisReservas(contenedor, emailBuscado) {
  // Sin email en la URL, se usa el del navegador: quien ya compró no vuelve a tipearlo.
  const email = emailBuscado
    ? decodeURIComponent(emailBuscado)
    : (clienteRecordado()?.email || "");
  const reservas = email ? await api.obtenerReservasDe(email) : null;

  const tarjetas = (reservas || []).map((r) => panel(`
      <div class="flex flex-wrap items-start justify-between gap-2">
        <div>
          <h2 class="font-semibold">${escapar(r.pelicula?.titulo || "—")}</h2>
          <p class="text-sm text-slate-600 dark:text-slate-300">
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
        <span class="text-slate-500 dark:text-slate-400">Butacas:</span>
        <span class="font-mono">${r.entradas.map((e) => e.codigo).join(", ")}</span>
      </p>
      ${r.pago
        ? `<p class="mt-1 text-xs text-slate-500 dark:text-slate-400">
             Pagada con ${etiqueta(r.pago.medio)} el ${escapar(fechaHora(r.pago.fecha))}</p>`
        : ""}
      <div class="mt-3 flex gap-2">
        <a href="#/ticket/${r.id}" class="rounded border border-slate-400 px-3 py-1 text-sm dark:border-slate-600 dark:text-slate-100">Ver ticket</a>
        ${r.estado === "RESERVADA"
          ? `<button type="button" data-cancelar="${r.id}"
               class="rounded border border-red-300 px-3 py-1 text-sm text-red-700 dark:border-red-800 dark:text-red-400">Cancelar</button>`
          : ""}
      </div>
    `, `p-4 ${r.estado === "CANCELADA" ? "opacity-60" : ""}`)).join("");

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Mis reservas</h1>
    <p class="mb-5 text-sm text-slate-500 dark:text-slate-400">
      Buscá con el email que dejaste al comprar.
    </p>

    <form id="buscar" class="mb-5 flex flex-wrap gap-2">
      <input name="email" type="email" required value="${escapar(email)}"
        placeholder="tu@email.com"
        class="min-w-64 flex-1 rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100" />
      ${boton("Buscar", { ancho: "" })}
    </form>

    ${reservas === null
      ? ""
      : reservas.length
        ? `<div class="space-y-3">${tarjetas}</div>`
        : `<p class="py-8 text-center text-slate-500 dark:text-slate-400">
             No hay reservas a nombre de ${escapar(email)}.</p>`}
  `;

  contenedor.querySelector("#buscar").addEventListener("submit", (evento) => {
    evento.preventDefault();
    const valor = new FormData(evento.target).get("email").trim();
    ir(`#/mis-reservas/${encodeURIComponent(valor)}`);
  });

  // El listener va en cada botón y no en el contenedor: los botones los borra el
  // innerHTML de arriba, el contenedor no. Enganchado al contenedor, cada búsqueda
  // sumaba un listener más al mismo elemento vivo —y esta vista se vuelve a dibujar
  // sola al cancelar—, así que un clic terminaba disparando una cancelación por cada
  // vez que se había pasado por acá.
  contenedor.querySelectorAll("button[data-cancelar]").forEach((botonCancelar) => {
    botonCancelar.addEventListener("click", async () => {
      // Cancelar tarda: sin esto, dos clics apurados son dos pedidos.
      botonCancelar.disabled = true;
      try {
        await api.cancelarReserva(botonCancelar.dataset.cancelar);
        avisar("Reserva cancelada, las butacas quedaron libres");
      } catch (e) {
        avisar(e.message, "error");
      }
      vistaMisReservas(contenedor, emailBuscado);
    });
  });
}
