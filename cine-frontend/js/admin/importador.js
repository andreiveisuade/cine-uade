import * as api from "../api.js";
import { boton, fantasma, filaTabla, panel, select, spinner, tabla } from "../componentes.js";
import { avisar, escapar } from "../dom.js";
import { chipEstado } from "../etiquetas.js";
import { fechaHora } from "../formato.js";

export async function vistaImportador(contenedor, destacada) {
  const [corridas, estado] = await Promise.all([
    api.obtenerImportaciones(),
    api.estadoImportador(),
  ]);

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Importador</h1>
    <p class="mb-5 max-w-2xl text-sm text-slate-500 dark:text-slate-400">
      Trae de TMDB las películas que están hoy en cartelera en Argentina. Nada se publica:
      todo cae en <a href="#/pendientes" class="underline">Por revisar</a> y espera que
      alguien lo confirme.
    </p>
    ${avisoDelImportador(estado)}
    ${panel(`
      <div class="flex flex-wrap items-end gap-4 p-4">
        ${select({ nombre: "paginas", etiqueta: "Cuánto traer", ancho: "w-56",
                   opciones: OPCIONES_PAGINAS })}
        <div>
          ${boton("Traer cartelera", { tipo: "button", ancho: "", atributos:
                    `id="importar" ${estado.disponible ? "" : "disabled"}` })}
        </div>
      </div>
    `, "mb-5")}
    <div id="resultado" class="mb-5"></div>
    <h2 class="mb-2 text-sm font-semibold uppercase text-slate-500 dark:text-slate-400">
      Corridas anteriores
    </h2>
    ${corridas.length === 0 ? vacio() : historial(corridas, destacada)}
  `;

  contenedor.querySelector("#importar").addEventListener("click", () => traer(contenedor));
}

async function traer(contenedor) {
  const botonImportar = contenedor.querySelector("#importar");
  const paginas = contenedor.querySelector("#paginas").value;
  botonImportar.disabled = true;
  botonImportar.innerHTML = `<span class="inline-flex items-center justify-center gap-2">
    ${spinner("h-4 w-4")}Trayendo…</span>`;
  contenedor.querySelector("#resultado").innerHTML = esqueleto();

  try {
    const corrida = await api.importarAhora(paginas);
    avisar(resumen(corrida), corrida.estado === "FALLIDA" ? "error" : "ok");
    await vistaImportador(contenedor, corrida.id);
  } catch (e) {
    avisar(e.message, "error");
    contenedor.querySelector("#resultado").innerHTML = "";
    botonImportar.disabled = false;
    botonImportar.textContent = "Traer cartelera";
  }
}

const OPCIONES_PAGINAS = `
  <option value="1">Una página (20 títulos)</option>
  <option value="2">Dos páginas (40 títulos)</option>
  <option value="3">Tres páginas (60 títulos)</option>
`;

function avisoDelImportador(estado) {
  if (estado.disponible) return "";
  return `
    <div class="mb-5 rounded border border-amber-300 bg-amber-50 p-4 text-sm text-amber-900
                dark:border-amber-800 dark:bg-amber-950 dark:text-amber-200">
      <p class="font-medium">El importador no está disponible</p>
      <p class="mt-1">${escapar(estado.detalle)}</p>
    </div>`;
}

function resumen(corrida) {
  if (corrida.estado === "FALLIDA") return corrida.detalle || "La importación falló";
  if (corrida.nuevas === 0) return "No había nada nuevo en TMDB";
  return `${corrida.nuevas} película${corrida.nuevas === 1 ? "" : "s"} nueva${
    corrida.nuevas === 1 ? "" : "s"} en Por revisar`;
}

function esqueleto() {
  return panel(`
    <div class="space-y-3 p-4">
      <p class="text-sm text-slate-500 dark:text-slate-400">
        Preguntándole a TMDB qué se está dando, y cargando lo que falte. Son unos segundos.
      </p>
      ${fantasma("h-4 w-2/3")}
      ${fantasma("h-4 w-1/2")}
      ${fantasma("h-4 w-3/5")}
    </div>
  `);
}

function vacio() {
  return panel(`
    <p class="text-sm text-slate-500 dark:text-slate-400">
      Todavía no se pidió ninguna importación.
    </p>
  `, "p-8 text-center");
}

function historial(corridas, destacada) {
  return panel(tabla(`
    <tr>
      <th class="px-3 py-2">Cuándo</th>
      <th class="px-3 py-2">Estado</th>
      <th class="px-3 py-2 text-right">Nuevas</th>
      <th class="px-3 py-2 text-right">Salteadas</th>
      <th class="px-3 py-2 text-right">Fallidas</th>
      <th class="px-3 py-2">Detalle</th>
    </tr>`,
    corridas.map((corrida) => fila(corrida, corrida.id === destacada)).join("")),
    "overflow-x-auto");
}

function fila(corrida, destacada) {
  return `
    <tr class="${filaTabla(destacada ? "bg-slate-50 dark:bg-slate-800" : "")}">
      <td class="px-3 py-2 whitespace-nowrap">${fechaHora(corrida.pedidaEn)}</td>
      <td class="px-3 py-2">${chipEstado(corrida.estado)}</td>
      <td class="px-3 py-2 text-right font-medium">${corrida.nuevas}</td>
      <td class="px-3 py-2 text-right text-slate-500 dark:text-slate-400">${corrida.salteadas}</td>
      <td class="px-3 py-2 text-right ${corrida.fallidas > 0 ? "text-red-700 dark:text-red-400" : "text-slate-500 dark:text-slate-400"}">${corrida.fallidas}</td>
      <td class="px-3 py-2">${detalle(corrida, destacada)}</td>
    </tr>`;
}

function detalle(corrida, abierto) {
  if (!corrida.detalle) return `<span class="text-slate-400">—</span>`;
  return `
    <details ${abierto ? "open" : ""}>
      <summary class="cursor-pointer text-xs text-slate-500 dark:text-slate-400">ver</summary>
      <pre class="mt-1 max-h-64 overflow-auto whitespace-pre-wrap text-xs text-slate-600 dark:text-slate-300">${escapar(corrida.detalle)}</pre>
    </details>`;
}

