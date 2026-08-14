import * as api from "../api.js";
import { ir } from "../router.js";
import { avisar, dia, escapar, etiqueta, hora, precio, error } from "../ui.js";
import { seleccion, catalogoTarifas, tarifaPorNombre, precioConTarifa, selectorTarifa,
         clienteRecordado, recordarCliente, sesionDeCompra, renovarMientrasSigaAca } from "./compra.js";

/* ------------------------------------------------------------- confirmación */

export async function vistaConfirmar(contenedor, id) {
  const [funcion] = await Promise.all([
    api.obtenerFuncion(id, sesionDeCompra()), catalogoTarifas()]);
  // Si se recargó la página la selección se perdió: volver al mapa.
  if (seleccion.funcionId !== funcion.id || Object.keys(seleccion.butacas).length === 0) {
    ir(`#/funcion/${funcion.id}`);
    return;
  }

  const codigos = Object.keys(seleccion.butacas);
  const elegidas = funcion.asientos.filter((a) => codigos.includes(a.codigo));
  const recordado = clienteRecordado();

  const totalDe = () => elegidas.reduce(
    (suma, a) => suma + precioConTarifa(a, seleccion.butacas[a.codigo]), 0);
  const aAcreditar = () => elegidas.filter(
    (a) => tarifaPorNombre(seleccion.butacas[a.codigo]).requiereAcreditacion);

  // El resumen se repinta solo cuando cambia una tarifa, sin tocar el formulario.
  const resumenCompra = () => `
  <section id="resumenCompra" class="rounded border border-slate-300 bg-white p-4 dark:border-slate-700 dark:bg-slate-900">
    <h2 class="mb-1 font-semibold">${escapar(funcion.pelicula.titulo)}</h2>
    <p class="mb-3 text-sm text-slate-600 dark:text-slate-300">
      ${escapar(dia(funcion.inicio))} ${hora(funcion.inicio)} ·
      ${escapar(funcion.sala.nombre)} (${etiqueta(funcion.sala.tipo)}) ·
      ${etiqueta(funcion.proyeccion)} · ${etiqueta(funcion.idioma)}
    </p>
    <table class="w-full text-sm">
      <thead>
        <tr class="border-b border-slate-300 text-left text-xs uppercase text-slate-500 dark:border-slate-700 dark:text-slate-400">
          <th class="py-1">Butaca</th><th>Tipo</th><th>Tarifa</th><th class="text-right">Precio</th>
        </tr>
      </thead>
      <tbody>
        ${elegidas.map((a) => `
          <tr class="border-b border-slate-200 dark:border-slate-800">
            <td class="py-1 font-medium">${a.codigo}</td>
            <td>${etiqueta(a.tipo)}</td>
            <td>${selectorTarifa(a.codigo, seleccion.butacas[a.codigo])}</td>
            <td class="text-right">${precio(precioConTarifa(a, seleccion.butacas[a.codigo]))}</td>
          </tr>`).join("")}
      </tbody>
      <tfoot>
        <tr class="font-semibold">
          <td class="py-2" colspan="3">Total</td>
          <td class="py-2 text-right">${precio(totalDe())}</td>
        </tr>
      </tfoot>
    </table>
    <p class="mt-2 text-xs text-slate-500 dark:text-slate-400">
      Precio base ${precio(funcion.precio)} × sala ${etiqueta(funcion.sala.tipo)} ×
      tipo de butaca × tarifa.
    </p>
    <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">
      Si hay promociones vigentes, el descuento se aplica al pagar: depende del medio
      de pago, así que el total definitivo aparece recién ahí.
    </p>
    ${aAcreditar().length ? `
      <p class="mt-3 rounded bg-amber-50 p-2 text-xs text-amber-900 dark:bg-amber-950 dark:text-amber-300">
        <strong>Acordate del carnet.</strong> En la puerta te van a pedir que acredites
        la tarifa de ${escapar(aAcreditar().map((a) => `${a.codigo} (${etiqueta(seleccion.butacas[a.codigo]).toLowerCase()})`).join(", "))}.
      </p>` : ""}
  </section>
  `;

  contenedor.innerHTML = `
    <a href="#/funcion/${funcion.id}" class="text-sm text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-100">&larr; Cambiar butacas</a>
    <h1 class="mt-2 mb-5 text-2xl font-bold">Confirmar reserva</h1>

    <div class="grid gap-4 md:grid-cols-2">
      <section class="rounded border border-slate-300 bg-white p-4 dark:border-slate-700 dark:bg-slate-900">
        <h2 class="mb-3 font-semibold">Tus datos</h2>
        <form id="datos" class="space-y-3">
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Nombre</span>
            <input name="nombre" required value="${escapar(recordado?.nombre || "")}"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100" />
          </label>
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Email</span>
            <input name="email" type="email" required value="${escapar(recordado?.email || "")}"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100" />
          </label>
          <button type="submit"
            class="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white dark:bg-white dark:text-slate-900">
            Confirmar reserva
          </button>
          <p id="errorForm" class="hidden text-sm text-red-700 dark:text-red-400"></p>
          ${recordado ? "" : `
            <p class="text-xs text-slate-500 dark:text-slate-400">
              ¿Ya compraste antes? <a href="#/registro" class="underline">Registrate</a>
              para no cargar los datos cada vez.
            </p>`}
        </form>
      </section>

      ${resumenCompra()}
    </div>
  `;

  const formulario = contenedor.querySelector("#datos");
  const errorForm = contenedor.querySelector("#errorForm");

  // Cambiar una tarifa acá repinta solo el resumen y no el formulario: si repintara la
  // vista entera, se perdería lo que la persona ya tipeó en nombre y email.
  contenedor.addEventListener("change", (evento) => {
    const select = evento.target.closest("select[data-tarifa-de]");
    if (!select) return;
    seleccion.butacas[select.dataset.tarifaDe] = select.value;
    contenedor.querySelector("#resumenCompra").outerHTML = resumenCompra();
  });
  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const datos = new FormData(formulario);
    try {
      const reserva = await api.crearReserva({
        funcionId: funcion.id,
        nombre: datos.get("nombre"),
        email: datos.get("email"),
        butacas: seleccion.butacas,
        // La misma sesión con la que se bloquearon: sin esto, el propio bloqueo haría
        // rebotar la reserva por butaca ocupada.
        sesion: sesionDeCompra(),
      });
      // Comprar sin registrarse igual deja los datos listos para la próxima.
      recordarCliente({ nombre: datos.get("nombre").trim(), email: datos.get("email").trim() });
      seleccion.funcionId = null;
      seleccion.butacas = {};
      ir(`#/ticket/${reserva.id}`);
    } catch (e) {
      // 409: alguien tomó la butaca en el medio. Dejar el resumen como está sería
      // mostrarle butacas que ya no puede comprar, así que vuelve al mapa recargado.
      if (e.status === 409) {
        seleccion.butacas = {};
        avisar(e.message, "error");
        ir(`#/funcion/${funcion.id}`);
        return;
      }
      errorForm.textContent = e.message;
      errorForm.classList.remove("hidden");
    }
  });

  // Completar el formulario lleva más de lo que dura un bloqueo: sin renovarlo, la
  // persona perdería las butacas justo mientras tipea el mail.
  renovarMientrasSigaAca(funcion.id);
}
