import { borrar, get, post } from "./http/clienteHttp.js";

// Sin credenciales de Google Cloud en el servidor el botón no tiene a dónde ir: la pantalla lo avisa.
export const ingresoConGoogle = () => get("/sesion/google");

// Página entera y fuera de /api: Google vuelve al backend, que abre la sesión (cookie HttpOnly) y redirige a #/ingresar.
export const irAGoogle = () => window.location.assign("/oauth2/authorization/google");

// El backend contesta con quien tenga la cookie, o 401 si no hay sesión.
export const sesionActual = () => post("/sesion");

export const salir = () => borrar("/sesion");
