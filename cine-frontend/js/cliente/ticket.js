import * as api from "../api.js";
import { botonSecundario } from "../componentes.js";
import { recordarCodigo } from "./compra.js";
import { avisar, escapar } from "../dom.js";
import { etiqueta } from "../etiquetas.js";
import { fechaHora, precio, precioExacto } from "../formato.js";

const LINEA = "=".repeat(44);

function campo(etiquetaTexto, valor) {
  return ` ${etiquetaTexto.padEnd(13)}: ${valor}`;
}

function centrar(texto) {
  return " ".repeat(Math.max(Math.floor((LINEA.length - texto.length) / 2), 0)) + texto;
}

function tarjetaCodigo(reserva) {
  if (!reserva.codigo) return "";
  const legible = `${reserva.codigo.slice(0, 4)} ${reserva.codigo.slice(4)}`;
  const usada = reserva.ingresadaEn;
  return `
    <div class="mt-4 rounded border-2 ${usada ? "border-slate-300 bg-slate-100 dark:border-slate-700 dark:bg-slate-800" : "border-slate-900 bg-white dark:border-slate-100 dark:bg-slate-900"} p-4 text-center">
      <p class="text-xs uppercase tracking-widest text-slate-500 dark:text-slate-400">Código de acceso</p>
      <p class="mt-1 font-mono text-3xl font-bold tracking-[0.2em] ${usada ? "text-slate-400 line-through dark:text-slate-500" : ""}">
        ${escapar(legible)}
      </p>
      <p class="mt-2 text-xs text-slate-500 dark:text-slate-400">
        ${usada
          ? `Ya se usó el ${escapar(fechaHora(usada))}`
          : "Mostralo en la puerta. Sirve una sola vez."}
      </p>
    </div>`;
}

function armarTicket(reserva) {
  return [
    LINEA,
    centrar("CINE UADE"),
    centrar("TICKET #" + reserva.id),
    LINEA,
    campo("Pelicula", reserva.pelicula.titulo),
    campo("Sala", `${reserva.sala.nombre} (${reserva.sala.tipo})`),
    campo("Funcion", fechaHora(reserva.funcion.inicio)),
    campo("Formato", `${reserva.funcion.proyeccion} ${reserva.funcion.idioma}`),
    campo("Cliente", reserva.cliente.nombre),
    LINEA,
    ...reserva.entradas.map((e) => campo(
      "Butaca " + e.codigo,
      precioExacto(e.precio) + (e.tarifa && e.tarifa !== "GENERAL" ? "  " + e.tarifa : ""))),
    LINEA,
    campo("Entradas", String(reserva.entradas.length)),
    campo("Total", precioExacto(reserva.total)),
    campo("Estado", reserva.estado),
    LINEA,
    centrar("CODIGO DE ACCESO"),
    centrar(reserva.codigo || ""),
    LINEA,
    centrar("Presentar en boleteria"),
    LINEA,
  ].join("\n");
}

function cartaCandy(productos, reservaId) {
  if (!productos.length) return "";
  return `
    <section class="mt-6 rounded border border-slate-300 bg-white p-4 dark:border-slate-700 dark:bg-slate-900">
      <h2 class="font-semibold">¿Pochoclos para la función?</h2>
      <p class="mb-3 text-sm text-slate-500 dark:text-slate-400">
        Comprá en el mostrador del candy antes de entrar. Si decís tu número de reserva
        (#${escapar(reservaId)}), la compra queda a tu nombre.
      </p>
      <ul class="divide-y divide-slate-200 text-sm dark:divide-slate-800">
        ${productos.map((p) => `
          <li class="flex items-baseline justify-between gap-3 py-1.5">
            <span>${escapar(p.nombre)}
              ${p.componentes?.length ? `<span class="block text-xs text-slate-500 dark:text-slate-400">${
                escapar(p.componentes.map((c) => `${c.cantidad}× ${c.nombre}`).join(" + "))}</span>` : ""}
            </span>
            <span class="whitespace-nowrap font-medium">${precio(p.precio)}</span>
          </li>`).join("")}
      </ul>
    </section>`;
}

export async function vistaTicket(contenedor, codigo) {
  const reserva = await api.obtenerReservaPorCodigo(decodeURIComponent(codigo));
  recordarCodigo(reserva.id, reserva.codigo);
  const productos = await api.obtenerProductosCandy().catch(() => []);
  const conAcreditacion = reserva.entradas.filter(
    (e) => e.tarifa && e.tarifa !== "GENERAL");

  contenedor.innerHTML = `
    ${reserva.estado === "CANCELADA" ? `
      <div class="rounded border border-slate-300 bg-slate-100 p-3 text-sm text-slate-700 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300">
        Esta reserva está cancelada.
      </div>` : `
      <div class="rounded border border-emerald-300 bg-emerald-50 p-3 text-sm text-emerald-900 dark:border-emerald-800 dark:bg-emerald-950 dark:text-emerald-300">
        Reserva confirmada. Presentá este comprobante en boletería.
      </div>`}

    ${tarjetaCodigo(reserva)}

    ${conAcreditacion.length ? `
      <div class="mt-3 rounded border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900 dark:border-amber-800 dark:bg-amber-950 dark:text-amber-300">
        <strong>Traé el carnet.</strong> En la puerta se acredita la tarifa de
        ${escapar(conAcreditacion.map((e) => `${e.codigo} (${etiqueta(e.tarifa).toLowerCase()})`).join(", "))}.
      </div>` : ""}

    <pre class="mt-4 overflow-x-auto rounded border border-slate-300 bg-white p-4 text-xs leading-5 dark:border-slate-700 dark:bg-slate-900">${escapar(armarTicket(reserva))}</pre>
    <div class="mt-4 flex gap-2">
      <a href="#/" class="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white dark:bg-white dark:text-slate-900">Volver a la cartelera</a>
      ${botonSecundario("Copiar", { tamano: "px-4 py-2", atributos: 'id="copiar"', clases: "dark:text-slate-100" })}
      ${reserva.estado === "RESERVADA"
        ? `<button type="button" id="cancelar"
             class="rounded border border-red-300 px-4 py-2 text-sm text-red-700 dark:border-red-800 dark:text-red-400">Cancelar reserva</button>`
        : ""}
    </div>

    ${cartaCandy(productos, reserva.id)}
  `;

  contenedor.querySelector("#copiar").addEventListener("click", async () => {
    await navigator.clipboard.writeText(armarTicket(reserva));
    avisar("Comprobante copiado");
  });

  contenedor.querySelector("#cancelar")?.addEventListener("click", async (evento) => {
    evento.target.disabled = true;
    try {
      await api.cancelarReservaPorCodigo(reserva.codigo);
      avisar("Reserva cancelada, las butacas quedaron libres");
    } catch (e) {
      avisar(e.message, "error");
    }
    vistaTicket(contenedor, codigo);
  });
}
