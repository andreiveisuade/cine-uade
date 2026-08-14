import * as api from "../api.js";
import { CLASES_TIPO, dibujarMapa, pantalla, referencia } from "../butacas.js";
import { avisar, chip, escapar, etiqueta, error } from "../ui.js";

/* --------------------------------------------------------------- ABM de salas */

function parsearNumeros(texto) {
  return String(texto || "").split(",")
    .map((n) => Number(n.trim()))
    .filter((n) => !Number.isNaN(n));
}

function parsearCodigos(texto) {
  return String(texto || "").split(",")
    .map((c) => c.trim().toUpperCase())
    .filter(Boolean);
}

export async function vistaSalas(contenedor, id) {
  if (id) return vistaMapaSala(contenedor, id);

  const [salas, tipos] = await Promise.all([api.obtenerSalas(), api.obtenerTiposSala()]);

  contenedor.innerHTML = `
    <h1 class="mb-5 text-2xl font-bold">Salas</h1>

    <div class="grid gap-4 lg:grid-cols-[1fr_320px]">
      <section class="overflow-x-auto rounded border border-slate-300 bg-white dark:border-slate-700 dark:bg-slate-900">
        <table class="w-full text-sm">
          <thead class="border-b border-slate-300 bg-slate-50 text-left text-xs uppercase text-slate-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-400">
            <tr><th class="p-2">Sala</th><th>Tipo</th><th>Distribución</th><th>Butacas</th><th>Limpieza</th><th></th></tr>
          </thead>
          <tbody>
            ${salas.map((s) => `
              <tr class="border-b border-slate-200 dark:border-slate-800">
                <td class="p-2 font-medium">${escapar(s.nombre)}</td>
                <td>${chip(etiqueta(s.tipo), "bg-indigo-100 text-indigo-800 dark:bg-indigo-900/40 dark:text-indigo-300")}</td>
                <td class="font-mono text-xs">${s.butacasPorFila.join(",")}</td>
                <td>${s.capacidadSala}</td>
                <td class="whitespace-nowrap">${s.minutosLimpieza} min</td>
                <td class="p-2 text-right whitespace-nowrap">
                  <a href="#/salas/${s.id}" class="text-xs text-slate-700 hover:underline dark:text-slate-200">Butacas</a>
                  <button type="button" data-borrar="${s.id}"
                    class="ml-2 text-xs text-red-700 hover:underline dark:text-red-400">Borrar</button>
                </td>
              </tr>`).join("")}
          </tbody>
        </table>
      </section>

      <section class="rounded border border-slate-300 bg-white dark:border-slate-700 dark:bg-slate-900 p-4">
        <h2 class="mb-3 font-semibold">Nueva sala</h2>
        <form id="alta" class="space-y-3">
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Nombre</span>
            <input name="nombre" required class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
          </label>
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Tipo</span>
            <select name="tipo" class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500">
              ${tipos.map((t) => `<option value="${t.nombre}">${etiqueta(t.nombre)} (×${t.multiplicador})</option>`).join("")}
            </select>
          </label>
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Butacas por fila</span>
            <input name="distribucion" required placeholder="8,10,12,12,14"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 font-mono dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
            <span class="text-xs text-slate-500 dark:text-slate-400">Una fila por número. La primera es la A.</span>
          </label>
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Butacas VIP</span>
            <input name="vip" placeholder="I1,I2,J1"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 font-mono dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
          </label>
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Butacas de pareja</span>
            <input name="pareja" placeholder="A1,A2"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 font-mono dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
          </label>
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Butacas accesibles</span>
            <input name="accesibles" placeholder="A1,A8"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 font-mono dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
          </label>
          <label class="block text-sm">
            <span class="text-slate-600 dark:text-slate-300">Minutos de limpieza</span>
            <input name="limpieza" type="number" min="0" value="15"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:placeholder-slate-500" />
            <span class="mt-1 block text-xs text-slate-500 dark:text-slate-400">
              Lo que hay que esperar entre dos funciones. Una sala chica se levanta más rápido.
            </span>
          </label>
          <p id="previa" class="text-xs text-slate-500 dark:text-slate-400"></p>
          <button type="submit"
            class="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white dark:bg-white dark:text-slate-900">Crear sala</button>
        </form>
      </section>
    </div>
  `;

  const formulario = contenedor.querySelector("#alta");
  const previa = contenedor.querySelector("#previa");

  formulario.distribucion.addEventListener("input", () => {
    const filas = parsearNumeros(formulario.distribucion.value);
    previa.textContent = filas.length
      ? `${filas.length} filas (A–${String.fromCharCode(64 + filas.length)}), ${filas.reduce((a, b) => a + b, 0)} butacas`
      : "";
  });

  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const datos = new FormData(formulario);
    try {
      const sala = await api.crearSala({
        nombre: datos.get("nombre"),
        tipo: datos.get("tipo"),
        butacasPorFila: parsearNumeros(datos.get("distribucion")),
        codigosVip: parsearCodigos(datos.get("vip")),
        codigosPareja: parsearCodigos(datos.get("pareja")),
        codigosAccesibles: parsearCodigos(datos.get("accesibles")),
        minutosLimpieza: Number(datos.get("limpieza")),
      });
      avisar(`${sala.nombre} creada con ${sala.capacidadSala} butacas`);
      vistaSalas(contenedor);
    } catch (e) {
      avisar(e.message, "error");
    }
  });

  contenedor.querySelector("tbody").addEventListener("click", async (evento) => {
    const boton = evento.target.closest("button[data-borrar]");
    if (!boton) return;
    try {
      await api.eliminarSala(boton.dataset.borrar);
      avisar("Sala borrada");
      vistaSalas(contenedor);
    } catch (e) {
      avisar(e.message, "error");
    }
  });
}

