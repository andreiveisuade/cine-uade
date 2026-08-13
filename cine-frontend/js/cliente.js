// Vistas del cliente: cartelera, detalle de película, mapa de butacas,
// confirmación y ticket. Sin login.

import * as api from "./api.js";
import { CLASES_TIPO, dibujarMapa, pantalla, referencia } from "./butacas.js";
import { iniciarRouter, ir } from "./router.js";
import {
  avisar, chip, chipClasificacion, chipEstado, dia, duracion, escapar, etiqueta, fechaHora,
  hora, imagenPoster, porDia, precio, precioExacto,
} from "./ui.js";

// Lo elegido en el mapa de butacas, para que lo lea la confirmación. Es un mapa de
// código a tarifa y no una lista, porque la tarifa es por persona: en una reserva de
// cuatro puede haber dos generales, un menor y un jubilado. De paso, elegir dos veces
// la misma butaca es imposible de expresar.
const seleccion = { funcionId: null, butacas: {} };

// El catálogo de tarifas sale del backend y no se repite acá: el multiplicador vive en
// el enum del dominio, y tenerlo duplicado serían dos fuentes de verdad para el precio.
let tarifas = null;

async function catalogoTarifas() {
  if (!tarifas) tarifas = await api.obtenerTarifas();
  return tarifas;
}

function tarifaPorNombre(nombre) {
  return (tarifas || []).find((t) => t.nombre === nombre) || { multiplicador: 1, requiereAcreditacion: false };
}

/** El precio de esa butaca con esa tarifa. asiento.precio siempre viene en GENERAL. */
function precioConTarifa(asiento, nombreTarifa) {
  return Math.round(asiento.precio * tarifaPorNombre(nombreTarifa).multiplicador * 100) / 100;
}

/** Un <select> de tarifas para una butaca. */
function selectorTarifa(codigo, elegida) {
  const opciones = (tarifas || []).map((t) =>
    `<option value="${t.nombre}" ${t.nombre === elegida ? "selected" : ""}>${etiqueta(t.nombre)}</option>`,
  ).join("");
  return `<select data-tarifa-de="${codigo}"
    class="rounded border border-slate-400 px-1 py-0.5 text-xs">${opciones}</select>`;
}

// El cliente no inicia sesión. Recordar sus datos en el navegador es lo que hace que
// registrarse sirva de algo: no vuelve a tipearlos al comprar ni al buscar sus reservas.
const CLAVE_CLIENTE = "cine.cliente";

function clienteRecordado() {
  try {
    return JSON.parse(localStorage.getItem(CLAVE_CLIENTE)) || null;
  } catch {
    return null;
  }
}

function recordarCliente(cliente) {
  localStorage.setItem(CLAVE_CLIENTE,
    JSON.stringify({ nombre: cliente.nombre, email: cliente.email }));
}

/* ------------------------------------------------------------------- registro */

