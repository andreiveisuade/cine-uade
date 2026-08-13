// Vistas del encargado: login, ABM de películas, ABM de salas con butacas fuera de
// servicio, programación de funciones y listado de reservas.

import * as api from "./api.js";
import { CLASES_TIPO, dibujarMapa, pantalla, referencia } from "./butacas.js";
import { iniciarRouter, ir } from "./router.js";
import {
  avisar, chip, chipClasificacion, dia, duracion, escapar, etiqueta,
  fechaHora, hora, imagenPoster, precio,
} from "./ui.js";

/* ------------------------------------------------------------------- sesión */

const CLAVE_SESION = "cine.sesion";

function sesionActual() {
  const guardada = sessionStorage.getItem(CLAVE_SESION);
  return guardada ? JSON.parse(guardada) : null;
}

function abrirSesion(admin) {
  sessionStorage.setItem(CLAVE_SESION, JSON.stringify(admin));
}

function cerrarSesion() {
  sessionStorage.removeItem(CLAVE_SESION);
  pintarEncabezado();
  ir("#/login");
}

function pintarEncabezado() {
  const sesion = sesionActual();
  document.getElementById("nav").classList.toggle("hidden", !sesion);
  document.getElementById("nav").classList.toggle("flex", !!sesion);
  document.getElementById("salir").classList.toggle("hidden", !sesion);
  document.getElementById("sesion").textContent = sesion ? sesion.nombre : "";
}

/* -------------------------------------------------------------------- login */

async function vistaLogin(contenedor) {
  contenedor.innerHTML = `
    <div class="mx-auto max-w-sm rounded border border-slate-300 bg-white p-5">
      <h1 class="mb-4 text-xl font-bold">Ingresar</h1>
      <form id="login" class="space-y-3">
        <label class="block text-sm">
          <span class="text-slate-600">Email</span>
          <input name="email" type="email" required autocomplete="username"
            class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
        </label>
        <label class="block text-sm">
          <span class="text-slate-600">Contraseña</span>
          <input name="password" type="password" required autocomplete="current-password"
            class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
        </label>
        <button type="submit"
          class="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white">Entrar</button>
        <p id="errorLogin" class="hidden text-sm text-red-700"></p>
      </form>
      <p class="mt-4 border-t border-slate-200 pt-3 text-xs text-slate-500">
        Datos de prueba: <code>encargado@cine.uade.ar</code> / <code>cine2026</code>
      </p>
    </div>
  `;

  const formulario = contenedor.querySelector("#login");
  const errorLogin = contenedor.querySelector("#errorLogin");
  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const datos = new FormData(formulario);
    try {
      abrirSesion(await api.login(datos.get("email"), datos.get("password")));
      pintarEncabezado();
      ir("#/peliculas");
    } catch (e) {
      errorLogin.textContent = e.message;
      errorLogin.classList.remove("hidden");
    }
  });
}

/* ---------------------------------------------------------- ABM de películas */

