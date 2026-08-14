// Lo que toca el DOM de verdad o el string que va a parar ahí: escapar, avisar por
// pantalla y esperar a que alguien termine de escribir. Nada de esto sabe de películas,
// salas ni reservas — es infraestructura de cualquier pantalla.

export function escapar(texto) {
  return String(texto).replace(/[&<>"']/g, (c) => (
    { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]
  ));
}

/**
 * Tag template que escapa cada valor interpolado por default. El escapado manual (115
 * llamados a `escapar()` repartidos por las vistas) ya se olvidó dos veces en pantallas
 * que muestran texto importado de TMDB: acá alcanza con escribir `html\`...${valor}...\``
 * y no hay nada que acordarse. No hace falta migrar lo que ya funciona — esto es para
 * código nuevo, sobre todo donde entra texto que no escribió el encargado.
 */
export function html(fragmentos, ...valores) {
  return fragmentos.reduce((resultado, fragmento, i) =>
    resultado + fragmento + (i < valores.length ? escapar(valores[i]) : ""), "");
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
