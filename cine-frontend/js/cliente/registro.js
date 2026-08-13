import * as api from "../api.js";
import { ir } from "../router.js";
import { avisar, escapar } from "../ui.js";
import { clienteRecordado, olvidarCliente, recordarCliente } from "./compra.js";

/* ------------------------------------------------------------------- registro */

export async function vistaRegistro(contenedor) {
  const recordado = clienteRecordado();

  contenedor.innerHTML = `
    <div class="mx-auto max-w-md">
      <h1 class="mb-1 text-2xl font-bold">Registrarme</h1>
      <p class="mb-5 text-sm text-slate-500">
        No hace falta para comprar: es para no tener que cargar tus datos cada vez.
      </p>

      ${recordado ? `
        <div class="mb-4 rounded border border-slate-300 bg-white p-3 text-sm">
          Este navegador ya recuerda a <strong>${escapar(recordado.nombre)}</strong>
          (${escapar(recordado.email)}).
          <button type="button" id="olvidar" class="ml-1 text-slate-500 underline">Olvidar</button>
        </div>` : ""}

      <form id="registro" class="space-y-3 rounded border border-slate-300 bg-white p-4">
        <label class="block text-sm">
          <span class="text-slate-600">Nombre</span>
          <input name="nombre" required class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
        </label>
        <label class="block text-sm">
          <span class="text-slate-600">Email</span>
          <input name="email" type="email" required
            class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
        </label>
        <button type="submit"
          class="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white">
          Registrarme
        </button>
        <p id="errorRegistro" class="hidden text-sm text-red-700"></p>
      </form>
    </div>
  `;

  const formulario = contenedor.querySelector("#registro");
  const errorRegistro = contenedor.querySelector("#errorRegistro");

  contenedor.querySelector("#olvidar")?.addEventListener("click", () => {
    olvidarCliente();
    vistaRegistro(contenedor);
  });

  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const datos = new FormData(formulario);
    try {
      const cliente = await api.registrarCliente({
        nombre: datos.get("nombre"),
        email: datos.get("email"),
      });
      recordarCliente(cliente);
      avisar(`Listo, ${cliente.nombre}`);
      ir("#/");
    } catch (e) {
      errorRegistro.textContent = e.message;
      errorRegistro.classList.remove("hidden");
    }
  });
}
