import * as api from "../api.js";
import { boton, botonSecundario, campo, filaTabla, panel, select, tabla } from "../componentes.js";
import { avisar, escapar } from "../dom.js";
import { etiqueta } from "../etiquetas.js";
import { dia, hora, precio } from "../formato.js";

/* ------------------------------------------------------ programar funciones */

/**
 * @param destacada id de la función a la que saltar, si se llegó desde la agenda. La
 *                  tabla tiene ciento cincuenta filas: aterrizar arriba de todo y que el
 *                  usuario busque a mano la que acaba de clickear sería mandarlo dos
 *                  veces al mismo lugar.
 */
export async function vistaFunciones(contenedor, destacada) {
  const [funciones, peliculas, salas, tipos, idiomas, proyecciones] = await Promise.all([
    api.obtenerFunciones(), api.obtenerPeliculas(), api.obtenerSalas(),
    api.obtenerTiposSala(), api.obtenerIdiomas(), api.obtenerProyecciones(),
  ]);
  const soporta3D = new Map(tipos.map((t) => [t.nombre, t.soportaTresD]));

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Funciones</h1>
    <p class="mb-3 text-sm text-slate-500 dark:text-slate-400">
      ${funciones.length} programadas. Es la lista más larga del panel: una semana de seis salas
      pasa de cien funciones.
    </p>

    <div class="mb-4 flex flex-wrap items-end gap-3">
      ${select({ nombre: "fpelicula", etiqueta: "Película", ancho: "block", opciones: `
          <option value="">Todas</option>
          ${peliculas.map((p) => `<option value="${p.id}">${escapar(p.titulo)}</option>`).join("")}` })}
      ${select({ nombre: "fsala", etiqueta: "Sala", ancho: "block", opciones: `
          <option value="">Todas</option>
          ${salas.map((s) => `<option value="${s.id}">${escapar(s.nombre)}</option>`).join("")}` })}
      ${campo({ nombre: "fdesde", etiqueta: "Desde", tipo: "date", ancho: "block" })}
      ${campo({ nombre: "fhasta", etiqueta: "Hasta", tipo: "date", ancho: "block" })}
      ${botonSecundario("Limpiar", { atributos: 'id="limpiar"' })}
      <p id="cuenta" class="text-sm text-slate-500 dark:text-slate-400"></p>
    </div>

    <div class="grid gap-4 lg:grid-cols-[1fr_320px]">
      ${panel(tabla(
        '<tr><th class="p-2">Cuándo</th><th>Película</th><th>Sala</th><th>Formato</th><th class="text-right">Precio</th><th></th></tr>',
        filas(funciones)), "overflow-x-auto")}

      ${panel(`
        <h2 class="mb-3 font-semibold">Programar función</h2>
        <form id="alta" class="space-y-3">
          ${select({ nombre: "peliculaId", etiqueta: "Película",
            opciones: peliculas.map((p) => `<option value="${p.id}">${escapar(p.titulo)} (${p.duracionMinutos}′)</option>`).join("") })}
          ${select({ nombre: "salaId", etiqueta: "Sala",
            opciones: salas.map((s) => `<option value="${s.id}">${escapar(s.nombre)} — ${etiqueta(s.tipo)}</option>`).join("") })}
          ${campo({ nombre: "inicio", etiqueta: "Inicio", tipo: "datetime-local", requerido: true })}
          <div class="grid grid-cols-2 gap-2">
            ${select({ nombre: "idioma", etiqueta: "Idioma",
              opciones: idiomas.map((i) => `<option value="${i}">${etiqueta(i)}</option>`).join("") })}
            ${select({ nombre: "proyeccion", etiqueta: "Proyección",
              opciones: proyecciones.map((p) => `<option value="${p}">${etiqueta(p)}</option>`).join("") })}
          </div>
          ${campo({ nombre: "precio", etiqueta: "Precio base", tipo: "number", requerido: true, extra: 'min="100" step="100"' })}
          <div id="avisos" class="space-y-1 text-xs"></div>
          ${boton("Programar")}
        </form>
      `, "p-4")}
    </div>
  `;

  // --- filtros: el backend los resuelve, acá solo se juntan y se mandan ---
  const cuerpo = contenedor.querySelector("tbody");
  const cuenta = contenedor.querySelector("#cuenta");
  const controles = [...contenedor.querySelectorAll("[name^=f]")]
    .filter((c) => ["fpelicula", "fsala", "fdesde", "fhasta"].includes(c.name));

  async function aplicarFiltros() {
    const v = (nombre) => controles.find((c) => c.name === nombre).value;
    const visibles = await api.obtenerFunciones({
      peliculaId: v("fpelicula"), salaId: v("fsala"), desde: v("fdesde"), hasta: v("fhasta"),
    });
    cuerpo.innerHTML = filas(visibles);
    cuenta.textContent = visibles.length === funciones.length
      ? "" : `mostrando ${visibles.length} de ${funciones.length}`;
  }

  controles.forEach((c) => c.addEventListener("change", aplicarFiltros));
  contenedor.querySelector("#limpiar").addEventListener("click", () => {
    controles.forEach((c) => { c.value = ""; });
    aplicarFiltros();
  });

  const formulario = contenedor.querySelector("#alta");
  const avisos = contenedor.querySelector("#avisos");

  // R8 y R3 se validan en el backend; acá se anticipan para no mandar algo que va a fallar.
  function revisarReglas() {
    const sala = salas.find((s) => s.id === Number(formulario.salaId.value));
    const pelicula = peliculas.find((p) => p.id === Number(formulario.peliculaId.value));
    if (!sala || !pelicula) return;
    const mensajes = [];

    const opcion3D = [...formulario.proyeccion.options].find((o) => o.value === "TRES_D");
    const puede3D = soporta3D.get(sala?.tipo);
    opcion3D.disabled = !puede3D;
    if (!puede3D) {
      if (formulario.proyeccion.value === "TRES_D") formulario.proyeccion.value = "DOS_D";
      mensajes.push(["R8", `${sala.nombre} es ${etiqueta(sala.tipo)} y no puede proyectar en 3D.`]);
    }

    if (formulario.inicio.value && sala && pelicula) {
      const desde = new Date(formulario.inicio.value);
      const hasta = new Date(desde.getTime() + pelicula.duracionMinutos * 60000);
      const pisada = funciones.filter((f) => f.salaId === sala.id).find((f) => {
        const otraDesde = new Date(f.inicio);
        const otraHasta = new Date(otraDesde.getTime() + f.pelicula.duracionMinutos * 60000);
        return desde < otraHasta && otraDesde < hasta;
      });
      if (pisada) {
        mensajes.push(["R3", `Se pisa con ${pisada.pelicula.titulo} de las ${hora(pisada.inicio)}.`]);
      } else {
        mensajes.push(["ok", `Termina ${hora(hasta)} · sala libre en ese rango.`]);
      }
    }

    avisos.innerHTML = mensajes.map(([tipo, texto]) => {
      const clases = tipo === "ok"
        ? "border-emerald-300 bg-emerald-50 text-emerald-900 dark:border-emerald-800 dark:bg-emerald-950 dark:text-emerald-300"
        : "border-amber-300 bg-amber-50 text-amber-900 dark:border-amber-800 dark:bg-amber-950 dark:text-amber-300";
      const prefijo = tipo === "ok" ? "" : `<strong>${tipo}</strong> · `;
      return `<p class="rounded border px-2 py-1 ${clases}">${prefijo}${escapar(texto)}</p>`;
    }).join("");
  }

  formulario.addEventListener("change", revisarReglas);
  formulario.inicio.addEventListener("input", revisarReglas);
  revisarReglas();

  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const datos = new FormData(formulario);
    try {
      await api.programarFuncion({
        peliculaId: datos.get("peliculaId"),
        salaId: datos.get("salaId"),
        inicio: datos.get("inicio"),
        idioma: datos.get("idioma"),
        proyeccion: datos.get("proyeccion"),
        precio: Number(datos.get("precio")),
      });
      avisar("Función programada");
      vistaFunciones(contenedor);
    } catch (e) {
      avisar(e.message, "error");
    }
  });

  contenedor.querySelector("tbody").addEventListener("click", async (evento) => {
    const botonBorrar = evento.target.closest("button[data-borrar]");
    if (!botonBorrar) return;
    try {
      await api.eliminarFuncion(botonBorrar.dataset.borrar);
      avisar("Función borrada");
      vistaFunciones(contenedor);
    } catch (e) {
      avisar(e.message, "error");
    }
  });

  // El salto va al final, con la tabla ya pintada: antes de eso la fila no existe.
  const fila = destacada && contenedor.querySelector(`#funcion-${CSS.escape(String(destacada))}`);
  if (fila) {
    fila.scrollIntoView({ block: "center" });
    fila.classList.add("bg-amber-100", "dark:bg-amber-900/40");
  }
}

