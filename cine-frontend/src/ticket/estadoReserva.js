// Presentación del estado que manda el backend; el front no decide vencimientos.
export const COLOR_ESTADO = { RESERVADA: "ambar", PAGADA: "green", EXPIRADA: "gray", CANCELADA: "gray" };

// Qué le decimos al comprador según el estado, en la cabecera de la entrada.
export const AVISO_POR_ESTADO = {
  RESERVADA: "Reserva confirmada. Pagala online o en boletería antes de que venza.",
  PAGADA: "Entrada paga. Mostrá el código en la puerta.",
  EXPIRADA: "Esta reserva venció. Las butacas ya se liberaron.",
  CANCELADA: "Esta reserva está cancelada.",
};

// Lo que el talón dice del código: la pantalla y el PDF lo cuentan igual.
export const CODIGO_POR_ESTADO = {
  RESERVADA: "Todavía no está paga. Sirve una sola vez cuando se pague.",
  PAGADA: "Mostralo en la puerta. Sirve una sola vez.",
  EXPIRADA: "Ya no sirve: la reserva venció.",
  CANCELADA: "Ya no sirve: la reserva se canceló.",
};

export const vigente = (reserva) => ["RESERVADA", "PAGADA"].includes(reserva.estado) && !reserva.ingresadaEn;

export const conAcreditacion = (entrada) => entrada.requiereAcreditacion;

// En dos bloques de cuatro se dicta y se lee mejor en la puerta.
export const codigoLegible = (codigo) => `${codigo.slice(0, 4)} ${codigo.slice(4)}`;
