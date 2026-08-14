import * as api from "../api.js";
import { botonSecundario } from "../componentes.js";
import { avisar, escapar } from "../dom.js";
import { etiqueta } from "../etiquetas.js";
import { fechaHora, precio, precioExacto } from "../formato.js";

/* -------------------------------------------------------------------- ticket */

const LINEA = "=".repeat(44);

function campo(etiquetaTexto, valor) {
  return ` ${etiquetaTexto.padEnd(13)}: ${valor}`;
}

function centrar(texto) {
  return " ".repeat(Math.max(Math.floor((LINEA.length - texto.length) / 2), 0)) + texto;
}

/** Mismo contenido y formato que tickets/ticket-<id>.txt del backend. */
/**
 * El código de acceso, grande y separado en dos grupos de cuatro para poder leerlo de
 * un renglón. No es un QR dibujado: generarlo de verdad pide una librería, y el código
 * en claro cumple la misma función —el acomodador lo escanea o lo tipea— sin sumar una
 * dependencia al proyecto. Cuando exista la app del escáner, el QR se arma con esto.
 */
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

export async function vistaTicket(contenedor, id) {
  const reserva = await api.obtenerReserva(id);
  const conAcreditacion = reserva.entradas.filter(
    (e) => e.tarifa && e.tarifa !== "GENERAL");

  contenedor.innerHTML = `
    <div class="rounded border border-emerald-300 bg-emerald-50 p-3 text-sm text-emerald-900 dark:border-emerald-800 dark:bg-emerald-950 dark:text-emerald-300">
      Reserva confirmada. Presentá este comprobante en boletería.
    </div>

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
    </div>
  `;

  contenedor.querySelector("#copiar").addEventListener("click", async () => {
    await navigator.clipboard.writeText(armarTicket(reserva));
    avisar("Comprobante copiado");
  });
}
