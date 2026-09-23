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
  POCHOCLOS: "Pochoclos",
  BEBIDA: "Bebida",
  GOLOSINA: "Golosina",
  COMBO: "Combo",
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

export function chipClasificacion(clasificacion) {
  return chip(etiqueta(clasificacion), COLOR_CLASIFICACION[clasificacion] || "");
}

const COLOR_ESTADO = {
  RESERVADA: "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300",
  PAGADA: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300",
  CANCELADA: "bg-slate-200 text-slate-600 dark:bg-slate-700 dark:text-slate-300",
  EXPIRADA: "bg-slate-200 text-slate-500 dark:bg-slate-700 dark:text-slate-400",
  EN_CURSO: "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300",
  TERMINADA: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300",
  FALLIDA: "bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300",
};

export function chipEstado(estado) {
  return chip(etiqueta(estado), COLOR_ESTADO[estado] || "");
}

export const DIAS_SEMANA = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

export const TIPOS_PRODUCTO_SUELTO = ["POCHOCLOS", "BEBIDA", "GOLOSINA"];
