// Traducción de los enums del dominio a lo que lee una persona. El backend manda
// MAS_16, esto devuelve "+16".

import { chip } from "./componentes.js";

const ETIQUETAS = {
  ACCION: "Acción",
  COMEDIA: "Comedia",
  DRAMA: "Drama",
  TERROR: "Terror",
  CIENCIA_FICCION: "Ciencia ficción",
  ANIMACION: "Animación",
  DOCUMENTAL: "Documental",
  ROMANCE: "Romance",
  SUSPENSO: "Suspenso",
  DOS_D: "2D",
  TRES_D: "3D",
  IMAX: "IMAX",
  CUATRO_D: "4D",
  VIP: "VIP",
  ESTANDAR: "Estándar",
  PAREJA: "Pareja",
  ACCESIBLE: "Accesible",
  DOBLADA: "Doblada",
  SUBTITULADA: "Subtitulada",
  HABILITADO: "Habilitada",
  FUERA_DE_SERVICIO: "Fuera de servicio",
  RESERVADA: "Reservada",
  PAGADA: "Pagada",
  CANCELADA: "Cancelada",
  EXPIRADA: "Vencida",
  GENERAL: "General",
  MENOR: "Menor",
  JUBILADO: "Jubilado",
  ESTUDIANTE: "Estudiante",
  PORCENTAJE: "Porcentaje",
  MONTO_FIJO: "Monto fijo",
  NXM: "NxM",
  ADMINISTRADOR: "Administrador",
  ACOMODADOR: "Acomodador",
  MONDAY: "Lunes",
  TUESDAY: "Martes",
  WEDNESDAY: "Miércoles",
  THURSDAY: "Jueves",
  FRIDAY: "Viernes",
  SATURDAY: "Sábado",
  SUNDAY: "Domingo",
  ATP: "ATP",
  MAS_13: "+13",
  MAS_16: "+16",
  MAS_18: "+18",
  EN_CURSO: "En curso",
  TERMINADA: "Terminada",
  FALLIDA: "Falló",
};

export function etiqueta(valor) {
  return ETIQUETAS[valor] || valor;
}

const COLOR_CLASIFICACION = {
  ATP: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300",
  MAS_13: "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300",
  MAS_16: "bg-orange-100 text-orange-800 dark:bg-orange-900/40 dark:text-orange-300",
  MAS_18: "bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300",
};

/** La clasificación se lee de un vistazo: verde ATP, rojo +18. */
export function chipClasificacion(clasificacion) {
  return chip(etiqueta(clasificacion), COLOR_CLASIFICACION[clasificacion] || "");
}

const COLOR_ESTADO = {
  RESERVADA: "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300",
  PAGADA: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300",
  CANCELADA: "bg-slate-200 text-slate-600 dark:bg-slate-700 dark:text-slate-300",
  // Vencida no es cancelada: al cliente no se le puede decir que canceló algo que no canceló.
  EXPIRADA: "bg-slate-200 text-slate-500 dark:bg-slate-700 dark:text-slate-400",
  // Los de una corrida del importador. Comparten el chip con los de una reserva porque
  // son lo mismo para quien mira: en qué terminó algo que estaba pasando.
  EN_CURSO: "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300",
  TERMINADA: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300",
  FALLIDA: "bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300",
};

export function chipEstado(estado) {
  return chip(etiqueta(estado), COLOR_ESTADO[estado] || "");
}

/** Los días como los nombra el backend, en el orden en que se muestran. */
export const DIAS_SEMANA = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
