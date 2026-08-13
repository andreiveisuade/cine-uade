// Dibujo de la sala fila por fila. La geometría es la misma para el cliente y para
// el encargado; lo que cambia es qué significa cada color, así que el pintado se pasa
// por parámetro.

export const SIMBOLO = { VIP: "*", PAREJA: "&", ACCESIBLE: "+", ESTANDAR: "" };

export const CLASES_TIPO = {
  VIP: "border-amber-500 bg-amber-50 text-amber-900 dark:border-amber-700 dark:bg-amber-950 dark:text-amber-300",
  PAREJA: "border-pink-500 bg-pink-50 text-pink-900 dark:border-pink-700 dark:bg-pink-950 dark:text-pink-300",
  ACCESIBLE: "border-sky-500 bg-sky-50 text-sky-900 dark:border-sky-700 dark:bg-sky-950 dark:text-sky-300",
  ESTANDAR: "border-slate-400 bg-white text-slate-700 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200",
};

/**
 * Una sala no es un rectángulo: cada fila tiene la cantidad de butacas que dice
 * butacasPorFila, y las filas se centran entre sí.
 *
 * @param pintar (asiento) => { clases, deshabilitado, titulo }
 */
export function dibujarMapa(sala, asientos, pintar) {
  const filas = [];
  for (let fila = 1; fila <= sala.filas; fila++) {
    const deLaFila = asientos.filter((a) => a.fila === fila);
    const letra = deLaFila[0]?.codigo.charAt(0) || "";
    const butacas = deLaFila.map((a) => {
      const { clases, deshabilitado, titulo } = pintar(a);
      const ancho = a.tipo === "PAREJA" ? "w-12" : "w-7";
      return `
        <button type="button" data-codigo="${a.codigo}" ${deshabilitado ? "disabled" : ""}
          title="${titulo}"
          class="h-7 ${ancho} shrink-0 rounded text-[10px] font-medium ${clases}">
          ${a.numero}${SIMBOLO[a.tipo]}
        </button>`;
    }).join("");
    filas.push(`
      <div class="flex items-center justify-center gap-1">
        <span class="w-4 shrink-0 text-right text-xs font-semibold text-slate-500 dark:text-slate-400">${letra}</span>
        ${butacas}
      </div>`);
  }
  return filas.join("");
}

export function pantalla() {
  return `<div class="mb-4 rounded bg-slate-800 py-1 text-center text-xs tracking-[0.3em] text-white dark:bg-slate-700">
    PANTALLA
  </div>`;
}

export function referencia(items) {
  return `<div class="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-600 dark:text-slate-400">
    ${items.map(([clases, texto]) =>
      `<span><span class="mr-1 inline-block h-3 w-3 rounded ${clases} align-middle"></span>${texto}</span>`,
    ).join("")}
  </div>`;
}