async function vistaRegistro(contenedor) {
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
    localStorage.removeItem(CLAVE_CLIENTE);
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

/* ---------------------------------------------------------------- cartelera */

async function vistaCartelera(contenedor, generoFiltrado) {
  const [peliculas, generos] = await Promise.all([
    api.obtenerCartelera(generoFiltrado),
    api.obtenerGeneros(),
  ]);

  const filtros = [
    `<a href="#/cartelera" class="rounded-full px-3 py-1 text-xs font-medium ${
      generoFiltrado ? "bg-slate-200 text-slate-700" : "bg-slate-900 text-white"}">Todos</a>`,
    ...generos.map((g) => `
      <a href="#/cartelera/${g}" class="rounded-full px-3 py-1 text-xs font-medium ${
        g === generoFiltrado ? "bg-slate-900 text-white" : "bg-slate-200 text-slate-700"}">
        ${etiqueta(g)}
      </a>`),
  ].join("");

  const tarjetas = peliculas.map((p) => `
    <a href="#/pelicula/${p.id}"
       class="flex gap-3 rounded border border-slate-300 bg-white p-3 hover:border-slate-500">
      ${imagenPoster(p, "h-32 w-[5.5rem] shrink-0 rounded")}
      <div class="min-w-0">
        <h2 class="font-semibold leading-tight">${escapar(p.titulo)}</h2>
        <p class="mt-1 text-sm text-slate-500">${duracion(p.duracionMinutos)}</p>
        <div class="mt-2">${chipClasificacion(p.clasificacion)}</div>
        <div class="mt-2 flex flex-wrap gap-1">
          ${p.generos.map((g) => chip(etiqueta(g))).join("")}
        </div>
      </div>
    </a>
  `).join("");

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Cartelera</h1>
    <p class="mb-3 text-sm text-slate-500">
      ${peliculas.length} película${peliculas.length === 1 ? "" : "s"}
      ${generoFiltrado ? `de ${etiqueta(generoFiltrado).toLowerCase()}` : "en cartel"}
    </p>
    <div class="mb-5 flex flex-wrap gap-1">${filtros}</div>
    ${peliculas.length
      ? `<div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">${tarjetas}</div>`
      : '<p class="py-8 text-center text-slate-500">No hay películas de ese género en cartelera.</p>'}
  `;
}

/* --------------------------------------------------- detalle de una película */

async function vistaPelicula(contenedor, id) {
  const [pelicula, funciones] = await Promise.all([
    api.obtenerPelicula(id),
    api.obtenerFuncionesDePelicula(id),
  ]);

  const dias = porDia(funciones).map(([, delDia]) => `
    <section class="mb-5">
      <h3 class="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">
        ${escapar(dia(delDia[0].inicio))}
      </h3>
      <div class="grid gap-2 sm:grid-cols-2">
        ${delDia.map((f) => `
          <a href="#/funcion/${f.id}"
             class="flex items-center justify-between rounded border border-slate-300 bg-white px-3 py-2 hover:border-slate-500">
            <div>
              <p class="font-semibold">${hora(f.inicio)}</p>
              <p class="text-sm text-slate-600">
                ${escapar(f.sala.nombre)} · ${etiqueta(f.sala.tipo)}
              </p>
              <div class="mt-1 flex gap-1">
                ${chip(etiqueta(f.proyeccion), "bg-indigo-100 text-indigo-800")}
                ${chip(etiqueta(f.idioma), "bg-amber-100 text-amber-800")}
              </div>
            </div>
            <div class="text-right">
              <p class="text-xs text-slate-500">desde</p>
              <p class="font-semibold">${precio(f.precioDesde)}</p>
            </div>
          </a>
        `).join("")}
      </div>
    </section>
  `).join("");

  contenedor.innerHTML = `
    <a href="#/" class="text-sm text-slate-500 hover:text-slate-900">&larr; Cartelera</a>
    <div class="mt-2 mb-6 flex flex-wrap gap-4">
      ${imagenPoster(pelicula, "h-48 w-32 shrink-0 rounded")}
      <div class="min-w-64 flex-1">
        <h1 class="text-2xl font-bold">${escapar(pelicula.titulo)}</h1>
        <p class="mt-1 text-sm text-slate-500">
          ${[pelicula.anio || null, duracion(pelicula.duracionMinutos),
             pelicula.idiomaOriginal || null].filter(Boolean).map(escapar).join(" · ")}
        </p>
        ${pelicula.director
          ? `<p class="mt-1 text-sm text-slate-600">Dirección: ${escapar(pelicula.director)}</p>`
          : ""}
        <div class="mt-2">${chipClasificacion(pelicula.clasificacion)}</div>
        <div class="mt-2 flex flex-wrap gap-1">
          ${pelicula.generos.map((g) => chip(etiqueta(g))).join("")}
        </div>
        ${pelicula.sinopsis
          ? `<p class="mt-3 max-w-prose text-sm text-slate-700">${escapar(pelicula.sinopsis)}</p>`
          : ""}
      </div>
    </div>
    <h2 class="mb-3 text-lg font-semibold">Funciones</h2>
    ${funciones.length ? dias : '<p class="text-slate-500">No hay funciones programadas.</p>'}
  `;
}

/* ------------------------------------------------------- mapa de butacas */

// El fondo dice el estado; el borde y el símbolo, el tipo de butaca.
function pintarParaComprar(asiento, elegidas) {
  const titulo = `${asiento.codigo} · ${etiqueta(asiento.tipo)} · ${precio(asiento.precio)}`;
  if (asiento.estado === "FUERA_DE_SERVICIO") {
    return { clases: "bg-slate-300 text-slate-500 line-through cursor-not-allowed",
             deshabilitado: true, titulo: `${asiento.codigo} · fuera de servicio` };
  }
  if (asiento.ocupado) {
    return { clases: "bg-slate-600 text-white cursor-not-allowed",
             deshabilitado: true, titulo: `${asiento.codigo} · ocupada` };
  }
  if (elegidas.includes(asiento.codigo)) {
    return { clases: "bg-emerald-600 text-white border border-emerald-800",
             deshabilitado: false, titulo };
  }
  return { clases: `border ${CLASES_TIPO[asiento.tipo]} hover:bg-emerald-100`,
           deshabilitado: false, titulo };
}

async function vistaFuncion(contenedor, id) {
  const [funcion] = await Promise.all([api.obtenerFuncion(id), catalogoTarifas()]);
  if (seleccion.funcionId !== funcion.id) {
    seleccion.funcionId = funcion.id;
    seleccion.butacas = {};
  }

  contenedor.innerHTML = `
    <a href="#/pelicula/${funcion.peliculaId}" class="text-sm text-slate-500 hover:text-slate-900">
      &larr; ${escapar(funcion.pelicula.titulo)}
    </a>
    <h1 class="mt-2 text-2xl font-bold">${escapar(funcion.pelicula.titulo)}</h1>
    <p class="text-sm text-slate-600">
      ${escapar(dia(funcion.inicio))} ${hora(funcion.inicio)} ·
      ${escapar(funcion.sala.nombre)} (${etiqueta(funcion.sala.tipo)}) ·
      ${etiqueta(funcion.proyeccion)} · ${etiqueta(funcion.idioma)}
    </p>
    <p class="mt-1 text-sm text-slate-500">
      ${funcion.libres} butacas libres de ${funcion.sala.capacidadSala} ·
      precio base ${precio(funcion.precio)}
    </p>

    <div class="mt-5 overflow-x-auto rounded border border-slate-300 bg-white p-4">
      ${pantalla()}
      <div id="mapa" class="flex flex-col gap-1"></div>
    </div>

    ${referencia([
      ["border border-slate-400 bg-white", "libre"],
      ["bg-emerald-600", "elegida"],
      ["bg-slate-600", "ocupada"],
      ["bg-slate-300", "fuera de servicio"],
      [`border ${CLASES_TIPO.VIP}`, "* VIP"],
      [`border ${CLASES_TIPO.PAREJA}`, "&amp; pareja"],
      [`border ${CLASES_TIPO.ACCESIBLE}`, "+ accesible"],
    ])}

    <div id="resumen" class="sticky bottom-0 mt-4 rounded border border-slate-300 bg-white p-3"></div>
  `;

  const mapa = contenedor.querySelector("#mapa");
  const resumen = contenedor.querySelector("#resumen");

  function refrescar() {
    const codigos = Object.keys(seleccion.butacas);
    mapa.innerHTML = dibujarMapa(funcion.sala, funcion.asientos,
      (a) => pintarParaComprar(a, codigos));
    const elegidas = funcion.asientos.filter((a) => codigos.includes(a.codigo));
    const total = elegidas.reduce((suma, a) => suma + precioConTarifa(a, seleccion.butacas[a.codigo]), 0);

    // Cada butaca lleva su propia tarifa: quien compra elige acá y ve el precio cambiar,
    // en vez de enterarse del descuento recién en el ticket.
    const detalle = elegidas.map((a) => `
      <div class="flex items-center justify-between gap-2 border-t border-slate-200 py-1">
        <span class="font-medium">${a.codigo}</span>
        <span class="flex items-center gap-2">
          ${selectorTarifa(a.codigo, seleccion.butacas[a.codigo])}
          <span class="w-20 text-right">${precio(precioConTarifa(a, seleccion.butacas[a.codigo]))}</span>
        </span>
      </div>`).join("");

    resumen.innerHTML = `
      ${elegidas.length ? `<div class="mb-2 max-h-40 overflow-y-auto text-sm">${detalle}</div>` : ""}
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div class="text-sm">
          ${elegidas.length
            ? `<span class="font-semibold">${elegidas.length} butaca${elegidas.length > 1 ? "s" : ""}</span>
               <span class="ml-2 font-semibold">${precio(total)}</span>`
            : '<span class="text-slate-500">Elegí una o más butacas</span>'}
        </div>
        <button type="button" id="continuar" ${elegidas.length ? "" : "disabled"}
          class="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:bg-slate-300">
          Continuar
        </button>
      </div>`;
    resumen.querySelector("#continuar").addEventListener("click", () => ir(`#/confirmar/${funcion.id}`));
    resumen.querySelectorAll("select[data-tarifa-de]").forEach((select) => {
      select.addEventListener("change", () => {
        seleccion.butacas[select.dataset.tarifaDe] = select.value;
        refrescar();
      });
    });
  }

  mapa.addEventListener("click", (evento) => {
    const boton = evento.target.closest("button[data-codigo]");
    if (!boton || boton.disabled) return;
    const codigo = boton.dataset.codigo;
    // Arranca en GENERAL: la tarifa reducida hay que elegirla a propósito, porque
    // después hay que acreditarla en la puerta.
    if (seleccion.butacas[codigo]) delete seleccion.butacas[codigo];
    else seleccion.butacas[codigo] = "GENERAL";
    refrescar();
  });

  refrescar();
}

