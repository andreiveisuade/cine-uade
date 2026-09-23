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

// Nombres de la paleta de Mantine: Chips.jsx resuelve el tono para claro y oscuro.
export const COLOR_CLASIFICACION = {
  ATP: "green",
  MAS_13: "yellow",
  MAS_16: "orange",
  MAS_18: "red",
};

export const COLOR_ESTADO = {
  RESERVADA: "yellow",
  PAGADA: "green",
  CANCELADA: "gray",
  EXPIRADA: "gray",
  EN_CURSO: "yellow",
  TERMINADA: "green",
  FALLIDA: "red",
};

export const DIAS_SEMANA = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

export const TIPOS_PRODUCTO_SUELTO = ["POCHOCLOS", "BEBIDA", "GOLOSINA"];
