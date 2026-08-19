import { ir } from "../router.js";
import { etiqueta } from "../etiquetas.js";

/* ------------------------------------------------------------------- sesión */

const CLAVE_SESION = "cine.sesion";

export function sesionActual() {
  const guardada = sessionStorage.getItem(CLAVE_SESION);
  return guardada ? JSON.parse(guardada) : null;
}

export function abrirSesion(admin) {
  sessionStorage.setItem(CLAVE_SESION, JSON.stringify(admin));
}

export function cerrarSesion() {
  sessionStorage.removeItem(CLAVE_SESION);
  pintarEncabezado();
  ir("#/login");
}

/** El acomodador solo valida entradas: no tiene por qué ver el ABM de la cartelera. */
export function esAdministrador() {
  return sesionActual()?.rol !== "ACOMODADOR";
}

export function pintarEncabezado() {
  const sesion = sesionActual();
  document.getElementById("nav").classList.toggle("hidden", !sesion);
  document.getElementById("nav").classList.toggle("flex", !!sesion);
  document.getElementById("salir").classList.toggle("hidden", !sesion);
  document.getElementById("sesion").textContent = sesion ? sesion.nombre : "";
  document.getElementById("rol").textContent = sesion ? etiqueta(sesion.rol) : "Panel";
  document.querySelectorAll("#nav a[data-rol=ADMINISTRADOR]").forEach((enlace) => {
    enlace.classList.toggle("hidden", !!sesion && !esAdministrador());
  });
}
