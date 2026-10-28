import { etiqueta } from "@/api/etiquetas.js";
import { fechaHora, precioExacto } from "@/compartido/formato.js";
import { conAcreditacion } from "@/ticket/estadoReserva.js";

const LINEA = "=".repeat(44);

const renglon = (etiquetaTexto, valor) => ` ${etiquetaTexto.padEnd(13)}: ${valor}`;

const centrar = (texto) => " ".repeat(Math.max(Math.floor((LINEA.length - texto.length) / 2), 0)) + texto;

/** Mismo contenido que tickets/ticket-<id>.txt del backend, con las constantes traducidas a su etiqueta. */
export function armarComprobante(reserva) {
  return [
    LINEA,
    centrar("CINE UADE"),
    centrar("TICKET #" + reserva.id),
    LINEA,
    renglon("Película", reserva.pelicula.titulo),
    renglon("Sala", `${reserva.sala.nombre} (${etiqueta(reserva.sala.tipo)})`),
    renglon("Función", fechaHora(reserva.funcion.inicio)),
    renglon("Formato", `${etiqueta(reserva.funcion.proyeccion)} ${etiqueta(reserva.funcion.idioma).toLowerCase()}`),
    renglon("Cliente", reserva.cliente.nombre),
    LINEA,
    ...reserva.entradas.map((e) => renglon(
      "Butaca " + e.codigo,
      precioExacto(e.precio) + (conAcreditacion(e) ? "  " + etiqueta(e.tarifa) : ""))),
    LINEA,
    renglon("Entradas", String(reserva.entradas.length)),
    renglon("Total", precioExacto(reserva.total)),
    renglon("Estado", etiqueta(reserva.estado)),
    LINEA,
    centrar("CÓDIGO DE ACCESO"),
    centrar(reserva.codigo || ""),
    LINEA,
    centrar("Presentar en boletería"),
    LINEA,
  ].join("\n");
}
