// Vistas del cliente: cartelera, detalle de película, mapa de butacas,
// confirmación y ticket. Sin login.

import * as api from "./api.js";
import { iniciarRouter, ir } from "./router.js";
import {
  avisar, chip, dia, duracion, escapar, etiqueta, fechaHora,
  hora, porDia, precio, precioExacto,
} from "./ui.js";

// Lo elegido en el mapa de butacas, para que lo lea la confirmación.
const seleccion = { funcionId: null, codigos: [] };

/* ---------------------------------------------------------------- cartelera */

async function vistaCartelera(contenedor) {
  const peliculas = await api.obtenerCartelera();

  const tarjetas = peliculas.map((p) => `
    <a href="#/pelicula/${p.id}"
       class="block rounded border border-slate-300 bg-white p-4 hover:border-slate-500">
      <h2 class="font-semibold leading-tight">${escapar(p.titulo)}</h2>
      <p class="mt-1 text-sm text-slate-500">${duracion(p.duracionMinutos)}</p>
      <div class="mt-3 flex flex-wrap gap-1">
        ${p.generos.map((g) => chip(etiqueta(g))).join("")}
      </div>
    </a>
  `).join("");

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Cartelera</h1>
    <p class="mb-5 text-sm text-slate-500">${peliculas.length} películas en cartel</p>
    <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">${tarjetas}</div>
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
    <h1 class="mt-2 text-2xl font-bold">${escapar(pelicula.titulo)}</h1>
    <p class="mt-1 text-sm text-slate-500">${duracion(pelicula.duracionMinutos)}</p>
    <div class="mt-2 mb-6 flex flex-wrap gap-1">
      ${pelicula.generos.map((g) => chip(etiqueta(g))).join("")}
    </div>
    <h2 class="mb-3 text-lg font-semibold">Funciones</h2>
    ${funciones.length ? dias : '<p class="text-slate-500">No hay funciones programadas.</p>'}
  `;
}

/* ------------------------------------------------------- mapa de butacas */

// El fondo dice el estado; el borde y el símbolo, el tipo de butaca.
function clasesButaca(asiento, elegida) {
  if (asiento.estado === "FUERA_DE_SERVICIO") {
    return "bg-slate-300 text-slate-500 line-through cursor-not-allowed";
  }
  if (asiento.ocupado) return "bg-slate-600 text-white cursor-not-allowed";
  if (elegida) return "bg-emerald-600 text-white border border-emerald-800";
  const porTipo = {
    VIP: "border-amber-500 bg-amber-50 text-amber-900",
    PAREJA: "border-pink-500 bg-pink-50 text-pink-900",
    ACCESIBLE: "border-sky-500 bg-sky-50 text-sky-900",
    ESTANDAR: "border-slate-400 bg-white text-slate-700",
  };
  return `border ${porTipo[asiento.tipo]} hover:bg-emerald-100`;
}

const SIMBOLO = { VIP: "*", PAREJA: "&", ACCESIBLE: "+", ESTANDAR: "" };

function dibujarSala(funcion, elegidas) {
  const filas = [];
  for (let fila = 1; fila <= funcion.sala.filas; fila++) {
    const deLaFila = funcion.asientos.filter((a) => a.fila === fila);
    const letra = deLaFila[0]?.codigo.charAt(0) || "";
    const butacas = deLaFila.map((a) => {
      const elegida = elegidas.includes(a.codigo);
      const bloqueada = a.ocupado || a.estado === "FUERA_DE_SERVICIO";
      const ancho = a.tipo === "PAREJA" ? "w-12" : "w-7";
      return `
        <button type="button" data-codigo="${a.codigo}" ${bloqueada ? "disabled" : ""}
          title="${a.codigo} · ${etiqueta(a.tipo)} · ${precio(a.precio)}"
          class="h-7 ${ancho} shrink-0 rounded text-[10px] font-medium ${clasesButaca(a, elegida)}">
          ${a.numero}${SIMBOLO[a.tipo]}
        </button>`;
    }).join("");
    filas.push(`
      <div class="flex items-center justify-center gap-1">
        <span class="w-4 shrink-0 text-right text-xs font-semibold text-slate-500">${letra}</span>
        ${butacas}
      </div>`);
  }
  return filas.join("");
}

async function vistaFuncion(contenedor, id) {
  const funcion = await api.obtenerFuncion(id);
  if (seleccion.funcionId !== funcion.id) {
    seleccion.funcionId = funcion.id;
    seleccion.codigos = [];
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
      <div class="mb-4 rounded bg-slate-800 py-1 text-center text-xs tracking-[0.3em] text-white">
        PANTALLA
      </div>
      <div id="mapa" class="flex flex-col gap-1"></div>
    </div>

    <div class="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-600">
      <span><span class="mr-1 inline-block h-3 w-3 rounded border border-slate-400 bg-white align-middle"></span>libre</span>
      <span><span class="mr-1 inline-block h-3 w-3 rounded bg-emerald-600 align-middle"></span>elegida</span>
      <span><span class="mr-1 inline-block h-3 w-3 rounded bg-slate-600 align-middle"></span>ocupada</span>
      <span><span class="mr-1 inline-block h-3 w-3 rounded bg-slate-300 align-middle"></span>fuera de servicio</span>
      <span><span class="mr-1 inline-block h-3 w-3 rounded border border-amber-500 bg-amber-50 align-middle"></span>* VIP</span>
      <span><span class="mr-1 inline-block h-3 w-3 rounded border border-pink-500 bg-pink-50 align-middle"></span>&amp; pareja</span>
      <span><span class="mr-1 inline-block h-3 w-3 rounded border border-sky-500 bg-sky-50 align-middle"></span>+ accesible</span>
    </div>

    <div id="resumen" class="sticky bottom-0 mt-4 rounded border border-slate-300 bg-white p-3"></div>
  `;

  const mapa = contenedor.querySelector("#mapa");
  const resumen = contenedor.querySelector("#resumen");

  function refrescar() {
    mapa.innerHTML = dibujarSala(funcion, seleccion.codigos);
    const elegidas = funcion.asientos.filter((a) => seleccion.codigos.includes(a.codigo));
    const total = elegidas.reduce((suma, a) => suma + a.precio, 0);
    resumen.innerHTML = `
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div class="text-sm">
          ${elegidas.length
            ? `<span class="font-semibold">${elegidas.length} butaca${elegidas.length > 1 ? "s" : ""}:</span>
               ${escapar(elegidas.map((a) => a.codigo).join(", "))}
               <span class="ml-2 font-semibold">${precio(total)}</span>`
            : '<span class="text-slate-500">Elegí una o más butacas</span>'}
        </div>
        <button type="button" id="continuar" ${elegidas.length ? "" : "disabled"}
          class="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:bg-slate-300">
          Continuar
        </button>
      </div>`;
    resumen.querySelector("#continuar").addEventListener("click", () => ir(`#/confirmar/${funcion.id}`));
  }

  mapa.addEventListener("click", (evento) => {
    const boton = evento.target.closest("button[data-codigo]");
    if (!boton || boton.disabled) return;
    const codigo = boton.dataset.codigo;
    const indice = seleccion.codigos.indexOf(codigo);
    if (indice === -1) seleccion.codigos.push(codigo);
    else seleccion.codigos.splice(indice, 1);
    refrescar();
  });

  refrescar();
}