/** Mapa de la sala para marcar y reponer butacas: acá no hay ocupación, es física. */
async function vistaMapaSala(contenedor, id) {
  const sala = await api.obtenerSala(id);
  const rotas = sala.asientos.filter((a) => a.estado === "FUERA_DE_SERVICIO");

  contenedor.innerHTML = `
    <a href="#/salas" class="text-sm text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-100">&larr; Salas</a>
    <h1 class="mt-2 text-2xl font-bold">${escapar(sala.nombre)}</h1>
    <p class="text-sm text-slate-600 dark:text-slate-300">
      ${etiqueta(sala.tipo)} · ${sala.filas} filas · ${sala.capacidadSala} butacas ·
      ${rotas.length} fuera de servicio
    </p>
    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
      Clic en una butaca para marcarla fuera de servicio o reponerla.
      Una butaca rota no se vende en ninguna función.
    </p>

    <div class="mt-5 overflow-x-auto rounded border border-slate-300 bg-white dark:border-slate-700 dark:bg-slate-900 p-4">
      ${pantalla()}
      <div id="mapa" class="flex flex-col gap-1"></div>
    </div>

    ${referencia([
      ["border border-slate-400 bg-white dark:border-slate-600 dark:bg-slate-800", "disponible"],
      ["bg-slate-300 dark:bg-slate-600", "fuera de servicio"],
      [`border ${CLASES_TIPO.VIP}`, "* VIP"],
      [`border ${CLASES_TIPO.PAREJA}`, "&amp; pareja"],
      [`border ${CLASES_TIPO.ACCESIBLE}`, "+ accesible"],
    ])}
  `;

  const mapa = contenedor.querySelector("#mapa");

  function pintar(asiento) {
    if (asiento.estado === "FUERA_DE_SERVICIO") {
      return {
        clases: "bg-slate-300 text-slate-500 line-through hover:bg-emerald-100 dark:bg-slate-600 dark:text-slate-400 dark:hover:bg-emerald-900/40",
        deshabilitado: false,
        titulo: `${asiento.codigo} · fuera de servicio · clic para reponer`,
      };
    }
    return {
      clases: `border ${CLASES_TIPO[asiento.tipo]} hover:bg-red-100 dark:hover:bg-red-900/40`,
      deshabilitado: false,
      titulo: `${asiento.codigo} · ${etiqueta(asiento.tipo)} · clic para marcar fuera de servicio`,
    };
  }

  mapa.innerHTML = dibujarMapa(sala, sala.asientos, pintar);

  mapa.addEventListener("click", async (evento) => {
    const boton = evento.target.closest("button[data-codigo]");
    if (!boton) return;
    const asiento = sala.asientos.find((a) => a.codigo === boton.dataset.codigo);
    const nuevo = asiento.estado === "FUERA_DE_SERVICIO" ? "HABILITADO" : "FUERA_DE_SERVICIO";
    try {
      await api.cambiarEstadoAsiento(sala.id, asiento.codigo, nuevo);
      avisar(`${asiento.codigo}: ${etiqueta(nuevo).toLowerCase()}`);
      vistaMapaSala(contenedor, id);
    } catch (e) {
      avisar(e.message, "error");
    }
  });
}