/* ------------------------------------------------------------- confirmación */

async function vistaConfirmar(contenedor, id) {
  const [funcion] = await Promise.all([api.obtenerFuncion(id), catalogoTarifas()]);
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
  <section id="resumenCompra" class="rounded border border-slate-300 bg-white p-4">
    <h2 class="mb-1 font-semibold">${escapar(funcion.pelicula.titulo)}</h2>
    <p class="mb-3 text-sm text-slate-600">
      ${escapar(dia(funcion.inicio))} ${hora(funcion.inicio)} ·
      ${escapar(funcion.sala.nombre)} (${etiqueta(funcion.sala.tipo)}) ·
      ${etiqueta(funcion.proyeccion)} · ${etiqueta(funcion.idioma)}
    </p>
    <table class="w-full text-sm">
      <thead>
        <tr class="border-b border-slate-300 text-left text-xs uppercase text-slate-500">
          <th class="py-1">Butaca</th><th>Tipo</th><th>Tarifa</th><th class="text-right">Precio</th>
        </tr>
      </thead>
      <tbody>
        ${elegidas.map((a) => `
          <tr class="border-b border-slate-200">
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
    <p class="mt-2 text-xs text-slate-500">
      Precio base ${precio(funcion.precio)} × sala ${etiqueta(funcion.sala.tipo)} ×
      tipo de butaca × tarifa.
    </p>
    <p class="mt-1 text-xs text-slate-500">
      Si hay promociones vigentes, el descuento se aplica al pagar: depende del medio
      de pago, así que el total definitivo aparece recién ahí.
    </p>
    ${aAcreditar().length ? `
      <p class="mt-3 rounded bg-amber-50 p-2 text-xs text-amber-900">
        <strong>Acordate del carnet.</strong> En la puerta te van a pedir que acredites
        la tarifa de ${escapar(aAcreditar().map((a) => `${a.codigo} (${etiqueta(seleccion.butacas[a.codigo]).toLowerCase()})`).join(", "))}.
      </p>` : ""}
  </section>
  `;

  contenedor.innerHTML = `
    <a href="#/funcion/${funcion.id}" class="text-sm text-slate-500 hover:text-slate-900">&larr; Cambiar butacas</a>
    <h1 class="mt-2 mb-5 text-2xl font-bold">Confirmar reserva</h1>

    <div class="grid gap-4 md:grid-cols-2">
      <section class="rounded border border-slate-300 bg-white p-4">
        <h2 class="mb-3 font-semibold">Tus datos</h2>
        <form id="datos" class="space-y-3">
          <label class="block text-sm">
            <span class="text-slate-600">Nombre</span>
            <input name="nombre" required value="${escapar(recordado?.nombre || "")}"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
          </label>
          <label class="block text-sm">
            <span class="text-slate-600">Email</span>
            <input name="email" type="email" required value="${escapar(recordado?.email || "")}"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
          </label>
          <button type="submit"
            class="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white">
            Confirmar reserva
          </button>
          <p id="errorForm" class="hidden text-sm text-red-700"></p>
          ${recordado ? "" : `
            <p class="text-xs text-slate-500">
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
}

/* --------------------------------------------------------------- mis reservas */

// El cliente no inicia sesión: recupera sus reservas con el email que dejó al comprar.
async function vistaMisReservas(contenedor, emailBuscado) {
  // Sin email en la URL, se usa el del navegador: quien ya compró no vuelve a tipearlo.
  const email = emailBuscado
    ? decodeURIComponent(emailBuscado)
    : (clienteRecordado()?.email || "");
  const reservas = email ? await api.obtenerReservasDe(email) : null;

  const tarjetas = (reservas || []).map((r) => `
    <article class="rounded border border-slate-300 bg-white p-4 ${r.estado === "CANCELADA" ? "opacity-60" : ""}">
      <div class="flex flex-wrap items-start justify-between gap-2">
        <div>
          <h2 class="font-semibold">${escapar(r.pelicula?.titulo || "—")}</h2>
          <p class="text-sm text-slate-600">
            ${r.funcion
              ? `${escapar(dia(r.funcion.inicio))} ${hora(r.funcion.inicio)} ·
                 ${escapar(r.sala.nombre)} (${etiqueta(r.sala.tipo)}) ·
                 ${etiqueta(r.funcion.proyeccion)} · ${etiqueta(r.funcion.idioma)}`
              : ""}
          </p>
        </div>
        <div class="text-right">
          ${chipEstado(r.estado)}
          <p class="mt-1 font-semibold">${precio(r.total)}</p>
        </div>
      </div>
      <p class="mt-2 text-sm">
        <span class="text-slate-500">Butacas:</span>
        <span class="font-mono">${r.entradas.map((e) => e.codigo).join(", ")}</span>
      </p>
      ${r.pago
        ? `<p class="mt-1 text-xs text-slate-500">
             Pagada con ${etiqueta(r.pago.medio)} el ${escapar(fechaHora(r.pago.fecha))}</p>`
        : ""}
      <div class="mt-3 flex gap-2">
        <a href="#/ticket/${r.id}" class="rounded border border-slate-400 px-3 py-1 text-sm">Ver ticket</a>
        ${r.estado === "RESERVADA"
          ? `<button type="button" data-cancelar="${r.id}"
               class="rounded border border-red-300 px-3 py-1 text-sm text-red-700">Cancelar</button>`
          : ""}
      </div>
    </article>
  `).join("");

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Mis reservas</h1>
    <p class="mb-5 text-sm text-slate-500">
      Buscá con el email que dejaste al comprar.
    </p>

    <form id="buscar" class="mb-5 flex flex-wrap gap-2">
      <input name="email" type="email" required value="${escapar(email)}"
        placeholder="tu@email.com"
        class="min-w-64 flex-1 rounded border border-slate-400 px-2 py-1.5" />
      <button type="submit" class="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white">
        Buscar
      </button>
    </form>

    ${reservas === null
      ? ""
      : reservas.length
        ? `<div class="space-y-3">${tarjetas}</div>`
        : `<p class="py-8 text-center text-slate-500">
             No hay reservas a nombre de ${escapar(email)}.</p>`}
  `;

  contenedor.querySelector("#buscar").addEventListener("submit", (evento) => {
    evento.preventDefault();
    const valor = new FormData(evento.target).get("email").trim();
    ir(`#/mis-reservas/${encodeURIComponent(valor)}`);
  });

  contenedor.addEventListener("click", async function alCancelar(evento) {
    const boton = evento.target.closest("button[data-cancelar]");
    if (!boton) return;
    contenedor.removeEventListener("click", alCancelar);
    try {
      await api.cancelarReserva(boton.dataset.cancelar);
      avisar("Reserva cancelada, las butacas quedaron libres");
    } catch (e) {
      avisar(e.message, "error");
    }
    vistaMisReservas(contenedor, emailBuscado);
  });
}

