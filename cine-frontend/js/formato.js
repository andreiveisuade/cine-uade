// Formateo de datos del dominio a texto: plata, tiempo y fecha. Ninguna de estas
// funciones toca el DOM ni arma HTML — son texto, para poner adentro de lo que sea.

export function precio(monto) {
  return "$ " + Math.round(monto).toLocaleString("es-AR");
}

/** Con dos decimales, como el comprobante .txt del backend. */
export function precioExacto(monto) {
  return "$ " + monto.toFixed(2);
}

export function duracion(minutos) {
  const horas = Math.floor(minutos / 60);
  const resto = minutos % 60;
  return horas > 0 ? `${horas}h ${resto}m` : `${resto}m`;
}

const DIAS = ["domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado"];
const MESES = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"];

function aFecha(iso) {
  return new Date(iso);
}

export function hora(iso) {
  const d = aFecha(iso);
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

export function dia(iso) {
  const d = aFecha(iso);
  const hoy = new Date();
  hoy.setHours(0, 0, 0, 0);
  const diferencia = Math.round((new Date(d).setHours(0, 0, 0, 0) - hoy) / 86400000);
  if (diferencia === 0) return "Hoy";
  if (diferencia === 1) return "Mañana";
  return `${DIAS[d.getDay()]} ${d.getDate()} ${MESES[d.getMonth()]}`;
}

export function fechaHora(iso) {
  const d = aFecha(iso);
  const pad = (n) => String(n).padStart(2, "0");
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${hora(iso)}`;
}

/** Agrupa funciones por día calendario, en orden. */
export function porDia(funciones) {
  const grupos = new Map();
  for (const funcion of funciones) {
    const clave = funcion.inicio.slice(0, 10);
    if (!grupos.has(clave)) grupos.set(clave, []);
    grupos.get(clave).push(funcion);
  }
  return [...grupos.entries()];
}

/** El día de hoy en el formato en que viajan las fechas por la API. */
export function hoyISO() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}
