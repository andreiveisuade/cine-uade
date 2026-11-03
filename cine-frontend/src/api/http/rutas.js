// Arma las rutas de la API: cada valor que se interpola va codificado, así un código con «/» no cambia la ruta.
export const ruta = (partes, ...valores) =>
  partes.reduce((armada, parte, i) => armada + encodeURIComponent(valores[i - 1]) + parte);

// La query sin los parámetros que no vienen: obtenerCartelera() y obtenerCartelera("DRAMA") comparten ruta.
export function consulta(parametros) {
  const pares = Object.entries(parametros).filter(([, valor]) => valor);
  return pares.length ? "?" + pares.map(([clave, valor]) => `${clave}=${encodeURIComponent(valor)}`).join("&") : "";
}
