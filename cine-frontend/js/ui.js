// Helpers de presentación: traducen los valores del dominio a algo legible
// y arman el andamiaje común de las vistas.

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
};

export function chipEstado(estado) {
  return chip(etiqueta(estado), COLOR_ESTADO[estado] || "");
}

/**
 * posterUrl es un String que puede venir vacío o con una URL que no carga: la inicial
 * del título queda siempre debajo y la imagen la tapa solo si llega bien.
 */
export function imagenPoster(pelicula, clases) {
  const inicial = escapar(pelicula.titulo.charAt(0).toUpperCase());
  const imagen = pelicula.posterUrl
    ? `<img src="${escapar(pelicula.posterUrl)}" alt="" loading="lazy"
        class="absolute inset-0 h-full w-full object-cover" onerror="this.remove()" />`
    : "";
  return `<div class="relative overflow-hidden bg-slate-300 dark:bg-slate-700 ${clases}">
    <span class="flex h-full w-full items-center justify-center text-2xl font-bold text-slate-500 dark:text-slate-400">${inicial}</span>
    ${imagen}
  </div>`;
}

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

export function escapar(texto) {
  return String(texto).replace(/[&<>"']/g, (c) => (
    { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]
  ));
}

export function chip(texto, clases = "bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-200") {
  return `<span class="inline-block rounded-full px-2 py-0.5 text-xs font-medium ${clases}">${escapar(texto)}</span>`;
}

/**
 * El círculo que gira. Vive acá y no en cada vista porque es el mismo en todas, y porque
 * es lo único que separa «está tardando» de «se colgó»: quieta, una espera de más de un
 * segundo se lee como una pantalla rota, y lo que hace el usuario es volver a apretar.
 */
export function spinner(clases = "h-5 w-5") {
  return `<span class="inline-block ${clases} animate-spin rounded-full border-2 border-slate-300 border-t-slate-700 dark:border-slate-600 dark:border-t-slate-200"></span>`;
}

export function cargando(mensaje = "Cargando…") {
  return `<div class="flex items-center justify-center gap-3 py-12 text-slate-500 dark:text-slate-400">
    ${spinner()}<span>${escapar(mensaje)}</span>
  </div>`;
}

/**
 * Un bloque gris que late, del tamaño de lo que se está por dibujar.
 *
 * Sirve donde se conoce la forma del resultado, y ahí es mejor que un cartel centrado por
 * dos motivos: dice **dónde** va a aparecer cada cosa —así la pantalla no salta cuando
 * llegan los datos— y deja ver de entrada que lo que viene son cuatro números y una tabla,
 * no una lista infinita.
 */
export function fantasma(clases) {
  return `<div class="animate-pulse rounded bg-slate-200 dark:bg-slate-700 ${clases}"></div>`;
}

export function error(mensaje) {
  return `<div class="rounded border border-red-300 bg-red-50 p-4 text-red-800 dark:border-red-800 dark:bg-red-950 dark:text-red-300">${escapar(mensaje)}</div>`;
}

/** Aviso efímero arriba de todo, para el resultado de una acción. */
export function avisar(mensaje, tipo = "ok") {
  const colores = tipo === "ok"
    ? "border-emerald-300 bg-emerald-50 text-emerald-900 dark:border-emerald-800 dark:bg-emerald-950 dark:text-emerald-300"
    : "border-red-300 bg-red-50 text-red-900 dark:border-red-800 dark:bg-red-950 dark:text-red-300";
  const caja = document.createElement("div");
  caja.className = `fixed top-4 left-1/2 z-50 -translate-x-1/2 rounded border px-4 py-2 shadow ${colores}`;
  caja.textContent = mensaje;
  document.body.appendChild(caja);
  setTimeout(() => caja.remove(), 3500);
}

/** El día de hoy en el formato en que viajan las fechas por la API. */
export function hoyISO() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** Los días como los nombra el backend, en el orden en que se muestran. */
export const DIAS_SEMANA = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

/**
 * Espera a que el usuario deje de escribir antes de ejecutar.
 *
 * Hace falta desde que los filtros los resuelve el backend: sin esto, "Matrix" son seis
 * pedidos y las respuestas pueden llegar desordenadas, así que la lista terminaría
 * mostrando el resultado de "Matri". Doscientos milisegundos es lo que separa una pausa
 * de seguir tecleando.
 */
export function conEspera(accion, ms = 200) {
  let pendiente;
  return (...args) => {
    clearTimeout(pendiente);
    pendiente = setTimeout(() => accion(...args), ms);
  };
}