/** Una fila por función. Separada para repintar solo el cuerpo al filtrar. */
function filas(funciones) {
  if (!funciones.length) {
    return `<tr><td colspan="6" class="p-6 text-center text-sm text-slate-500 dark:text-slate-400">
      Ninguna función coincide con el filtro.
    </td></tr>`;
  }
  return funciones.map((f) => `
              <tr id="funcion-${f.id}" class="${filaTabla()}">
                <td class="p-2 whitespace-nowrap">
                  ${escapar(dia(f.inicio))} <span class="font-medium">${hora(f.inicio)}</span>
                </td>
                <td>${escapar(f.pelicula.titulo)}</td>
                <td class="whitespace-nowrap">${escapar(f.sala.nombre)}</td>
                <td class="whitespace-nowrap">${etiqueta(f.proyeccion)} · ${etiqueta(f.idioma)}</td>
                <td class="text-right whitespace-nowrap">${precio(f.precio)}</td>
                <td class="p-2 text-right whitespace-nowrap">
                  <a href="#/funcion/${f.id}"
                    class="text-xs font-medium text-slate-900 hover:underline dark:text-slate-100">Informes</a>
                  <button type="button" data-borrar="${f.id}"
                    class="ml-2 text-xs text-red-700 hover:underline dark:text-red-400">Borrar</button>
                </td>
              </tr>`).join("");
}
