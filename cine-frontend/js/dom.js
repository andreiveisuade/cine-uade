export function escapar(texto) {
  return String(texto).replace(/[&<>"']/g, (c) => (
    { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]
  ));
}

// Escapa cada valor interpolado por default.
export function html(fragmentos, ...valores) {
  return fragmentos.reduce((resultado, fragmento, i) =>
    resultado + fragmento + (i < valores.length ? escapar(valores[i]) : ""), "");
}

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

export function conEspera(accion, ms = 200) {
  let pendiente;
  return (...args) => {
    clearTimeout(pendiente);
    pendiente = setTimeout(() => accion(...args), ms);
  };
}
