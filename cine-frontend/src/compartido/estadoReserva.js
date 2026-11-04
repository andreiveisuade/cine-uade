import { etiqueta } from "@/api/etiquetas.js";
import { cantidad } from "@/compartido/formato.js";

// Presentación del estado que manda el backend; el front no decide vencimientos: lo que queda viene en segundosParaPagar.
export const COLOR_ESTADO = { RESERVADA: "ambar", PAGADA: "green", EXPIRADA: "gray", CANCELADA: "gray", NO_USADA: "gray" };

// El backend no tiene estado de usada: es una PAGADA con ingresadaEn, y para el comprador ya no está vigente.
export const estadoVisible = (reserva) => (reserva.ingresadaEn
  ? { texto: "Usada", color: "gray" }
  : { texto: etiqueta(reserva.estado), color: COLOR_ESTADO[reserva.estado] });

// Qué le decimos al comprador según el estado, en la cabecera de la entrada.
const AVISO_POR_ESTADO = {
  // Sin hora: venceEn no contempla que la función puede empezar antes (R19). El tiempo lo cuenta TiempoParaPagar.
  // RESERVADA sin cobrable es la función empezada: la vencida ya llega EXPIRADA.
  RESERVADA: (r) => (r.cobrable
    ? "Reserva confirmada. Pagala online o en boletería: cuando se termine el tiempo se liberan las butacas."
    : "La función ya empezó: esta reserva ya no se puede pagar."),
  PAGADA: (r) => (r.ingresadaEn ? "Esta entrada ya se usó en la puerta." : "Entrada paga. Mostrá el código en la puerta."),
  EXPIRADA: () => "Esta reserva venció. Las butacas ya se liberaron.",
  CANCELADA: () => "Esta reserva está cancelada.",
  NO_USADA: () => "La función ya terminó: esta entrada venció sin usarse.",
};

export const avisoDe = (reserva) => AVISO_POR_ESTADO[reserva.estado]?.(reserva);

// Lo que el talón dice del código: la pantalla y el PDF lo cuentan igual.
export const CODIGO_POR_ESTADO = {
  RESERVADA: "Todavía no está paga: el QR aparece cuando se pague.",
  PAGADA: "Mostralo en la puerta. Sirve una sola vez.",
  EXPIRADA: "Ya no sirve: la reserva venció.",
  CANCELADA: "Ya no sirve: la reserva se canceló.",
  NO_USADA: "La entrada ya no sirve: la función terminó.",
};

export const notaDelCodigo = (reserva) => (reserva.estado === "RESERVADA" && !reserva.cobrable
  ? "Ya no sirve: la función empezó sin que se pague."
  : CODIGO_POR_ESTADO[reserva.estado]);

// Las que ya no sirven: cancelada, vencida y (R32) la paga que nadie usó con la función terminada.
export const cerrada = (reserva) => ["EXPIRADA", "CANCELADA", "NO_USADA"].includes(reserva.estado);

// Paga aunque la entrada ya no sirva: lo que se cobró sigue cobrado.
const paga = (reserva) => ["PAGADA", "NO_USADA"].includes(reserva.estado);

export const vigente = (reserva) => ["RESERVADA", "PAGADA"].includes(reserva.estado) && !reserva.ingresadaEn;

// Lo que sirve en la puerta (R18): QR, PDF y «Mostrar en la puerta» solo con la entrada paga y sin usar.
export const entradaValida = (reserva) => reserva.estado === "PAGADA" && !reserva.ingresadaEn;

export const conAcreditacion = (entrada) => entrada.requiereAcreditacion;

// El candy de la entrada: lo pedido sin pagar no se entrega, y lo cobrado se retira una vez con el QR.
export const estadoDelCandy = (compra) => {
  if (compra.estado === "PENDIENTE") return { texto: "sin pagar", color: "gray" };
  return compra.retiradaEn ? { texto: "retirado", color: "gray" } : { texto: "a retirar", color: "ambar" };
};

// Cada producto con el estado de su compra: la entrada en pantalla y la del PDF lo listan igual.
export const renglonesDelCandy = (reserva) => (reserva.candy ?? []).flatMap((c) => c.items.map((i) =>
  ({ ...i, clave: `${c.id}-${i.productoId}`, estado: estadoDelCandy(c), cobrado: c.estado !== "PENDIENTE" })));

// El pedido de candy que se paga con la reserva (R30): hay uno solo por vez.
export const pedidoPendiente = (reserva) => (reserva.candy ?? []).find((c) => c.estado === "PENDIENTE");

export const comprasCobradas = (reserva) => (reserva.candy ?? []).filter((c) => c.estado === "COBRADA");

// El candy no vence (R32): con la entrada usada o vencida sin usar, el QR sigue sirviendo para retirar lo cobrado.
export const qrSoloParaElCandy = (reserva) => !entradaValida(reserva) && comprasCobradas(reserva).some((c) => !c.retiradaEn);

// Ya paga manda el Pago que guardó el backend: `total` es de lista y la promo no se recalcula acá.
export const importeDeLasEntradas = (reserva) => reserva.pago?.monto ?? reserva.total;
export const descuentoDeLaEntrada = (reserva) => reserva.pago?.descuento ?? 0;

// Antes de pagar suma el candy pendiente, que se cobra con la reserva; ya paga, solo lo cobrado: lo pendiente se
// paga aparte y figura «sin pagar». La pantalla y el PDF lo cuentan igual.
export function totalDeLaEntrada(reserva) {
  const candy = renglonesDelCandy(reserva).filter((r) => r.cobrado || !paga(reserva));
  return {
    texto: `Total · ${cantidad(reserva.entradas.length, "entrada")}${candy.length ? " y candy" : ""}`,
    importe: importeDeLasEntradas(reserva) + candy.reduce((suma, r) => suma + r.subtotal, 0),
  };
}

// En dos bloques de cuatro se dicta y se lee mejor en la puerta.
export const codigoLegible = (codigo) => `${codigo.slice(0, 4)} ${codigo.slice(4)}`;
