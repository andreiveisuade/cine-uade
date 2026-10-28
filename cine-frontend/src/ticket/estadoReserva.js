import { hora } from "@/compartido/formato.js";

// Presentación del estado que manda el backend; el front no decide vencimientos: la hora límite viene en venceEn.
export const COLOR_ESTADO = { RESERVADA: "ambar", PAGADA: "green", EXPIRADA: "gray", CANCELADA: "gray" };

// Qué le decimos al comprador según el estado, en la cabecera de la entrada.
const AVISO_POR_ESTADO = {
  RESERVADA: (r) => `Reserva confirmada. Pagala online o en boletería antes de las ${hora(r.venceEn)}: después se liberan las butacas.`,
  PAGADA: () => "Entrada paga. Mostrá el código en la puerta.",
  EXPIRADA: () => "Esta reserva venció. Las butacas ya se liberaron.",
  CANCELADA: () => "Esta reserva está cancelada.",
};

export const avisoDe = (reserva) => AVISO_POR_ESTADO[reserva.estado]?.(reserva);

// Lo que el talón dice del código: la pantalla y el PDF lo cuentan igual.
export const CODIGO_POR_ESTADO = {
  RESERVADA: "Todavía no está paga: el QR aparece cuando se pague.",
  PAGADA: "Mostralo en la puerta. Sirve una sola vez.",
  EXPIRADA: "Ya no sirve: la reserva venció.",
  CANCELADA: "Ya no sirve: la reserva se canceló.",
};

export const vigente = (reserva) => ["RESERVADA", "PAGADA"].includes(reserva.estado) && !reserva.ingresadaEn;

// Lo que sirve en la puerta (R18): QR, PDF y «Mostrar en la puerta» solo con la entrada paga y sin usar.
export const entradaValida = (reserva) => reserva.estado === "PAGADA" && !reserva.ingresadaEn;

export const conAcreditacion = (entrada) => entrada.requiereAcreditacion;

// El candy de la entrada: lo pedido sin pagar no se entrega, y lo cobrado se retira una vez con el QR.
export const estadoDelCandy = (compra) => {
  if (compra.estado === "PENDIENTE") return { texto: "sin pagar", color: "gray" };
  return compra.retiradaEn ? { texto: "retirado", color: "gray" } : { texto: "a retirar", color: "ambar" };
};

// En dos bloques de cuatro se dicta y se lee mejor en la puerta.
export const codigoLegible = (codigo) => `${codigo.slice(0, 4)} ${codigo.slice(4)}`;