/* -------------------------------------------------------------------- ticket */

const LINEA = "=".repeat(44);

function campo(etiquetaTexto, valor) {
  return ` ${etiquetaTexto.padEnd(13)}: ${valor}`;
}

function centrar(texto) {
  return " ".repeat(Math.max(Math.floor((LINEA.length - texto.length) / 2), 0)) + texto;
}

/** Mismo contenido y formato que tickets/ticket-<id>.txt del backend. */
/**
 * El código de acceso, grande y separado en dos grupos de cuatro para poder leerlo de
 * un renglón. No es un QR dibujado: generarlo de verdad pide una librería, y el código
 * en claro cumple la misma función —el acomodador lo escanea o lo tipea— sin sumar una
 * dependencia al proyecto. Cuando exista la app del escáner, el QR se arma con esto.
 */
function tarjetaCodigo(reserva) {
  if (!reserva.codigo) return "";
  const legible = `${reserva.codigo.slice(0, 4)} ${reserva.codigo.slice(4)}`;
  const usada = reserva.ingresadaEn;
  return `
    <div class="mt-4 rounded border-2 ${usada ? "border-slate-300 bg-slate-100" : "border-slate-900 bg-white"} p-4 text-center">
      <p class="text-xs uppercase tracking-widest text-slate-500">Código de acceso</p>
      <p class="mt-1 font-mono text-3xl font-bold tracking-[0.2em] ${usada ? "text-slate-400 line-through" : ""}">
        ${escapar(legible)}
      </p>
      <p class="mt-2 text-xs text-slate-500">
        ${usada
          ? `Ya se usó el ${escapar(fechaHora(usada))}`
          : "Mostralo en la puerta. Sirve una sola vez."}
      </p>
    </div>`;
}

