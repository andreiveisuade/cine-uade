import * as api from "../api.js";
import { ir } from "../router.js";
import { boton, campo, panel } from "../componentes.js";
import { abrirSesion, esAdministrador, pintarEncabezado } from "./sesion.js";

/* -------------------------------------------------------------------- login */

export async function vistaLogin(contenedor) {
  contenedor.innerHTML = panel(`
      <h1 class="mb-4 text-xl font-bold">Ingresar</h1>
      <form id="login" class="space-y-3">
        ${campo({ nombre: "email", etiqueta: "Email", tipo: "email", requerido: true, extra: 'autocomplete="username"' })}
        ${campo({ nombre: "password", etiqueta: "Contraseña", tipo: "password", requerido: true, extra: 'autocomplete="current-password"' })}
        ${boton("Entrar")}
        <p id="errorLogin" class="hidden text-sm text-red-700 dark:text-red-400"></p>
      </form>
      <p class="mt-4 border-t border-slate-200 pt-3 text-xs text-slate-500 dark:border-slate-800 dark:text-slate-400">
        Datos de prueba: <code>encargado@cine.uade.ar</code> / <code>cine2026</code>
      </p>
    `, "mx-auto max-w-sm p-5");

  const formulario = contenedor.querySelector("#login");
  const errorLogin = contenedor.querySelector("#errorLogin");
  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const datos = new FormData(formulario);
    try {
      abrirSesion(await api.login(datos.get("email"), datos.get("password")));
      pintarEncabezado();
      // El acomodador entra directo a la puerta: es lo único que puede hacer.
      ir(esAdministrador() ? "#/peliculas" : "#/puerta");
    } catch (e) {
      errorLogin.textContent = e.message;
      errorLogin.classList.remove("hidden");
    }
  });
}
