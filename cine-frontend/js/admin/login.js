import * as api from "../api.js";
import { ir } from "../router.js";
import { abrirSesion, esAdministrador, pintarEncabezado } from "./sesion.js";

/* -------------------------------------------------------------------- login */

export async function vistaLogin(contenedor) {
  contenedor.innerHTML = `
    <div class="mx-auto max-w-sm rounded border border-slate-300 bg-white p-5 dark:border-slate-700 dark:bg-slate-900">
      <h1 class="mb-4 text-xl font-bold">Ingresar</h1>
      <form id="login" class="space-y-3">
        <label class="block text-sm">
          <span class="text-slate-600 dark:text-slate-300">Email</span>
          <input name="email" type="email" required autocomplete="username"
            class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
        </label>
        <label class="block text-sm">
          <span class="text-slate-600 dark:text-slate-300">Contraseña</span>
          <input name="password" type="password" required autocomplete="current-password"
            class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
        </label>
        <button type="submit"
          class="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white dark:bg-white dark:text-slate-900">Entrar</button>
        <p id="errorLogin" class="hidden text-sm text-red-700 dark:text-red-400"></p>
      </form>
      <p class="mt-4 border-t border-slate-200 pt-3 text-xs text-slate-500 dark:border-slate-800 dark:text-slate-400">
        Datos de prueba: <code>encargado@cine.uade.ar</code> / <code>cine2026</code>
      </p>
    </div>
  `;

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
