import { ErrorApi } from "./ErrorApi.js";
import { mensajeDeError } from "./mensajeDeError.js";

const BASE = "/api";

// Patrón Facade: las Api de cada subdominio piden por verbo y ruta; fetch, JSON y errores quedan acá.
async function pedir(metodo, ruta, cuerpo) {
  const respuesta = await enviar(metodo, ruta, cuerpo);
  if (respuesta.status === 204) return null;
  const texto = await respuesta.text();
  const datos = leerJson(texto, respuesta.ok);
  if (!respuesta.ok) throw new ErrorApi(mensajeDeError(respuesta.status, datos, texto), respuesta.status);
  return datos;
}

async function enviar(metodo, ruta, cuerpo) {
  try {
    // Sin `credentials`: el default (same-origin) manda la cookie de la sesión del cliente.
    return await fetch(BASE + ruta, {
      headers: cuerpo ? { "Content-Type": "application/json" } : {},
      method: metodo,
      body: cuerpo ? JSON.stringify(cuerpo) : undefined,
    });
  } catch {
    throw new Error("No se pudo conectar con el servidor");
  }
}

// Un error sin JSON lo resuelve mensajeDeError; un 2xx que no se puede leer no tiene con qué seguir.
function leerJson(texto, ok) {
  if (!texto) return null;
  try {
    return JSON.parse(texto);
  } catch {
    if (ok) throw new Error("El servidor devolvió una respuesta que no se pudo leer");
    return null;
  }
}

export const get = (ruta) => pedir("GET", ruta);
export const post = (ruta, cuerpo) => pedir("POST", ruta, cuerpo);
export const put = (ruta, cuerpo) => pedir("PUT", ruta, cuerpo);
export const borrar = (ruta) => pedir("DELETE", ruta);

// Página entera y no fetch: lo que sigue lo decide el backend con un redirect (Google, el retorno del pago).
export const navegar = (ruta) => window.location.assign(BASE + ruta);

// Avisar sin esperar respuesta: el redirect no se sigue y una falla de red no importa.
export const avisar = (ruta) => fetch(BASE + ruta, { redirect: "manual" }).then(() => {}, () => {});
