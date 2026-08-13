import * as api from "../api.js";
import { CLASES_TIPO, dibujarMapa, pantalla, referencia } from "../butacas.js";
import { ir } from "../router.js";
import { dia, escapar, etiqueta, hora, precio } from "../ui.js";
import { seleccion, catalogoTarifas, precioConTarifa, selectorTarifa } from "./compra.js";

/* ------------------------------------------------------- mapa de butacas */

// El fondo dice el estado; el borde y el símbolo, el tipo de butaca.
function pintarParaComprar(asiento, elegidas) {
  const titulo = `${asiento.codigo} · ${etiqueta(asiento.tipo)} · ${precio(asiento.precio)}`;
  if (asiento.estado === "FUERA_DE_SERVICIO") {
    return { clases: "bg-slate-300 text-slate-500 line-through cursor-not-allowed dark:bg-slate-700 dark:text-slate-500",
             deshabilitado: true, titulo: `${asiento.codigo} · fuera de servicio` };
  }
  if (asiento.ocupado) {
    return { clases: "bg-slate-600 text-white cursor-not-allowed dark:bg-slate-500",
             deshabilitado: true, titulo: `${asiento.codigo} · ocupada` };
  }
  if (elegidas.includes(asiento.codigo)) {
    return { clases: "bg-emerald-600 text-white border border-emerald-800 dark:bg-emerald-500 dark:border-emerald-700",
             deshabilitado: false, titulo };
  }
  return { clases: `border ${CLASES_TIPO[asiento.tipo]} hover:bg-emerald-100 dark:hover:bg-emerald-900/40`,
           deshabilitado: false, titulo };
}

export async function vistaFuncion(contenedor, id) {
  const [funcion] = await Promise.all([api.obtenerFuncion(id), catalogoTarifas()]);
  if (seleccion.funcionId !== funcion.id) {
    seleccion.funcionId = funcion.id;
    seleccion.butacas = {};
  }

  contenedor.innerHTML = `
    <a href="#/pelicula/${funcion.peliculaId}" class="text-sm text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-100">
      &larr; ${escapar(funcion.pelicula.titulo)}
    </a>
    <h1 class="mt-2 text-2xl font-bold">${escapar(funcion.pelicula.titulo)}</h1>
    <p class="text-sm text-slate-600 dark:text-slate-300">
      ${escapar(dia(funcion.inicio))} ${hora(funcion.inicio)} ·
      ${escapar(funcion.sala.nombre)} (${etiqueta(funcion.sala.tipo)}) ·
      ${etiqueta(funcion.proyeccion)} · ${etiqueta(funcion.idioma)}
    </p>
    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
      ${funcion.libres} butacas libres de ${funcion.sala.capacidadSala} ·
      precio base ${precio(funcion.precio)}
    </p>

    <div class="mt-5 overflow-x-auto rounded border border-slate-300 bg-white p-4 dark:border-slate-700 dark:bg-slate-900">
      ${pantalla()}
      <div id="mapa" class="flex flex-col gap-1"></div>
    </div>

    ${referencia([
      ["border border-slate-400 bg-white dark:border-slate-600 dark:bg-slate-800", "libre"],
      ["bg-emerald-600 dark:bg-emerald-500", "elegida"],
      ["bg-slate-600 dark:bg-slate-500", "ocupada"],
      ["bg-slate-300 dark:bg-slate-700", "fuera de servicio"],
      [`border ${CLASES_TIPO.VIP}`, "* VIP"],
      [`border ${CLASES_TIPO.PAREJA}`, "&amp; pareja"],
      [`border ${CLASES_TIPO.ACCESIBLE}`, "+ accesible"],
    ])}

    <div id="resumen" class="sticky bottom-0 mt-4 rounded border border-slate-300 bg-white p-3 dark:border-slate-700 dark:bg-slate-900"></div>
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
      <div class="flex items-center justify-between gap-2 border-t border-slate-200 py-1 dark:border-slate-800">
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
            : '<span class="text-slate-500 dark:text-slate-400">Elegí una o más butacas</span>'}
        </div>
        <button type="button" id="continuar" ${elegidas.length ? "" : "disabled"}
          class="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:bg-slate-300 dark:bg-white dark:text-slate-900 dark:disabled:bg-slate-700 dark:disabled:text-slate-400">
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
