import * as api from "../api.js";
import { boton, botonSecundario, campo, chip, filaTabla, panel, select, tabla } from "../componentes.js";
import { avisar, escapar } from "../dom.js";
import { TIPOS_PRODUCTO_SUELTO, etiqueta } from "../etiquetas.js";
import { fechaHora, hora, hoyISO, precio, precioExacto } from "../formato.js";

const PESTANAS = [
  ["", "Carta"],
  ["venta", "Venta de mostrador"],
  ["ventas", "Ventas del día"],
];

function encabezado(actual) {
  return `
    <h1 class="mb-1 text-2xl font-bold">Candy</h1>
    <p class="mb-4 text-sm text-slate-500 dark:text-slate-400">
      La otra caja del cine: se cobra en el mostrador y se entrega, sin reserva de por medio.
    </p>
    <div class="mb-5 flex flex-wrap gap-2 text-sm">
      ${PESTANAS.map(([ruta, texto]) => `
        <a href="#/candy${ruta ? "/" + ruta : ""}"
          class="rounded px-3 py-1.5 ${ruta === actual
            ? "bg-slate-900 text-white dark:bg-white dark:text-slate-900"
            : "border border-slate-400 hover:bg-slate-100 dark:border-slate-600 dark:hover:bg-slate-800"}">${texto}</a>`).join("")}
    </div>`;
}

function componentesDe(producto) {
  if (!producto.componentes?.length) return "—";
  return producto.componentes.map((c) => `${c.cantidad}× ${escapar(c.nombre)}`).join(" + ");
}

function error(id) {
  return `<p id="${id}" class="hidden text-sm text-red-700 dark:text-red-400"></p>`;
}

function mostrarError(nodo, mensaje) {
  nodo.textContent = mensaje;
  nodo.classList.remove("hidden");
}

export async function vistaCandy(contenedor, pestana = "") {
  if (pestana === "venta") return vistaVenta(contenedor);
  if (pestana === "ventas") return vistaVentas(contenedor);
  return vistaCarta(contenedor);
}

async function vistaCarta(contenedor, editando = null) {
  const productos = await api.obtenerProductosCandy(true);
  const sueltos = productos.filter((p) => !p.esCombo);

  contenedor.innerHTML = `
    ${encabezado("")}

    <div class="grid gap-4 lg:grid-cols-[1fr_340px]">
      ${panel(`
        ${tabla('<tr><th class="p-2">Producto</th><th>Tipo</th><th>Trae</th><th class="text-right">Precio</th><th>Estado</th><th></th></tr>',
          productos.length ? productos.map((p) => p.id === editando ? filaEdicion(p) : `
              <tr class="${filaTabla(p.disponible ? "" : "text-slate-400 dark:text-slate-500")}">
                <td class="p-2 font-medium">${escapar(p.nombre)}</td>
                <td>${chip(etiqueta(p.tipo), p.esCombo
                  ? "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300"
                  : undefined)}</td>
                <td class="text-xs">${componentesDe(p)}</td>
                <td class="text-right whitespace-nowrap font-semibold">${precio(p.precio)}</td>
                <td class="whitespace-nowrap text-xs">${p.disponible ? "A la venta" : "Fuera de la carta"}</td>
                <td class="p-2 text-right whitespace-nowrap">
                  <button type="button" data-editar="${p.id}"
                    class="text-xs text-slate-700 hover:underline dark:text-slate-200">Editar</button>
                  <button type="button" data-disponible="${p.disponible ? "false" : "true"}" data-id="${p.id}"
                    class="ml-2 text-xs ${p.disponible ? "text-red-700 dark:text-red-400" : "text-emerald-700 dark:text-emerald-400"} hover:underline">
                    ${p.disponible ? "Sacar de la carta" : "Reponer"}
                  </button>
                </td>
              </tr>`).join("")
            : '<tr><td colspan="6" class="p-6 text-center text-slate-500 dark:text-slate-400">Todavía no hay productos.</td></tr>')}
        <p id="errorCarta" class="hidden p-2 text-sm text-red-700 dark:text-red-400"></p>
        <p class="border-t border-slate-200 p-2 text-xs text-slate-500 dark:border-slate-800 dark:text-slate-400">
          Los productos no se borran: se sacan de la carta. Uno que ya se vendió tiene que
          seguir existiendo para que el ticket de esa venta diga qué se llevó. Cambiar un
          precio no toca las ventas hechas: cada una guardó el precio que tenía.
        </p>
      `, "overflow-x-auto")}

      <div class="space-y-4">
        ${panel(`
          <h2 class="mb-3 font-semibold">Nuevo producto</h2>
          <form id="altaProducto" class="space-y-3">
            ${campo({ nombre: "nombre", etiqueta: "Nombre", requerido: true, placeholder: "Pochoclos grandes" })}
            ${select({ nombre: "tipo", etiqueta: "Tipo",
              opciones: TIPOS_PRODUCTO_SUELTO.map((t) => `<option value="${t}">${etiqueta(t)}</option>`).join("") })}
            ${campo({ nombre: "precio", etiqueta: "Precio", tipo: "number", requerido: true, extra: 'min="1" step="any"' })}
            ${boton("Agregar a la carta")}
            ${error("errorProducto")}
          </form>
        `, "p-4")}

        ${panel(`
          <h2 class="mb-1 font-semibold">Armar combo</h2>
          <p class="mb-3 text-xs text-slate-500 dark:text-slate-400">
            Al menos dos productos. El combo tiene que salir menos que sus componentes
            sueltos (R14): si no, no habría motivo para ofrecerlo.
          </p>
          <form id="altaCombo" class="space-y-3">
            ${campo({ nombre: "nombreCombo", etiqueta: "Nombre", requerido: true, placeholder: "Combo clásico" })}
            <fieldset class="text-sm">
              <legend class="text-slate-600 dark:text-slate-300">Qué trae (cantidad)</legend>
              ${sueltos.length ? sueltos.map((p) => `
                <label class="mt-1 flex items-center justify-between gap-2">
                  <span class="${p.disponible ? "" : "text-slate-400 dark:text-slate-500"}">${escapar(p.nombre)}
                    <span class="text-xs text-slate-500 dark:text-slate-400">· ${precio(p.precio)}</span></span>
                  <input type="number" min="0" value="0" data-componente="${p.id}"
                    class="w-16 rounded border border-slate-400 px-2 py-1 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100" />
                </label>`).join("")
                : '<p class="text-xs text-slate-500 dark:text-slate-400">Primero cargá productos sueltos.</p>'}
            </fieldset>
            ${campo({ nombre: "precioCombo", etiqueta: "Precio del combo", tipo: "number", requerido: true, extra: 'min="1" step="any"' })}
            ${boton("Armar combo")}
            ${error("errorCombo")}
          </form>
        `, "p-4")}
      </div>
    </div>
  `;

  const recargar = (id = null) => vistaCarta(contenedor, id);

  contenedor.querySelector("#altaProducto").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const datos = new FormData(evento.target);
    try {
      await api.crearProductoCandy({
        nombre: datos.get("nombre"), tipo: datos.get("tipo"), precio: datos.get("precio"),
      });
      avisar("Producto agregado");
      recargar();
    } catch (e) {
      mostrarError(contenedor.querySelector("#errorProducto"), e.message);
    }
  });

  contenedor.querySelector("#altaCombo").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const componentes = {};
    evento.target.querySelectorAll("[data-componente]").forEach((input) => {
      if (Number(input.value) > 0) componentes[input.dataset.componente] = Number(input.value);
    });
    const errorCombo = contenedor.querySelector("#errorCombo");
    if (Object.keys(componentes).length < 2) {
      mostrarError(errorCombo, "Un combo tiene que juntar al menos dos productos distintos");
      return;
    }
    const datos = new FormData(evento.target);
    try {
      await api.armarComboCandy({
        nombre: datos.get("nombreCombo"), precio: datos.get("precioCombo"), componentes,
      });
      avisar("Combo armado");
      recargar();
    } catch (e) {
      mostrarError(errorCombo, e.message);
    }
  });

  const tablaCarta = contenedor.querySelector("table");
  tablaCarta.addEventListener("click", async (evento) => {
    const editar = evento.target.closest("button[data-editar]");
    const disponible = evento.target.closest("button[data-disponible]");
    const cancelar = evento.target.closest("button[data-cancelar]");
    if (editar) return recargar(Number(editar.dataset.editar));
    if (cancelar) return recargar();
    if (!disponible) return;
    try {
      await api.cambiarDisponibilidadCandy(disponible.dataset.id, disponible.dataset.disponible === "true");
      recargar();
    } catch (e) {
      avisar(e.message, "error");
    }
  });

  tablaCarta.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const formulario = evento.target;
    try {
      await api.editarProductoCandy(formulario.dataset.id, {
        nombre: formulario.nombre.value, precio: formulario.precio.value,
      });
      avisar("Producto actualizado");
      recargar();
    } catch (e) {
      mostrarError(contenedor.querySelector("#errorCarta"), e.message);
    }
  });
}

function filaEdicion(p) {
  const clases = "w-full rounded border border-slate-400 px-2 py-1 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100";
  return `
    <tr class="${filaTabla("bg-amber-50 dark:bg-amber-950/30")}">
      <td colspan="6" class="p-2">
        <form data-id="${p.id}" class="flex flex-wrap items-end gap-2">
          <label class="flex-1 text-xs">Nombre
            <input name="nombre" required value="${escapar(p.nombre)}" class="${clases}" /></label>
          <label class="w-32 text-xs">Precio
            <input name="precio" type="number" min="1" step="any" required value="${p.precio}" class="${clases}" /></label>
          ${boton("Guardar", { ancho: "", tamano: "px-3 py-1.5" })}
          ${botonSecundario("Cancelar", { atributos: "data-cancelar" })}
        </form>
        ${p.esCombo ? `<p class="mt-1 text-xs text-slate-500 dark:text-slate-400">Trae ${componentesDe(p)}. Los componentes se fijan al armarlo.</p>` : ""}
      </td>
    </tr>`;
}

async function vistaVenta(contenedor) {
  const [productos, medios] = await Promise.all([
    api.obtenerProductosCandy(false),
    api.obtenerMediosPago(),
  ]);

  contenedor.innerHTML = `
    ${encabezado("venta")}

    <div class="grid gap-4 lg:grid-cols-[1fr_360px]">
      ${panel(`
        <form id="venta" class="space-y-4">
          ${tabla('<tr><th class="p-2">Producto</th><th>Trae</th><th class="text-right">Precio</th><th class="p-2 text-right">Cantidad</th></tr>',
            productos.length ? productos.map((p) => `
              <tr class="${filaTabla()}">
                <td class="p-2 font-medium">${escapar(p.nombre)}</td>
                <td class="text-xs">${componentesDe(p)}</td>
                <td class="text-right whitespace-nowrap">${precio(p.precio)}</td>
                <td class="p-2 text-right">
                  <input type="number" min="0" value="0" data-producto="${p.id}"
                    class="w-16 rounded border border-slate-400 px-2 py-1 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100" />
                </td>
              </tr>`).join("")
              : '<tr><td colspan="4" class="p-6 text-center text-slate-500 dark:text-slate-400">No hay nada a la venta. Cargá productos en la carta.</td></tr>')}

          <div class="grid gap-3 px-4 sm:grid-cols-2">
            ${select({ nombre: "medio", etiqueta: "Medio de pago",
              opciones: medios.map((m) => `<option value="${m.nombre}">${etiqueta(m.nombre)}</option>`).join("") })}
            <div id="bloqueCodigo">
              ${campo({ nombre: "codigo", etiqueta: "Código de autorización", extra: 'autocomplete="off"',
                pista: "El que da el posnet o la app (R11). En efectivo no hace falta." })}
            </div>
            ${campo({ nombre: "email", etiqueta: "Cliente (opcional)", tipo: "email", placeholder: "email del cliente" })}
            ${campo({ nombre: "reservaId", etiqueta: "Reserva (opcional)", tipo: "number", extra: 'min="1"',
              pista: "Con reserva, el cliente sale de ella y la venta suma al informe de esa función." })}
          </div>
          <div class="px-4 pb-4">
            ${boton("Cobrar")}
            ${error("errorVenta")}
          </div>
        </form>
      `, "overflow-x-auto")}

      <div id="ticket">
        ${panel(`
          <p class="text-sm text-slate-500 dark:text-slate-400">
            El total lo calcula el backend con los precios de la carta: acá no se tipea.
            Al cobrar aparece el ticket.
          </p>`, "p-4")}
      </div>
    </div>
  `;

  const formulario = contenedor.querySelector("#venta");
  const bloqueCodigo = contenedor.querySelector("#bloqueCodigo");
  const errorVenta = contenedor.querySelector("#errorVenta");

  const requiereCodigo = () =>
    medios.find((m) => m.nombre === formulario.medio.value)?.requiereAutorizacion;
  const ajustarMedio = () => bloqueCodigo.classList.toggle("hidden", !requiereCodigo());
  formulario.medio.addEventListener("change", ajustarMedio);
  ajustarMedio();

  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    errorVenta.classList.add("hidden");
    const cantidades = {};
    formulario.querySelectorAll("[data-producto]").forEach((input) => {
      if (Number(input.value) > 0) cantidades[input.dataset.producto] = Number(input.value);
    });
    if (!Object.keys(cantidades).length) {
      mostrarError(errorVenta, "Hay que elegir al menos un producto");
      return;
    }
    const codigo = formulario.codigo.value.trim();
    if (requiereCodigo() && !codigo) {
      mostrarError(errorVenta, `El pago con ${etiqueta(formulario.medio.value)} necesita código de autorización`);
      return;
    }

    try {
      const reservaId = formulario.reservaId.value.trim();
      let clienteId = null;
      const email = formulario.email.value.trim();
      // Con reserva el email sobra: el backend toma el cliente de la reserva.
      if (email && !reservaId) {
        const cliente = await api.buscarClientePorEmail(email);
        if (!cliente) {
          mostrarError(errorVenta, `No hay ningún cliente con el email ${email}`);
          return;
        }
        clienteId = cliente.id;
      }
      const compra = await api.venderCandy({
        clienteId, reservaId, cantidades,
        medio: formulario.medio.value,
        codigoAutorizacion: requiereCodigo() ? codigo : "",
      });
      avisar(`Cobrado ${precio(compra.total)}`);
      contenedor.querySelector("#ticket").innerHTML = panel(`
        <p class="mb-2 text-sm font-semibold">Venta #${compra.id}</p>
        <pre class="overflow-x-auto text-xs leading-5">${escapar(armarTicket(compra))}</pre>
      `, "p-4");
      formulario.querySelectorAll("[data-producto]").forEach((input) => { input.value = 0; });
      formulario.codigo.value = "";
    } catch (e) {
      mostrarError(errorVenta, e.message);
    }
  });
}

const LINEA = "=".repeat(40);

function armarTicket(compra) {
  const renglon = (izquierda, derecha) => ` ${izquierda.padEnd(26)}${derecha.padStart(12)}`;
  return [
    LINEA,
    "  CINE UADE · CANDY",
    `  ${fechaHora(compra.fecha)}`,
    LINEA,
    ...compra.items.map((i) => renglon(`${i.cantidad}x ${i.nombre}`.slice(0, 26), precioExacto(i.subtotal))),
    LINEA,
    renglon("TOTAL", precioExacto(compra.total)),
    ...(compra.ahorro > 0 ? [renglon("Ahorro por combos", precioExacto(compra.ahorro))] : []),
    renglon("Medio", etiqueta(compra.medio)),
    ...(compra.codigoAutorizacion ? [renglon("Autorizacion", compra.codigoAutorizacion)] : []),
    ...(compra.reservaId ? [renglon("Reserva", "#" + compra.reservaId)] : []),
    LINEA,
  ].join("\n");
}

async function vistaVentas(contenedor, fecha = hoyISO()) {
  const compras = await api.obtenerComprasCandy({ fecha });

  contenedor.innerHTML = `
    ${encabezado("ventas")}
    <div class="mb-5 flex flex-wrap items-end gap-4">
      ${campo({ nombre: "fecha", etiqueta: "Fecha", tipo: "date", valor: fecha, ancho: "block" })}
      ${panel(`
        <span class="text-xs uppercase text-slate-500 dark:text-slate-400">Ventas</span>
        <p class="text-2xl font-bold">${compras.length}</p>`, "px-4 py-2")}
    </div>
    ${panel(tablaCompras(compras), "overflow-x-auto")}
    <p class="mt-2 text-xs text-slate-500 dark:text-slate-400">
      El total cobrado del día está en <a href="#/caja" class="underline">Caja</a>, al lado de la boletería.
    </p>
  `;

  contenedor.querySelector("#fecha").addEventListener("change", (evento) => {
    vistaVentas(contenedor, evento.target.value);
  });
}

export function tablaCompras(compras) {
  return tabla(`
      <tr>
        <th class="p-2">Hora</th><th>Venta</th><th>Qué se llevó</th><th>Reserva</th>
        <th>Medio</th><th>Autorización</th><th class="text-right">Ahorro</th>
        <th class="p-2 text-right">Total</th>
      </tr>`, compras.length ? compras.map((c) => `
      <tr class="${filaTabla()}">
        <td class="p-2 whitespace-nowrap">${hora(c.fecha)}</td>
        <td>#${c.id}</td>
        <td class="text-xs">${c.items.map((i) => `${i.cantidad}× ${escapar(i.nombre)}`).join(", ")}</td>
        <td>${c.reservaId ? "#" + c.reservaId : "—"}</td>
        <td>${etiqueta(c.medio)}</td>
        <td class="font-mono text-xs">${escapar(c.codigoAutorizacion || "—")}</td>
        <td class="text-right whitespace-nowrap text-xs ${c.ahorro > 0 ? "text-emerald-700 dark:text-emerald-400" : "text-slate-400 dark:text-slate-500"}">
          ${c.ahorro > 0 ? precio(c.ahorro) : "—"}
        </td>
        <td class="p-2 text-right whitespace-nowrap font-medium">${precio(c.total)}</td>
      </tr>`).join("")
      : '<tr><td colspan="8" class="p-6 text-center text-slate-500 dark:text-slate-400">No se vendió nada ese día.</td></tr>');
}
