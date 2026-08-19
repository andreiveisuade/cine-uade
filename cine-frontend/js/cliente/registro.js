import * as api from "../api.js";
import { ir } from "../router.js";
import { boton, campo, panel } from "../componentes.js";
import { avisar, escapar } from "../dom.js";
import { clienteRecordado, olvidarCliente, recordarCliente } from "./compra.js";

/* ------------------------------------------------------------------- registro */

export async function vistaRegistro(contenedor) {
  const recordado = clienteRecordado();

  contenedor.innerHTML = `
    <div class="mx-auto max-w-md">
      <h1 class="mb-1 text-2xl font-bold">Registrarme</h1>
      <p class="mb-5 text-sm text-slate-500 dark:text-slate-400">
        No hace falta para comprar: es para no tener que cargar tus datos cada vez.
      </p>

      ${recordado ? panel(`
          Este navegador ya recuerda a <strong>${escapar(recordado.nombre)}</strong>
          (${escapar(recordado.email)}).
          <button type="button" id="olvidar" class="ml-1 text-slate-500 underline dark:text-slate-400">Olvidar</button>
        `, "mb-4 p-3 text-sm") : ""}

      ${panel(`
        <form id="registro" class="space-y-3">
          ${campo({ nombre: "nombre", etiqueta: "Nombre", requerido: true })}
          ${campo({ nombre: "email", etiqueta: "Email", tipo: "email", requerido: true })}
          ${boton("Registrarme")}
          <p id="errorRegistro" class="hidden text-sm text-red-700 dark:text-red-400"></p>
        </form>
      `, "p-4")}
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
