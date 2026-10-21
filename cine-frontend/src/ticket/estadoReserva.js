// Presentación del estado que manda el backend; el front no decide vencimientos.
export const COLOR_ESTADO = { RESERVADA: "ambar", PAGADA: "green", EXPIRADA: "gray", CANCELADA: "gray" };

export const vigente = (reserva) => ["RESERVADA", "PAGADA"].includes(reserva.estado) && !reserva.ingresadaEn;

export const conAcreditacion = (entrada) => entrada.tarifa && entrada.tarifa !== "GENERAL";

// En dos bloques de cuatro se dicta y se lee mejor en la puerta.
export const codigoLegible = (codigo) => `${codigo.slice(0, 4)} ${codigo.slice(4)}`;