// El mismo formulario sirve para alta y edición: null es alta, un id es edición.
async function vistaPeliculas(contenedor, editandoId = null) {
  const [peliculas, generos, clasificaciones] = await Promise.all([
    api.obtenerPeliculas(),
    api.obtenerGeneros(),
    api.obtenerClasificaciones(),
  ]);
  const editando = editandoId
    ? peliculas.find((p) => p.id === Number(editandoId))
    : null;
  const valor = (campo) => escapar(editando?.[campo] ?? "");

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Películas</h1>
    <p class="mb-5 text-sm text-slate-500">
      ${peliculas.length} cargadas · ${peliculas.filter((p) => p.enCartelera).length} en cartelera
    </p>

    <div class="grid gap-4 lg:grid-cols-[1fr_340px]">
      <section class="overflow-x-auto rounded border border-slate-300 bg-white">
        <table class="w-full text-sm">
          <thead class="border-b border-slate-300 bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr><th class="p-2"></th><th>Título</th><th>Duración</th><th>Edad</th><th>Géneros</th><th>Estado</th><th></th></tr>
          </thead>
          <tbody>
            ${peliculas.map((p) => `
              <tr class="border-b border-slate-200 ${p.id === editando?.id ? "bg-amber-50" : ""}">
                <td class="p-2">${imagenPoster(p, "h-12 w-8 rounded")}</td>
                <td>
                  <span class="font-medium">${escapar(p.titulo)}</span>
                  <span class="block text-xs text-slate-500">
                    ${[p.anio || null, p.director || null].filter(Boolean).map(escapar).join(" · ")}
                  </span>
                </td>
                <td class="whitespace-nowrap">${duracion(p.duracionMinutos)}</td>
                <td>${chipClasificacion(p.clasificacion)}</td>
                <td class="py-1">
                  <div class="flex flex-wrap gap-1">${p.generos.map((g) => chip(etiqueta(g))).join("")}</div>
                </td>
                <td>
                  <button type="button" data-cartelera="${p.id}" title="Clic para cambiar"
                    class="rounded-full px-2 py-0.5 text-xs font-medium ${p.enCartelera
                      ? "bg-emerald-100 text-emerald-800" : "bg-slate-200 text-slate-600"}">
                    ${p.enCartelera ? "En cartelera" : "Fuera"}
                  </button>
                </td>
                <td class="p-2 text-right whitespace-nowrap">
                  <button type="button" data-editar="${p.id}"
                    class="text-xs text-slate-700 hover:underline">Editar</button>
                  <button type="button" data-borrar="${p.id}"
                    class="ml-2 text-xs text-red-700 hover:underline">Borrar</button>
                </td>
              </tr>`).join("")}
          </tbody>
        </table>
      </section>

      <section class="rounded border border-slate-300 bg-white p-4">
        <h2 class="mb-3 font-semibold">
          ${editando ? `Editar ${escapar(editando.titulo)}` : "Nueva película"}
        </h2>
        <form id="alta" class="space-y-3">
          <label class="block text-sm">
            <span class="text-slate-600">Título</span>
            <input name="titulo" required value="${valor("titulo")}"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
          </label>
          <div class="grid grid-cols-2 gap-2">
            <label class="block text-sm">
              <span class="text-slate-600">Duración (min)</span>
              <input name="duracion" type="number" min="1" required
                value="${editando ? editando.duracionMinutos : ""}"
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
            </label>
            <label class="block text-sm">
              <span class="text-slate-600">Año</span>
              <input name="anio" type="number" min="1888" value="${editando?.anio || ""}"
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
            </label>
          </div>
          <label class="block text-sm">
            <span class="text-slate-600">Clasificación</span>
            <select name="clasificacion" class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5">
              ${clasificaciones.map((c) => `
                <option value="${c.nombre}" ${c.nombre === editando?.clasificacion ? "selected" : ""}>
                  ${etiqueta(c.nombre)}${c.edadMinima ? ` — desde ${c.edadMinima} años` : " — todo público"}
                </option>`).join("")}
            </select>
          </label>
          <label class="block text-sm">
            <span class="text-slate-600">Dirección</span>
            <input name="director" value="${valor("director")}"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
          </label>
          <label class="block text-sm">
            <span class="text-slate-600">Idioma original</span>
            <input name="idiomaOriginal" value="${valor("idiomaOriginal")}"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
            <span class="text-xs text-slate-500">El de la película, no el de la función.</span>
          </label>
          <label class="block text-sm">
            <span class="text-slate-600">Sinopsis</span>
            <textarea name="sinopsis" rows="3"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5">${valor("sinopsis")}</textarea>
          </label>
          <label class="block text-sm">
            <span class="text-slate-600">Poster (URL)</span>
            <input name="posterUrl" value="${valor("posterUrl")}" placeholder="https://…"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
            <span class="text-xs text-slate-500">Opcional. Sin poster se muestra la inicial del título.</span>
          </label>
          <fieldset class="text-sm">
            <legend class="text-slate-600">Géneros (al menos uno)</legend>
            <div class="mt-1 grid grid-cols-2 gap-1">
              ${generos.map((g) => `
                <label class="flex items-center gap-1 text-xs">
                  <input type="checkbox" name="genero" value="${g}"
                    ${editando?.generos.includes(g) ? "checked" : ""} /> ${etiqueta(g)}
                </label>`).join("")}
            </div>
          </fieldset>
          <label class="flex items-center gap-2 text-sm">
            <input type="checkbox" name="enCartelera" ${!editando || editando.enCartelera ? "checked" : ""} />
            <span class="text-slate-600">En cartelera</span>
          </label>
          <div class="flex gap-2">
            <button type="submit"
              class="flex-1 rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white">
              ${editando ? "Guardar cambios" : "Agregar"}
            </button>
            ${editando
              ? `<button type="button" id="cancelar"
                   class="rounded border border-slate-400 px-4 py-2 text-sm">Cancelar</button>`
              : ""}
          </div>
        </form>
      </section>
    </div>
  `;

  const formulario = contenedor.querySelector("#alta");
  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const datos = new FormData(formulario);
    const campos = {
      titulo: datos.get("titulo"),
      duracionMinutos: Number(datos.get("duracion")),
      generos: datos.getAll("genero"),
      clasificacion: datos.get("clasificacion"),
      posterUrl: datos.get("posterUrl"),
      director: datos.get("director"),
      anio: datos.get("anio"),
      idiomaOriginal: datos.get("idiomaOriginal"),
      sinopsis: datos.get("sinopsis"),
      enCartelera: datos.get("enCartelera") !== null,
    };
    try {
      if (editando) {
        await api.actualizarPelicula(editando.id, campos);
        avisar("Cambios guardados");
      } else {
        await api.crearPelicula(campos);
        avisar("Película agregada");
      }
      vistaPeliculas(contenedor);
    } catch (e) {
      avisar(e.message, "error");
    }
  });

  contenedor.querySelector("#cancelar")?.addEventListener("click", () => vistaPeliculas(contenedor));

  // El listener va en el tbody, que se reemplaza en cada render: colgarlo del
  // contenedor lo acumularía una vez por refresco.
  contenedor.querySelector("tbody").addEventListener("click", async (evento) => {
    const editar = evento.target.closest("button[data-editar]");
    if (editar) return vistaPeliculas(contenedor, editar.dataset.editar);

    const alternar = evento.target.closest("button[data-cartelera]");
    if (alternar) {
      const pelicula = peliculas.find((p) => p.id === Number(alternar.dataset.cartelera));
      try {
        await api.actualizarPelicula(pelicula.id, { enCartelera: !pelicula.enCartelera });
        avisar(pelicula.enCartelera ? "Sacada de cartelera" : "Puesta en cartelera");
        vistaPeliculas(contenedor, editandoId);
      } catch (e) {
        avisar(e.message, "error");
      }
      return;
    }

    const borrar = evento.target.closest("button[data-borrar]");
    if (!borrar) return;
    try {
      await api.eliminarPelicula(borrar.dataset.borrar);
      avisar("Película borrada");
      vistaPeliculas(contenedor);
    } catch (e) {
      avisar(e.message, "error");
    }
  });
}

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

async function vistaSalas(contenedor, id) {
  if (id) return vistaMapaSala(contenedor, id);

  const [salas, tipos] = await Promise.all([api.obtenerSalas(), api.obtenerTiposSala()]);

  contenedor.innerHTML = `
    <h1 class="mb-5 text-2xl font-bold">Salas</h1>

    <div class="grid gap-4 lg:grid-cols-[1fr_320px]">
      <section class="overflow-x-auto rounded border border-slate-300 bg-white">
        <table class="w-full text-sm">
          <thead class="border-b border-slate-300 bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr><th class="p-2">Sala</th><th>Tipo</th><th>Distribución</th><th>Butacas</th><th></th></tr>
          </thead>
          <tbody>
            ${salas.map((s) => `
              <tr class="border-b border-slate-200">
                <td class="p-2 font-medium">${escapar(s.nombre)}</td>
                <td>${chip(etiqueta(s.tipo), "bg-indigo-100 text-indigo-800")}</td>
                <td class="font-mono text-xs">${s.butacasPorFila.join(",")}</td>
                <td>${s.capacidadSala}</td>
                <td class="p-2 text-right whitespace-nowrap">
                  <a href="#/salas/${s.id}" class="text-xs text-slate-700 hover:underline">Butacas</a>
                  <button type="button" data-borrar="${s.id}"
                    class="ml-2 text-xs text-red-700 hover:underline">Borrar</button>
                </td>
              </tr>`).join("")}
          </tbody>
        </table>
      </section>

      <section class="rounded border border-slate-300 bg-white p-4">
        <h2 class="mb-3 font-semibold">Nueva sala</h2>
        <form id="alta" class="space-y-3">
          <label class="block text-sm">
            <span class="text-slate-600">Nombre</span>
            <input name="nombre" required class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
          </label>
          <label class="block text-sm">
            <span class="text-slate-600">Tipo</span>
            <select name="tipo" class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5">
              ${tipos.map((t) => `<option value="${t.nombre}">${etiqueta(t.nombre)} (×${t.multiplicador})</option>`).join("")}
            </select>
          </label>
          <label class="block text-sm">
            <span class="text-slate-600">Butacas por fila</span>
            <input name="distribucion" required placeholder="8,10,12,12,14"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 font-mono" />
            <span class="text-xs text-slate-500">Una fila por número. La primera es la A.</span>
          </label>
          <label class="block text-sm">
            <span class="text-slate-600">Butacas premium</span>
            <input name="vip" placeholder="I1,I2,J1"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 font-mono" />
            <span class="text-xs text-slate-500">VIP, o de pareja si la sala es VIP.</span>
          </label>
          <label class="block text-sm">
            <span class="text-slate-600">Butacas accesibles</span>
            <input name="accesibles" placeholder="A1,A8"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 font-mono" />
          </label>
          <p id="previa" class="text-xs text-slate-500"></p>
          <button type="submit"
            class="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white">Crear sala</button>
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
        codigosAccesibles: parsearCodigos(datos.get("accesibles")),
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
    <a href="#/salas" class="text-sm text-slate-500 hover:text-slate-900">&larr; Salas</a>
    <h1 class="mt-2 text-2xl font-bold">${escapar(sala.nombre)}</h1>
    <p class="text-sm text-slate-600">
      ${etiqueta(sala.tipo)} · ${sala.filas} filas · ${sala.capacidadSala} butacas ·
      ${rotas.length} fuera de servicio
    </p>
    <p class="mt-1 text-sm text-slate-500">
      Clic en una butaca para marcarla fuera de servicio o reponerla.
      Una butaca rota no se vende en ninguna función.
    </p>

    <div class="mt-5 overflow-x-auto rounded border border-slate-300 bg-white p-4">
      ${pantalla()}
      <div id="mapa" class="flex flex-col gap-1"></div>
    </div>

    ${referencia([
      ["border border-slate-400 bg-white", "disponible"],
      ["bg-slate-300", "fuera de servicio"],
      [`border ${CLASES_TIPO.VIP}`, "* VIP"],
      [`border ${CLASES_TIPO.PAREJA}`, "&amp; pareja"],
      [`border ${CLASES_TIPO.ACCESIBLE}`, "+ accesible"],
    ])}
  `;

  const mapa = contenedor.querySelector("#mapa");

  function pintar(asiento) {
    if (asiento.estado === "FUERA_DE_SERVICIO") {
      return {
        clases: "bg-slate-300 text-slate-500 line-through hover:bg-emerald-100",
        deshabilitado: false,
        titulo: `${asiento.codigo} · fuera de servicio · clic para reponer`,
      };
    }
    return {
      clases: `border ${CLASES_TIPO[asiento.tipo]} hover:bg-red-100`,
      deshabilitado: false,
      titulo: `${asiento.codigo} · ${etiqueta(asiento.tipo)} · clic para marcar fuera de servicio`,
    };
  }

  mapa.innerHTML = dibujarMapa(sala, sala.asientos, pintar);

  mapa.addEventListener("click", async (evento) => {
    const boton = evento.target.closest("button[data-codigo]");
    if (!boton) return;
    const asiento = sala.asientos.find((a) => a.codigo === boton.dataset.codigo);
    const nuevo = asiento.estado === "FUERA_DE_SERVICIO" ? "DISPONIBLE" : "FUERA_DE_SERVICIO";
    try {
      await api.cambiarEstadoAsiento(sala.id, asiento.codigo, nuevo);
      avisar(`${asiento.codigo}: ${etiqueta(nuevo).toLowerCase()}`);
      vistaMapaSala(contenedor, id);
    } catch (e) {
      avisar(e.message, "error");
    }
  });
}

/* ------------------------------------------------------ programar funciones */

async function vistaFunciones(contenedor) {
  const [funciones, peliculas, salas, tipos, idiomas, proyecciones] = await Promise.all([
    api.obtenerFunciones(), api.obtenerPeliculas(), api.obtenerSalas(),
    api.obtenerTiposSala(), api.obtenerIdiomas(), api.obtenerProyecciones(),
  ]);
  const soporta3D = new Map(tipos.map((t) => [t.nombre, t.soportaTresD]));

  contenedor.innerHTML = `
    <h1 class="mb-5 text-2xl font-bold">Funciones</h1>

    <div class="grid gap-4 lg:grid-cols-[1fr_320px]">
      <section class="overflow-x-auto rounded border border-slate-300 bg-white">
        <table class="w-full text-sm">
          <thead class="border-b border-slate-300 bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr><th class="p-2">Cuándo</th><th>Película</th><th>Sala</th><th>Formato</th><th class="text-right">Precio</th><th></th></tr>
          </thead>
          <tbody>
            ${funciones.map((f) => `
              <tr class="border-b border-slate-200">
                <td class="p-2 whitespace-nowrap">
                  ${escapar(dia(f.inicio))} <span class="font-medium">${hora(f.inicio)}</span>
                </td>
                <td>${escapar(f.pelicula.titulo)}</td>
                <td class="whitespace-nowrap">${escapar(f.sala.nombre)}</td>
                <td class="whitespace-nowrap">${etiqueta(f.proyeccion)} · ${etiqueta(f.idioma)}</td>
                <td class="text-right whitespace-nowrap">${precio(f.precio)}</td>
                <td class="p-2 text-right">
                  <button type="button" data-borrar="${f.id}"
                    class="text-xs text-red-700 hover:underline">Borrar</button>
                </td>
              </tr>`).join("")}
          </tbody>
        </table>
      </section>

      <section class="rounded border border-slate-300 bg-white p-4">
        <h2 class="mb-3 font-semibold">Programar función</h2>
        <form id="alta" class="space-y-3">
          <label class="block text-sm">
            <span class="text-slate-600">Película</span>
            <select name="peliculaId" class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5">
              ${peliculas.map((p) => `<option value="${p.id}">${escapar(p.titulo)} (${p.duracionMinutos}′)</option>`).join("")}
            </select>
          </label>
          <label class="block text-sm">
            <span class="text-slate-600">Sala</span>
            <select name="salaId" class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5">
              ${salas.map((s) => `<option value="${s.id}">${escapar(s.nombre)} — ${etiqueta(s.tipo)}</option>`).join("")}
            </select>
          </label>
          <label class="block text-sm">
            <span class="text-slate-600">Inicio</span>
            <input name="inicio" type="datetime-local" required
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
          </label>
          <div class="grid grid-cols-2 gap-2">
            <label class="block text-sm">
              <span class="text-slate-600">Idioma</span>
              <select name="idioma" class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5">
                ${idiomas.map((i) => `<option value="${i}">${etiqueta(i)}</option>`).join("")}
              </select>
            </label>
            <label class="block text-sm">
              <span class="text-slate-600">Proyección</span>
              <select name="proyeccion" class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5">
                ${proyecciones.map((p) => `<option value="${p}">${etiqueta(p)}</option>`).join("")}
              </select>
            </label>
          </div>
          <label class="block text-sm">
            <span class="text-slate-600">Precio base</span>
            <input name="precio" type="number" min="1" step="100" required
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
          </label>
          <div id="avisos" class="space-y-1 text-xs"></div>
          <button type="submit"
            class="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white">Programar</button>
        </form>
      </section>
    </div>
  `;

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
        ? "border-emerald-300 bg-emerald-50 text-emerald-900"
        : "border-amber-300 bg-amber-50 text-amber-900";
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
    const boton = evento.target.closest("button[data-borrar]");
    if (!boton) return;
    try {
      await api.eliminarFuncion(boton.dataset.borrar);
      avisar("Función borrada");
      vistaFunciones(contenedor);
    } catch (e) {
      avisar(e.message, "error");
    }
  });
}

/* -------------------------------------------------------- listado de reservas */

const COLOR_ESTADO = {
  RESERVADA: "bg-amber-100 text-amber-800",
  PAGADA: "bg-emerald-100 text-emerald-800",
  CANCELADA: "bg-slate-200 text-slate-600",
};

async function vistaReservas(contenedor) {
  const reservas = await api.obtenerReservas();
  const activas = reservas.filter((r) => r.estado !== "CANCELADA");
  const aCobrar = reservas.filter((r) => r.estado === "RESERVADA");

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Reservas</h1>
    <p class="mb-5 text-sm text-slate-500">
      ${reservas.length} reservas · ${activas.length} activas ·
      ${aCobrar.length} pendientes de cobro
      ${aCobrar.length ? `(${precio(aCobrar.reduce((s, r) => s + r.total, 0))})` : ""}
    </p>

    <div class="overflow-x-auto rounded border border-slate-300 bg-white">
      <table class="w-full text-sm">
        <thead class="border-b border-slate-300 bg-slate-50 text-left text-xs uppercase text-slate-500">
          <tr>
            <th class="p-2">#</th><th>Función</th><th>Cliente</th>
            <th>Butacas</th><th class="text-right">Total</th><th>Estado</th><th></th>
          </tr>
        </thead>
        <tbody>
          ${reservas.map((r) => `
            <tr class="border-b border-slate-200 ${r.estado === "CANCELADA" ? "text-slate-400" : ""}">
              <td class="p-2">${r.id}</td>
              <td>
                ${escapar(r.pelicula?.titulo || "—")}
                <span class="block text-xs text-slate-500">
                  ${r.funcion ? `${escapar(dia(r.funcion.inicio))} ${hora(r.funcion.inicio)} · ${escapar(r.sala.nombre)}` : ""}
                </span>
              </td>
              <td>
                ${escapar(r.cliente?.nombre || "—")}
                <span class="block text-xs text-slate-500">${escapar(r.cliente?.email || "")}</span>
              </td>
              <td class="font-mono text-xs">${r.entradas.map((e) => e.codigoAsiento).join(", ")}</td>
              <td class="text-right whitespace-nowrap">${precio(r.total)}</td>
              <td class="p-2">
                ${chip(etiqueta(r.estado), COLOR_ESTADO[r.estado])}
                ${r.pago
                  ? `<span class="block text-xs text-slate-500">${etiqueta(r.pago.medio)} · ${hora(r.pago.fecha)}</span>`
                  : ""}
              </td>
              <td class="p-2 text-right whitespace-nowrap">
                ${r.estado === "RESERVADA" ? `
                  <a href="#/cobrar/${r.id}" class="text-xs font-medium text-slate-900 hover:underline">Cobrar</a>
                  <button type="button" data-cancelar="${r.id}"
                    class="ml-2 text-xs text-red-700 hover:underline">Cancelar</button>` : ""}
              </td>
            </tr>`).join("")}
        </tbody>
      </table>
    </div>
  `;

  contenedor.querySelector("tbody").addEventListener("click", async (evento) => {
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

/* ------------------------------------------------------------------- cobrar */

async function vistaCobrar(contenedor, id) {
  const [reservas, medios] = await Promise.all([api.obtenerReservas(), api.obtenerMediosPago()]);
  const reserva = reservas.find((r) => r.id === Number(id));
  if (!reserva) throw new Error(`No existe la reserva ${id}`);

  if (reserva.estado !== "RESERVADA") {
    contenedor.innerHTML = `
      <a href="#/reservas" class="text-sm text-slate-500 hover:text-slate-900">&larr; Reservas</a>
      <div class="mt-4 rounded border border-amber-300 bg-amber-50 p-4 text-amber-900">
        La reserva ${reserva.id} está ${etiqueta(reserva.estado).toLowerCase()}, no se puede cobrar.
        ${reserva.pago
          ? `Se cobró ${precio(reserva.pago.monto)} con ${etiqueta(reserva.pago.medio)}
             el ${escapar(fechaHora(reserva.pago.fecha))}.`
          : ""}
      </div>`;
    return;
  }

  contenedor.innerHTML = `
    <a href="#/reservas" class="text-sm text-slate-500 hover:text-slate-900">&larr; Reservas</a>
    <h1 class="mt-2 mb-5 text-2xl font-bold">Cobrar reserva #${reserva.id}</h1>

    <div class="grid gap-4 md:grid-cols-2">
      <section class="rounded border border-slate-300 bg-white p-4">
        <h2 class="mb-3 font-semibold">Cobro</h2>
        <form id="cobro" class="space-y-3">
          <label class="block text-sm">
            <span class="text-slate-600">Medio de pago</span>
            <select name="medio" class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5">
              ${medios.map((m) => `<option value="${m.nombre}">${etiqueta(m.nombre)}</option>`).join("")}
            </select>
          </label>
          <label id="campoCodigo" class="block text-sm">
            <span class="text-slate-600">Código de autorización</span>
            <input name="codigo" class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
            <span class="text-xs text-slate-500">Lo devuelve el procesador. El efectivo no lleva.</span>
          </label>
          <div class="rounded bg-slate-100 p-3 text-sm">
            <span class="text-slate-600">A cobrar</span>
            <p class="text-xl font-bold">${precio(reserva.total)}</p>
            <p class="text-xs text-slate-500">
              Sale del total de las butacas: no se puede cobrar otro importe.
            </p>
          </div>
          <button type="submit"
            class="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white">
            Registrar cobro
          </button>
        </form>
      </section>

      <section class="rounded border border-slate-300 bg-white p-4">
        <h2 class="mb-1 font-semibold">${escapar(reserva.pelicula?.titulo || "—")}</h2>
        <p class="mb-3 text-sm text-slate-600">
          ${reserva.funcion
            ? `${escapar(dia(reserva.funcion.inicio))} ${hora(reserva.funcion.inicio)} ·
               ${escapar(reserva.sala.nombre)} (${etiqueta(reserva.sala.tipo)})`
            : ""}
        </p>
        <p class="mb-3 text-sm">
          ${escapar(reserva.cliente?.nombre || "—")}
          <span class="block text-xs text-slate-500">${escapar(reserva.cliente?.email || "")}</span>
        </p>
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-slate-300 text-left text-xs uppercase text-slate-500">
              <th class="py-1">Butaca</th><th class="text-right">Precio</th>
            </tr>
          </thead>
          <tbody>
            ${reserva.entradas.map((e) => `
              <tr class="border-b border-slate-200">
                <td class="py-1 font-medium">${e.codigoAsiento}</td>
                <td class="text-right">${precio(e.precio)}</td>
              </tr>`).join("")}
          </tbody>
          <tfoot>
            <tr class="font-semibold">
              <td class="py-2">Total</td>
              <td class="py-2 text-right">${precio(reserva.total)}</td>
            </tr>
          </tfoot>
        </table>
      </section>
    </div>
  `;

  const formulario = contenedor.querySelector("#cobro");
  const campoCodigo = contenedor.querySelector("#campoCodigo");

  // R11: el código solo se pide cuando el medio lo exige.
  function ajustarCodigo() {
    const medio = medios.find((m) => m.nombre === formulario.medio.value);
    campoCodigo.classList.toggle("hidden", !medio.requiereAutorizacion);
    formulario.codigo.required = medio.requiereAutorizacion;
  }
  formulario.medio.addEventListener("change", ajustarCodigo);
  ajustarCodigo();

  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const datos = new FormData(formulario);
    try {
      const pago = await api.cobrar(reserva.id, datos.get("medio"), datos.get("codigo"));
      avisar(`Cobrado ${precio(pago.monto)} con ${etiqueta(pago.medio)}`);
      ir("#/caja");
    } catch (e) {
      avisar(e.message, "error");
    }
  });
}

/* ------------------------------------------------------------ arqueo del día */

function hoyISO() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

async function vistaCaja(contenedor, fecha = hoyISO()) {
  const arqueo = await api.obtenerArqueo(fecha);
  const medios = Object.entries(arqueo.porMedio);

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Arqueo</h1>
    <p class="mb-5 text-sm text-slate-500">Lo cobrado en el día, por medio de pago.</p>

    <div class="mb-5 flex flex-wrap items-end gap-4">
      <label class="text-sm">
        <span class="text-slate-600">Fecha</span>
        <input type="date" id="fecha" value="${arqueo.fecha}"
          class="mt-1 block rounded border border-slate-400 px-2 py-1.5" />
      </label>
      <div class="rounded border border-slate-300 bg-white px-4 py-2">
        <span class="text-xs uppercase text-slate-500">Total cobrado</span>
        <p class="text-2xl font-bold">${precio(arqueo.total)}</p>
      </div>
      <div class="rounded border border-slate-300 bg-white px-4 py-2">
        <span class="text-xs uppercase text-slate-500">Operaciones</span>
        <p class="text-2xl font-bold">${arqueo.pagos.length}</p>
      </div>
      <div class="rounded border border-slate-300 bg-white px-4 py-2">
        <span class="text-xs uppercase text-slate-500">Entradas</span>
        <p class="text-2xl font-bold">${arqueo.entradas}</p>
      </div>
    </div>

    ${medios.length ? `
      <div class="mb-5 flex flex-wrap gap-2">
        ${medios.map(([medio, datos]) => `
          <div class="rounded border border-slate-300 bg-white px-3 py-2 text-sm">
            <span class="font-medium">${etiqueta(medio)}</span>
            <span class="text-slate-500">· ${datos.cantidad}</span>
            <span class="ml-2 font-semibold">${precio(datos.total)}</span>
          </div>`).join("")}
      </div>` : ""}

    <div class="overflow-x-auto rounded border border-slate-300 bg-white">
      <table class="w-full text-sm">
        <thead class="border-b border-slate-300 bg-slate-50 text-left text-xs uppercase text-slate-500">
          <tr>
            <th class="p-2">Hora</th><th>Reserva</th><th>Película</th><th>Cliente</th>
            <th>Medio</th><th>Autorización</th><th class="text-right">Monto</th>
          </tr>
        </thead>
        <tbody>
          ${arqueo.pagos.length ? arqueo.pagos.map((p) => `
            <tr class="border-b border-slate-200">
              <td class="p-2 whitespace-nowrap">${hora(p.fecha)}</td>
              <td>#${p.reservaId}</td>
              <td>${escapar(p.pelicula?.titulo || "—")}</td>
              <td>${escapar(p.cliente?.nombre || "—")}</td>
              <td>${etiqueta(p.medio)}</td>
              <td class="font-mono text-xs">${escapar(p.codigoAutorizacion || "—")}</td>
              <td class="text-right whitespace-nowrap font-medium">${precio(p.monto)}</td>
            </tr>`).join("")
            : '<tr><td colspan="7" class="p-6 text-center text-slate-500">No se cobró nada ese día.</td></tr>'}
        </tbody>
      </table>
    </div>
  `;

  contenedor.querySelector("#fecha").addEventListener("change", (evento) => {
    vistaCaja(contenedor, evento.target.value);
  });
}

/* ------------------------------------------------------------------- arranque */

document.getElementById("salir").addEventListener("click", cerrarSesion);
pintarEncabezado();

iniciarRouter({
  contenedor: document.getElementById("app"),
  inicial: sesionActual() ? "peliculas" : "login",
  guardia: (ruta) => {
    if (!sesionActual() && ruta !== "login") return "#/login";
    if (sesionActual() && ruta === "login") return "#/peliculas";
    return null;
  },
  rutas: {
    login: vistaLogin,
    peliculas: vistaPeliculas,
    salas: vistaSalas,
    funciones: vistaFunciones,
    reservas: vistaReservas,
    cobrar: vistaCobrar,
    caja: vistaCaja,
  },
});