/* ------------------------------------------------------------- confirmación */

async function vistaConfirmar(contenedor, id) {
  const funcion = await api.obtenerFuncion(id);
  // Si se recargó la página la selección se perdió: volver al mapa.
  if (seleccion.funcionId !== funcion.id || seleccion.codigos.length === 0) {
    ir(`#/funcion/${funcion.id}`);
    return;
  }

  const elegidas = funcion.asientos.filter((a) => seleccion.codigos.includes(a.codigo));
  const total = elegidas.reduce((suma, a) => suma + a.precio, 0);

  contenedor.innerHTML = `
    <a href="#/funcion/${funcion.id}" class="text-sm text-slate-500 hover:text-slate-900">&larr; Cambiar butacas</a>
    <h1 class="mt-2 mb-5 text-2xl font-bold">Confirmar reserva</h1>

    <div class="grid gap-4 md:grid-cols-2">
      <section class="rounded border border-slate-300 bg-white p-4">
        <h2 class="mb-3 font-semibold">Tus datos</h2>
        <form id="datos" class="space-y-3">
          <label class="block text-sm">
            <span class="text-slate-600">Nombre</span>
            <input name="nombre" required
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
          </label>
          <label class="block text-sm">
            <span class="text-slate-600">Email</span>
            <input name="email" type="email" required
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
          </label>
          <button type="submit"
            class="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white">
            Confirmar reserva
          </button>
          <p id="errorForm" class="hidden text-sm text-red-700"></p>
        </form>
      </section>

      <section class="rounded border border-slate-300 bg-white p-4">
        <h2 class="mb-1 font-semibold">${escapar(funcion.pelicula.titulo)}</h2>
        <p class="mb-3 text-sm text-slate-600">
          ${escapar(dia(funcion.inicio))} ${hora(funcion.inicio)} ·
          ${escapar(funcion.sala.nombre)} (${etiqueta(funcion.sala.tipo)}) ·
          ${etiqueta(funcion.proyeccion)} · ${etiqueta(funcion.idioma)}
        </p>
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-slate-300 text-left text-xs uppercase text-slate-500">
              <th class="py-1">Butaca</th><th>Tipo</th><th class="text-right">Precio</th>
            </tr>
          </thead>
          <tbody>
            ${elegidas.map((a) => `
              <tr class="border-b border-slate-200">
                <td class="py-1 font-medium">${a.codigo}</td>
                <td>${etiqueta(a.tipo)}</td>
                <td class="text-right">${precio(a.precio)}</td>
              </tr>`).join("")}
          </tbody>
          <tfoot>
            <tr class="font-semibold">
              <td class="py-2" colspan="2">Total</td>
              <td class="py-2 text-right">${precio(total)}</td>
            </tr>
          </tfoot>
        </table>
        <p class="mt-2 text-xs text-slate-500">
          Precio base ${precio(funcion.precio)} × sala ${etiqueta(funcion.sala.tipo)} × tipo de butaca.
        </p>
      </section>
    </div>
  `;

  const formulario = contenedor.querySelector("#datos");
  const errorForm = contenedor.querySelector("#errorForm");
  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const datos = new FormData(formulario);
    try {
      const reserva = await api.crearReserva({
        funcionId: funcion.id,
        nombre: datos.get("nombre"),
        email: datos.get("email"),
        codigos: seleccion.codigos,
      });
      seleccion.funcionId = null;
      seleccion.codigos = [];
      ir(`#/ticket/${reserva.id}`);
    } catch (e) {
      errorForm.textContent = e.message;
      errorForm.classList.remove("hidden");
    }
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
    ...reserva.entradas.map((e) => campo("Butaca " + e.codigoAsiento, precioExacto(e.precio))),
    LINEA,
    campo("Entradas", String(reserva.entradas.length)),
    campo("Total", precioExacto(reserva.total)),
    campo("Estado", reserva.estado),
    LINEA,
    centrar("Presentar en boleteria"),
    LINEA,
  ].join("\n");
}

async function vistaTicket(contenedor, id) {
  const reserva = await api.obtenerReserva(id);

  contenedor.innerHTML = `
    <div class="rounded border border-emerald-300 bg-emerald-50 p-3 text-sm text-emerald-900">
      Reserva confirmada. Presentá este comprobante en boletería.
    </div>
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
  },
});