function armarTicket(reserva) {
  return [
    LINEA,
    centrar("CINE UADE"),
    centrar("TICKET #" + reserva.id),
    LINEA,
    campo("Pelicula", reserva.pelicula.titulo),
    campo("Sala", `${reserva.sala.nombre} (${reserva.sala.tipo})`),
    campo("Funcion", fechaHora(reserva.funcion.inicio)),
    campo("Formato", `${reserva.funcion.proyeccion} ${reserva.funcion.idioma}`),
    campo("Cliente", reserva.cliente.nombre),
    LINEA,
    ...reserva.entradas.map((e) => campo(
      "Butaca " + e.codigo,
      precioExacto(e.precio) + (e.tarifa && e.tarifa !== "GENERAL" ? "  " + e.tarifa : ""))),
    LINEA,
    campo("Entradas", String(reserva.entradas.length)),
    campo("Total", precioExacto(reserva.total)),
    campo("Estado", reserva.estado),
    LINEA,
    centrar("CODIGO DE ACCESO"),
    centrar(reserva.codigo || ""),
    LINEA,
    centrar("Presentar en boleteria"),
    LINEA,
  ].join("\n");
}

async function vistaTicket(contenedor, id) {
  const reserva = await api.obtenerReserva(id);
  const conAcreditacion = reserva.entradas.filter(
    (e) => e.tarifa && e.tarifa !== "GENERAL");

  contenedor.innerHTML = `
    <div class="rounded border border-emerald-300 bg-emerald-50 p-3 text-sm text-emerald-900">
      Reserva confirmada. Presentá este comprobante en boletería.
    </div>

    ${tarjetaCodigo(reserva)}

    ${conAcreditacion.length ? `
      <div class="mt-3 rounded border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
        <strong>Traé el carnet.</strong> En la puerta se acredita la tarifa de
        ${escapar(conAcreditacion.map((e) => `${e.codigo} (${etiqueta(e.tarifa).toLowerCase()})`).join(", "))}.
      </div>` : ""}

    <pre class="mt-4 overflow-x-auto rounded border border-slate-300 bg-white p-4 text-xs leading-5">${escapar(armarTicket(reserva))}</pre>
    <div class="mt-4 flex gap-2">
      <a href="#/" class="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white">Volver a la cartelera</a>
      <button type="button" id="copiar" class="rounded border border-slate-400 px-4 py-2 text-sm">Copiar</button>
    </div>
  `;

  contenedor.querySelector("#copiar").addEventListener("click", async () => {
    await navigator.clipboard.writeText(armarTicket(reserva));
    avisar("Comprobante copiado");
  });
}

/* ------------------------------------------------------------------- router */

iniciarRouter({
  contenedor: document.getElementById("app"),
  inicial: "cartelera",
  rutas: {
    cartelera: vistaCartelera,
    pelicula: vistaPelicula,
    funcion: vistaFuncion,
    confirmar: vistaConfirmar,
    ticket: vistaTicket,
    "mis-reservas": vistaMisReservas,
    registro: vistaRegistro,
  },
});
