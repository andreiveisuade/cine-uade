import * as api from "../api.js";
import { ir } from "../router.js";
import { avisar, chipEstado, conEspera, dia, escapar, etiqueta, fechaHora, hora, precio, error } from "../ui.js";

/* -------------------------------------------------------- listado de reservas */

/**
 * Los estados en el orden en que le importan a quien atiende: primero lo que hay que
 * cobrar hoy, al final lo que ya no se toca.
 */
const ESTADOS = ["RESERVADA", "PAGADA", "EXPIRADA", "CANCELADA"];

export async function vistaReservas(contenedor) {
  const reservas = await api.obtenerReservas();
  const activas = reservas.filter((r) => r.estado !== "CANCELADA");
  const aCobrar = reservas.filter((r) => r.estado === "RESERVADA");

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Reservas</h1>
    <p class="mb-3 text-sm text-slate-500 dark:text-slate-400">
      ${reservas.length} reservas · ${activas.length} activas ·
      ${aCobrar.length} pendientes de cobro
      ${aCobrar.length ? `(${precio(aCobrar.reduce((s, r) => s + r.total, 0))})` : ""}
    </p>

    <div class="mb-4 flex flex-wrap items-end gap-3">
      <label class="text-sm">
        <span class="text-slate-600 dark:text-slate-300">Buscar</span>
        <input id="busqueda" type="search" placeholder="cliente, email, película o butaca"
          class="mt-1 block w-72 rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
      </label>
      <label class="text-sm">
        <span class="text-slate-600 dark:text-slate-300">Estado</span>
        <select id="estado" class="mt-1 block rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100">
          <option value="">Todos</option>
          ${ESTADOS.map((e) => `<option value="${e}">${etiqueta(e)}</option>`).join("")}
        </select>
      </label>
      <label class="text-sm">
        <span class="text-slate-600 dark:text-slate-300">Función del día</span>
        <input id="fecha" type="date"
          class="mt-1 block rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100" />
      </label>
      <button type="button" id="limpiar" class="rounded border border-slate-400 px-3 py-1.5 text-sm dark:border-slate-600">Limpiar</button>
      <p id="cuenta" class="text-sm text-slate-500 dark:text-slate-400"></p>
    </div>

    <div class="overflow-x-auto rounded border border-slate-300 bg-white dark:border-slate-700 dark:bg-slate-900">
      <table class="w-full text-sm">
        <thead class="border-b border-slate-300 bg-slate-50 text-left text-xs uppercase text-slate-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-400">
          <tr>
            <th class="p-2">#</th><th>Función</th><th>Cliente</th>
            <th>Butacas</th><th class="text-right">Total</th><th>Estado</th><th></th>
          </tr>
        </thead>
        <tbody>${filas(reservas)}</tbody>
      </table>
    </div>
  `;

  const busqueda = contenedor.querySelector("#busqueda");
  const estado = contenedor.querySelector("#estado");
  const fecha = contenedor.querySelector("#fecha");
  const cuenta = contenedor.querySelector("#cuenta");
  const cuerpo = contenedor.querySelector("tbody");

  // El filtro lo resuelve el backend: los criterios son del negocio, no de la pantalla,
  // y viajando en la URL se pueden probar con un curl y compartir pegando el link.
  async function aplicarFiltros() {
    const visibles = await api.obtenerReservas({
      estado: estado.value, dia: fecha.value, q: busqueda.value,
    });
    cuerpo.innerHTML = filas(visibles);
    cuenta.textContent = visibles.length === reservas.length
      ? "" : `mostrando ${visibles.length} de ${reservas.length}`;
  }

  // Con debounce, porque ahora cada tecla sería un pedido: se espera a que deje de
  // escribir. Los selects no lo necesitan —un cambio es una decisión, no un tanteo.
  busqueda.addEventListener("input", conEspera(aplicarFiltros));
  estado.addEventListener("change", aplicarFiltros);
  fecha.addEventListener("change", aplicarFiltros);
  contenedor.querySelector("#limpiar").addEventListener("click", () => {
    busqueda.value = "";
    estado.value = "";
    fecha.value = "";
    aplicarFiltros();
    busqueda.focus();
  });

  cuerpo.addEventListener("click", async (evento) => {
    const boton = evento.target.closest("button[data-cancelar]");
    if (!boton) return;
    try {
      await api.cancelarReserva(boton.dataset.cancelar);
      avisar("Reserva cancelada, las butacas quedaron libres");
      vistaReservas(contenedor);
    } catch (e) {
      avisar(e.message, "error");
    }
  });
}


/** Una fila por reserva. Se separa para poder repintar solo el cuerpo al filtrar. */
function filas(reservas) {
  if (!reservas.length) {
    return `<tr><td colspan="7" class="p-6 text-center text-sm text-slate-500 dark:text-slate-400">
      Ninguna reserva coincide con el filtro.
    </td></tr>`;
  }
  return reservas.map((r) => `
            <tr class="border-b border-slate-200 dark:border-slate-800 ${r.estado === "CANCELADA" ? "text-slate-400 dark:text-slate-500" : ""}">
              <td class="p-2">${r.id}</td>
              <td>
                ${escapar(r.pelicula?.titulo || "—")}
                <span class="block text-xs text-slate-500 dark:text-slate-400">
                  ${r.funcion ? `${escapar(dia(r.funcion.inicio))} ${hora(r.funcion.inicio)} · ${escapar(r.sala.nombre)}` : ""}
                </span>
              </td>
              <td>
                ${escapar(r.cliente?.nombre || "—")}
                <span class="block text-xs text-slate-500 dark:text-slate-400">${escapar(r.cliente?.email || "")}</span>
              </td>
              <td class="font-mono text-xs">${r.entradas.map((e) => e.codigo).join(", ")}</td>
              <td class="text-right whitespace-nowrap">${precio(r.total)}</td>
              <td class="p-2">
                ${chipEstado(r.estado)}
                ${r.pago
                  ? `<span class="block text-xs text-slate-500 dark:text-slate-400">${etiqueta(r.pago.medio)} · ${hora(r.pago.fecha)}</span>`
                  : ""}
              </td>
              <td class="p-2 text-right whitespace-nowrap">
                ${r.estado === "RESERVADA" ? `
                  <a href="#/cobrar/${r.id}" class="text-xs font-medium text-slate-900 hover:underline dark:text-slate-100">Cobrar</a>
                  <button type="button" data-cancelar="${r.id}"
                    class="ml-2 text-xs text-red-700 hover:underline dark:text-red-400">Cancelar</button>` : ""}
              </td>
            </tr>`).join("");
}

/* ------------------------------------------------------------------- cobrar */

export async function vistaCobrar(contenedor, id) {
  const [reservas, medios] = await Promise.all([api.obtenerReservas(), api.obtenerMediosPago()]);
  const reserva = reservas.find((r) => r.id === Number(id));
  if (!reserva) throw new Error(`No existe la reserva ${id}`);

  if (reserva.estado !== "RESERVADA") {
    contenedor.innerHTML = `
      <a href="#/reservas" class="text-sm text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-100">&larr; Reservas</a>
      <div class="mt-4 rounded border border-amber-300 bg-amber-50 p-4 text-amber-900 dark:border-amber-800 dark:bg-amber-950 dark:text-amber-300">
        La reserva ${reserva.id} está ${etiqueta(reserva.estado).toLowerCase()}, no se puede cobrar.
        ${reserva.pago
          ? `Se cobró ${precio(reserva.pago.monto)} con ${etiqueta(reserva.pago.medio)}${
              reserva.pago.descuento > 0
                ? ` (subtotal ${precio(reserva.pago.subtotal)} − ${precio(reserva.pago.descuento)} de promoción)`
                : ""}
             el ${escapar(fechaHora(reserva.pago.fecha))}.`
          : ""}
      </div>`;
    return;
  }

  contenedor.innerHTML = `
    <a href="#/reservas" class="text-sm text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-100">&larr; Reservas</a>
    <h1 class="mt-2 mb-5 text-2xl font-bold">Cobrar reserva #${reserva.id}</h1>

    <div class="grid gap-4 md:grid-cols-2 md:items-start">
      <section class="rounded border border-slate-300 bg-white dark:border-slate-700 dark:bg-slate-900 p-4">
        <h2 class="mb-3 font-semibold">Cobro</h2>
        <form id="cobro" class="space-y-3">
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Medio de pago</span>
            <select name="medio" class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500">
              ${medios.map((m) => `<option value="${m.nombre}">${etiqueta(m.nombre)}</option>`).join("")}
            </select>
          </label>
          <div class="rounded bg-slate-100 p-3 text-sm dark:bg-slate-800">
            <span class="text-slate-600 dark:text-slate-300">A cobrar</span>
            <p class="text-xl font-bold">${precio(reserva.total)}</p>
            <p class="text-xs text-slate-500 dark:text-slate-400">
              Sale del total de las butacas: no se puede cobrar otro importe. Si hay una
              promoción vigente para este medio de pago, el descuento se aplica al cobrar.
            </p>
          </div>
          <p id="comoCobra" class="text-xs text-slate-500 dark:text-slate-400"></p>
          <button type="submit" id="botonCobro"
            class="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white dark:bg-white dark:text-slate-900">
            Registrar cobro
          </button>
        </form>
        <div id="checkout" class="mt-3"></div>
      </section>

      <section class="rounded border border-slate-300 bg-white dark:border-slate-700 dark:bg-slate-900 p-4">
        <h2 class="mb-1 font-semibold">${escapar(reserva.pelicula?.titulo || "—")}</h2>
        <p class="mb-3 text-sm text-slate-600 dark:text-slate-300">
          ${reserva.funcion
            ? `${escapar(dia(reserva.funcion.inicio))} ${hora(reserva.funcion.inicio)} ·
               ${escapar(reserva.sala.nombre)} (${etiqueta(reserva.sala.tipo)})`
            : ""}
        </p>
        <p class="mb-3 text-sm">
          ${escapar(reserva.cliente?.nombre || "—")}
          <span class="block text-xs text-slate-500 dark:text-slate-400">${escapar(reserva.cliente?.email || "")}</span>
        </p>
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-slate-300 text-left text-xs uppercase text-slate-500 dark:border-slate-700 dark:text-slate-400">
              <th class="py-1">Butaca</th><th>Tarifa</th><th class="text-right">Precio</th>
            </tr>
          </thead>
          <tbody>
            ${reserva.entradas.map((e) => `
              <tr class="border-b border-slate-200 dark:border-slate-800">
                <td class="py-1 font-medium">${e.codigo}</td>
                <td class="text-xs ${e.tarifa && e.tarifa !== "GENERAL" ? "font-semibold text-amber-800 dark:text-amber-300" : "text-slate-500 dark:text-slate-400"}">
                  ${etiqueta(e.tarifa || "GENERAL")}
                </td>
                <td class="text-right">${precio(e.precio)}</td>
              </tr>`).join("")}
          </tbody>
          <tfoot>
            <tr class="font-semibold">
              <td class="py-2" colspan="2">Subtotal</td>
              <td class="py-2 text-right">${precio(reserva.total)}</td>
            </tr>
          </tfoot>
        </table>
      </section>
    </div>
  `;

  const formulario = contenedor.querySelector("#cobro");
  const panelCheckout = contenedor.querySelector("#checkout");
  const boton = contenedor.querySelector("#botonCobro");
  const comoCobra = contenedor.querySelector("#comoCobra");

  const porCheckout = () =>
    medios.find((m) => m.nombre === formulario.medio.value).requiereAutorizacion;

  /**
   * R11 partido en dos caminos. El código de autorización de un medio electrónico lo
   * devuelve el procesador, así que dejó de tipearse a mano: ese campo era una invitación
   * a inventar un código y registrar un cobro que nadie autorizó. El efectivo no tiene
   * procesador ni código, y se sigue cobrando en la caja.
   */
  function ajustarMedio() {
    // Un checkout es de un medio y un monto concretos: cambiar el medio lo invalida.
    panelCheckout.innerHTML = "";
    boton.textContent = porCheckout() ? "Abrir checkout" : "Registrar cobro";
    comoCobra.textContent = porCheckout()
      ? "El cliente paga en la pasarela y el código de autorización lo devuelve ella."
      : "Se cobra en la caja del cine. El efectivo no lleva código de autorización.";
  }
  formulario.medio.addEventListener("change", ajustarMedio);
  ajustarMedio();

  /** Con descuento no alcanza con decir cuánto entró: hay que poder explicar por qué se
   *  cobró menos que el subtotal, que es justo lo que el cliente va a preguntar. */
  function avisarCobro(pago) {
    avisar(pago.descuento > 0
      ? `Cobrado ${precio(pago.monto)} con ${etiqueta(pago.medio)} · ${precio(pago.descuento)} de descuento`
      : `Cobrado ${precio(pago.monto)} con ${etiqueta(pago.medio)}`);
    ir("#/caja");
  }

  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const medio = formulario.medio.value;
    try {
      if (!porCheckout()) {
        avisarCobro(await api.cobrar(reserva.id, medio, ""));
        return;
      }
      // Abrir el checkout todavía no cobra: valida R5, R17 y R19 y devuelve qué tiene que
      // aprobar el cliente. Se valida acá y no al confirmar porque mandar a pagar una
      // reserva que no se puede cobrar termina en plata que hay que devolver, y la
      // devolución es justo lo que no existe (R13).
      panelCheckout.innerHTML = dibujarCheckout(await api.abrirCheckout(reserva.id, medio));
    } catch (e) {
      avisar(e.message, "error");
    }
  });

  panelCheckout.addEventListener("click", async (evento) => {
    const confirmar = evento.target.closest("button[data-confirmar]");
    if (!confirmar) return;
    confirmar.disabled = true;
    try {
      // Qué se está pagando sale del checkout, no de quien confirma.
      avisarCobro(await api.confirmarCheckout(confirmar.dataset.confirmar));
    } catch (e) {
      confirmar.disabled = false;
      avisar(e.message, "error");
    }
  });
}

/**
 * El checkout abierto: lo que el cliente tiene que aprobar en la pasarela.
 *
 * `codigoQr` es el *contenido* del QR y no una imagen, así que se muestra tal cual: la
 * pasarela es una emulación y el host no existe, de modo que dibujar el cuadrado o linkear
 * la URL harían parecer real algo que no lo es.
 */
function dibujarCheckout(checkout) {
  return `
    <div class="rounded border border-slate-300 bg-slate-50 p-3 dark:border-slate-700 dark:bg-slate-800">
      <p class="text-sm font-semibold">Checkout abierto · ${etiqueta(checkout.medio)}</p>
      <p class="mb-2 text-xs text-slate-500 dark:text-slate-400">${escapar(checkout.id)}</p>

      <p class="text-sm text-slate-600 dark:text-slate-300">El cliente aprueba</p>
      <p class="mb-3 text-2xl font-bold">${precio(checkout.monto)}</p>

      <p class="text-xs text-slate-600 dark:text-slate-300">Contenido del QR</p>
      <p class="mb-2 break-all rounded border border-dashed border-slate-400 bg-white p-2 font-mono text-xs dark:border-slate-600 dark:bg-slate-900">
        ${escapar(checkout.codigoQr)}
      </p>
      <p class="text-xs text-slate-600 dark:text-slate-300">Link de pago</p>
      <p class="mb-3 break-all font-mono text-xs text-slate-500 dark:text-slate-400">${escapar(checkout.urlPago)}</p>

      <button type="button" data-confirmar="${escapar(checkout.id)}"
        class="w-full rounded bg-emerald-700 px-4 py-2 text-sm font-medium text-white disabled:bg-slate-400 dark:bg-emerald-600">
        El cliente pagó · confirmar
      </button>
      <p class="mt-2 text-xs text-slate-500 dark:text-slate-400">
        El monto <strong>ya tiene el descuento aplicado</strong>: es el importe que se
        aprueba, y si después se cobrara otro no coincidirían. La pasarela es una
        emulación —el host no existe—, así que el aviso de «pagó» lo da esta pantalla;
        en una integración de verdad lo dispara el procesador.
      </p>
    </div>`;
}
