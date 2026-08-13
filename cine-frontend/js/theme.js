// Tema claro/oscuro. El <head> de cada página ya aplicó la clase "dark" antes del primer
// paint (ver script inline) para evitar el flash; acá solo vive el toggle y su persistencia.

const CLAVE = "cine-tema";

function temaActual() {
  return document.documentElement.classList.contains("dark") ? "dark" : "light";
}

function aplicar(tema) {
  document.documentElement.classList.toggle("dark", tema === "dark");
  localStorage.setItem(CLAVE, tema);
}

function actualizarIcono(boton) {
  boton.textContent = temaActual() === "dark" ? "☀️" : "🌙";
  boton.setAttribute("aria-label", temaActual() === "dark" ? "Cambiar a modo claro" : "Cambiar a modo oscuro");
}

/** Engancha el botón #theme-toggle de la página con el cambio de tema. */
export function wireToggle() {
  const boton = document.getElementById("theme-toggle");
  if (!boton) return;
  actualizarIcono(boton);
  boton.addEventListener("click", () => {
    aplicar(temaActual() === "dark" ? "light" : "dark");
    actualizarIcono(boton);
  });
}
