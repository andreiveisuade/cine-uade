// Vistas del encargado: login, ABM de películas, ABM de salas con butacas fuera de
// servicio, programación de funciones y listado de reservas.

import * as api from "./api.js";
import { CLASES_TIPO, dibujarMapa, pantalla, referencia } from "./butacas.js";
import { iniciarRouter, ir } from "./router.js";
import {
  avisar, chip, chipClasificacion, chipEstado, dia, duracion, escapar, etiqueta,
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

/** El acomodador solo valida entradas: no tiene por qué ver el ABM de la cartelera. */
function esAdministrador() {
  return sesionActual()?.rol !== "ACOMODADOR";
}

function pintarEncabezado() {
  const sesion = sesionActual();
  document.getElementById("nav").classList.toggle("hidden", !sesion);
  document.getElementById("nav").classList.toggle("flex", !!sesion);
  document.getElementById("salir").classList.toggle("hidden", !sesion);
  document.getElementById("sesion").textContent = sesion ? sesion.nombre : "";
  document.getElementById("rol").textContent = sesion ? etiqueta(sesion.rol) : "Panel";
  document.querySelectorAll("#nav a[data-rol=ADMINISTRADOR]").forEach((enlace) => {
    enlace.classList.toggle("hidden", !!sesion && !esAdministrador());
  });
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
      // El acomodador entra directo a la puerta: es lo único que puede hacer.
      ir(esAdministrador() ? "#/peliculas" : "#/puerta");
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
      ${peliculas.length} cargadas · ${peliculas.filter((p) => p.enCartelera).length} publicadas.
      Una película llega a la cartelera cuando tiene funciones por delante; despublicarla
      la baja aunque las tenga.
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
                  <button type="button" data-cartelera="${p.id}"
                    title="${p.enCartelera
                      ? "Publicada: aparece en la cartelera si tiene funciones por delante"
                      : "Despublicada: no aparece aunque tenga funciones"}"
                    class="rounded-full px-2 py-0.5 text-xs font-medium ${p.enCartelera
                      ? "bg-emerald-100 text-emerald-800" : "bg-slate-200 text-slate-600"}">
                    ${p.enCartelera ? "Publicada" : "Despublicada"}
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
            <span class="text-slate-600">Publicada</span>
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
        avisar(pelicula.enCartelera ? "Despublicada" : "Publicada");
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
            <span class="text-slate-600">Butacas VIP</span>
            <input name="vip" placeholder="I1,I2,J1"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 font-mono" />
          </label>
          <label class="block text-sm">
            <span class="text-slate-600">Butacas de pareja</span>
            <input name="pareja" placeholder="A1,A2"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5 font-mono" />
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
        codigosPareja: parsearCodigos(datos.get("pareja")),
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
              <td class="font-mono text-xs">${r.entradas.map((e) => e.codigo).join(", ")}</td>
              <td class="text-right whitespace-nowrap">${precio(r.total)}</td>
              <td class="p-2">
                ${chipEstado(r.estado)}
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
              Sale del total de las butacas: no se puede cobrar otro importe. Si hay una
              promoción vigente para este medio de pago, el descuento se aplica al cobrar.
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
              <th class="py-1">Butaca</th><th>Tarifa</th><th class="text-right">Precio</th>
            </tr>
          </thead>
          <tbody>
            ${reserva.entradas.map((e) => `
              <tr class="border-b border-slate-200">
                <td class="py-1 font-medium">${e.codigo}</td>
                <td class="text-xs ${e.tarifa && e.tarifa !== "GENERAL" ? "font-semibold text-amber-800" : "text-slate-500"}">
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
      // Con descuento no alcanza con decir cuánto entró: hay que poder explicar por qué
      // se cobró menos que el subtotal, que es justo lo que el cliente va a preguntar.
      avisar(pago.descuento > 0
        ? `Cobrado ${precio(pago.monto)} con ${etiqueta(pago.medio)} · ${precio(pago.descuento)} de descuento`
        : `Cobrado ${precio(pago.monto)} con ${etiqueta(pago.medio)}`);
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
            <th>Medio</th><th>Autorización</th><th class="text-right">Descuento</th>
            <th class="text-right">Monto</th>
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
              <td class="text-right whitespace-nowrap text-xs ${p.descuento > 0 ? "text-emerald-700" : "text-slate-400"}">
                ${p.descuento > 0 ? "−" + precio(p.descuento) : "—"}
              </td>
              <td class="text-right whitespace-nowrap font-medium">${precio(p.monto)}</td>
            </tr>`).join("")
            : '<tr><td colspan="8" class="p-6 text-center text-slate-500">No se cobró nada ese día.</td></tr>'}
        </tbody>
      </table>
    </div>
  `;

  contenedor.querySelector("#fecha").addEventListener("change", (evento) => {
    vistaCaja(contenedor, evento.target.value);
  });
}



/* ------------------------------------------------------------ programaciones */

/**
 * CU-03b: la grilla. Un cine no carga quince funciones de a una, define
 * «Matrix en la Sala 1, todos los días a las 20:30, del 1 al 15».
 *
 * La pantalla tiene dos botones y un solo formulario, que es el punto: «Previsualizar»
 * muestra fecha por fecha qué va a pasar sin escribir nada, y «Confirmar» aplica. El
 * informe que devuelven los dos es el mismo, así que lo que se ve antes de confirmar es
 * literalmente lo que se va a guardar.
 *
 * El botón de confirmar arranca deshabilitado a propósito: se habilita recién cuando
 * hay una previsualización de esos mismos datos. Cambiar cualquier campo la invalida y
 * vuelve a apagarlo, porque un informe de otra grilla no dice nada de esta.
 */
async function vistaProgramaciones(contenedor) {
  const [programaciones, peliculas, salas, idiomas, proyecciones] = await Promise.all([
    api.obtenerProgramaciones(), api.obtenerPeliculas(), api.obtenerSalas(),
    api.obtenerIdiomas(), api.obtenerProyecciones(),
  ]);

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Grilla de funciones</h1>
    <p class="mb-5 text-sm text-slate-500">
      Una grilla genera las funciones del rango de una sola vez. Las que chocan con algo
      ya programado en esa sala se saltean, y el informe dice cuáles.
    </p>

    <div class="grid gap-4 lg:grid-cols-[1fr_340px]">
      <section class="overflow-x-auto rounded border border-slate-300 bg-white">
        <table class="w-full text-sm">
          <thead class="border-b border-slate-300 bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr><th class="p-2">Película</th><th>Sala</th><th>Cuándo</th><th>Días</th><th class="text-right">Precio</th><th></th></tr>
          </thead>
          <tbody>
            ${programaciones.length ? programaciones.map((p) => `
              <tr class="cursor-pointer border-b border-slate-200 hover:bg-slate-50 ${p.activa ? "" : "text-slate-400"}"
                  data-ver="${p.id}">
                <td class="p-2 font-medium">${escapar(tituloDe(peliculas, p.peliculaId))}</td>
                <td class="whitespace-nowrap">${escapar(nombreDeSala(salas, p.salaId))}</td>
                <td class="whitespace-nowrap text-xs">
                  ${escapar(p.desde)} al ${escapar(p.hasta)} · <span class="font-medium">${escapar(p.horaInicio.slice(0, 5))}</span>
                </td>
                <td class="text-xs">${escapar(diasDeLaGrilla(p))}</td>
                <td class="text-right whitespace-nowrap">${precio(p.precio)}</td>
                <td class="p-2 text-right whitespace-nowrap">
                  <button type="button" data-${p.activa ? "baja" : "alta"}="${p.id}"
                    class="text-xs ${p.activa ? "text-red-700" : "text-emerald-700"} hover:underline">
                    ${p.activa ? "Dar de baja" : "Reactivar"}
                  </button>
                </td>
              </tr>`).join("")
              : '<tr><td colspan="6" class="p-6 text-center text-slate-500">Todavía no hay grillas cargadas.</td></tr>'}
          </tbody>
        </table>
        <p class="border-t border-slate-200 p-2 text-xs text-slate-500">
          Dar de baja una grilla <strong>no borra las funciones que ya generó</strong>:
          pueden tener entradas vendidas. Solo evita que genere nuevas. Hacé clic en una
          fila para ver qué funciones creó.
        </p>
        <div id="detalle"></div>
      </section>

      <section class="rounded border border-slate-300 bg-white p-4">
        <h2 class="mb-3 font-semibold">Nueva grilla</h2>
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

          <div class="grid grid-cols-2 gap-2">
            <label class="block text-sm">
              <span class="text-slate-600">Desde</span>
              <input name="desde" type="date" required value="${hoyISO()}"
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
            </label>
            <label class="block text-sm">
              <span class="text-slate-600">Hasta</span>
              <input name="hasta" type="date" required
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
            </label>
          </div>

          <label class="block text-sm">
            <span class="text-slate-600">Hora de la función</span>
            <input name="horaInicio" type="time" required value="20:30"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
          </label>

          <fieldset class="text-sm">
            <legend class="text-slate-600">Días (ninguno = todos)</legend>
            <div class="mt-1 flex flex-wrap gap-x-3 gap-y-1">
              ${DIAS_SEMANA.map((d) => `
                <label class="flex items-center gap-1 text-xs">
                  <input type="checkbox" name="dia" value="${d}" /> ${etiqueta(d).slice(0, 3)}
                </label>`).join("")}
            </div>
          </fieldset>

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
            <input name="precio" type="number" min="1" step="100" required value="5000"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
          </label>

          <div class="grid grid-cols-2 gap-2">
            <button type="button" id="previsualizar"
              class="rounded border border-slate-400 px-4 py-2 text-sm font-medium">Previsualizar</button>
            <button type="submit" id="confirmar" disabled
              class="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:bg-slate-300">
              Confirmar
            </button>
          </div>
          <p id="errorAlta" class="hidden text-sm text-red-700"></p>
        </form>

        <div id="informe" class="mt-3"></div>

        <p class="mt-3 border-t border-slate-200 pt-3 text-xs text-slate-500">
          Al confirmar, el servidor <strong>vuelve a revisar</strong> cada fecha: entre que
          mirás el informe y confirmás, otro puede haber programado algo en esa sala.
        </p>
      </section>
    </div>
  `;

  const formulario = contenedor.querySelector("#alta");
  const errorAlta = contenedor.querySelector("#errorAlta");
  const informe = contenedor.querySelector("#informe");
  const botonConfirmar = contenedor.querySelector("#confirmar");

  function leerFormulario() {
    const datos = new FormData(formulario);
    return {
      peliculaId: datos.get("peliculaId"),
      salaId: datos.get("salaId"),
      desde: datos.get("desde"),
      hasta: datos.get("hasta"),
      horaInicio: datos.get("horaInicio"),
      diasSemana: datos.getAll("dia"),
      idioma: datos.get("idioma"),
      proyeccion: datos.get("proyeccion"),
      precio: Number(datos.get("precio")),
    };
  }

  /** Una previsualización vale solo para los datos con los que se pidió. */
  function invalidarPrevisualizacion() {
    botonConfirmar.disabled = true;
    informe.innerHTML = "";
  }

  function mostrarError(mensaje) {
    errorAlta.textContent = mensaje;
    errorAlta.classList.remove("hidden");
  }

  formulario.addEventListener("input", invalidarPrevisualizacion);
  formulario.addEventListener("change", invalidarPrevisualizacion);

  contenedor.querySelector("#previsualizar").addEventListener("click", async () => {
    if (!formulario.reportValidity()) return;
    errorAlta.classList.add("hidden");
    try {
      const plan = await api.previsualizarProgramacion(leerFormulario());
      informe.innerHTML = dibujarInforme(plan, false);
      botonConfirmar.disabled = plan.generadas === 0;
    } catch (e) {
      invalidarPrevisualizacion();
      mostrarError(e.message);
    }
  });

  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    errorAlta.classList.add("hidden");
    try {
      const plan = await api.crearProgramacion(leerFormulario());
      avisar(`Grilla creada: ${plan.generadas} funciones`
        + (plan.salteadas ? `, ${plan.salteadas} salteadas` : ""));
      await vistaProgramaciones(contenedor);
      // El informe del alta sobrevive al repintado: es donde se lee qué quedó afuera.
      contenedor.querySelector("#informe").innerHTML = dibujarInforme(plan, true);
    } catch (e) {
      mostrarError(e.message);
    }
  });

  contenedor.addEventListener("click", async (evento) => {
    const baja = evento.target.closest("button[data-baja]");
    const alta = evento.target.closest("button[data-alta]");
    if (baja || alta) {
      try {
        if (baja) await api.darDeBajaProgramacion(baja.dataset.baja);
        else await api.darDeAltaProgramacion(alta.dataset.alta);
        vistaProgramaciones(contenedor);
      } catch (e) {
        avisar(e.message, "error");
      }
      return;
    }

    const fila = evento.target.closest("tr[data-ver]");
    if (!fila) return;
    try {
      const grilla = await api.obtenerProgramacion(fila.dataset.ver);
      contenedor.querySelector("#detalle").innerHTML = dibujarFuncionesGeneradas(grilla);
    } catch (e) {
      avisar(e.message, "error");
    }
  });
}

const tituloDe = (peliculas, id) =>
  peliculas.find((p) => p.id === id)?.titulo || `Película ${id}`;

const nombreDeSala = (salas, id) =>
  salas.find((s) => s.id === id)?.nombre || `Sala ${id}`;

/** Sin días no quiere decir ninguno: quiere decir todos los del rango. */
function diasDeLaGrilla(grilla) {
  return grilla.diasSemana?.length
    ? grilla.diasSemana.map((d) => etiqueta(d).slice(0, 3)).join(", ")
    : "todos";
}

/**
 * El informe, fecha por fecha. Es el mismo dibujo antes y después de confirmar porque
 * es el mismo dato: lo único que cambia es el encabezado.
 */
function dibujarInforme(plan, aplicado) {
  const titulo = aplicado
    ? `Se generaron ${plan.generadas} funciones`
    : `Se van a generar ${plan.generadas} funciones`;
  return `
    <div class="rounded border ${aplicado ? "border-emerald-300 bg-emerald-50" : "border-slate-300 bg-slate-50"} p-3">
      <p class="mb-2 text-sm font-semibold">
        ${escapar(titulo)}${plan.salteadas ? `, ${plan.salteadas} se ${aplicado ? "saltearon" : "saltean"}` : ""}
      </p>
      <ul class="max-h-64 space-y-1 overflow-y-auto text-xs">
        ${plan.funciones.map((f) => (f.choca
          ? `<li class="rounded border border-amber-300 bg-amber-50 px-2 py-1 text-amber-900">
               <strong>${escapar(fechaHora(f.inicio))}</strong> · ${escapar(f.motivo || "se pisa con otra función")}
             </li>`
          : `<li class="px-2 py-1 text-slate-600">${escapar(fechaHora(f.inicio))}</li>`)).join("")}
      </ul>
    </div>`;
}

function dibujarFuncionesGeneradas(grilla) {
  if (!grilla.funciones?.length) {
    return `<p class="border-t border-slate-200 p-3 text-sm text-slate-500">
      Esta grilla no generó ninguna función: todas sus fechas chocaban con algo ya programado.
    </p>`;
  }
  return `
    <div class="border-t border-slate-200 p-3">
      <p class="mb-2 text-sm font-semibold">
        Funciones de la grilla ${grilla.id} (${grilla.funciones.length})
      </p>
      <div class="flex flex-wrap gap-1 text-xs">
        ${grilla.funciones.map((f) =>
          `<span class="rounded bg-slate-100 px-2 py-0.5">${escapar(fechaHora(f.inicio))}</span>`).join("")}
      </div>
    </div>`;
}

/* -------------------------------------------------------------- promociones */

const DIAS_SEMANA = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

/** Cómo se lee el beneficio de cada tipo, que es lo que las diferencia. */
function beneficioDe(promocion) {
  if (promocion.tipo === "PORCENTAJE") return `${promocion.porcentaje}% off`;
  if (promocion.tipo === "MONTO_FIJO") return `${precio(promocion.monto)} off`;
  return `${promocion.lleva}x${promocion.paga}`;
}

function condicionesDe(promocion) {
  const partes = [];
  if (promocion.diasSemana?.length) {
    partes.push(promocion.diasSemana.map((d) => etiqueta(d).slice(0, 3)).join(", "));
  }
  if (promocion.horaDesde || promocion.horaHasta) {
    partes.push(`${(promocion.horaDesde || "00:00").slice(0, 5)}–${(promocion.horaHasta || "23:59").slice(0, 5)}`);
  }
  if (promocion.mediosPago?.length) partes.push(promocion.mediosPago.map(etiqueta).join(", "));
  // Sin condiciones no quiere decir "ninguna": quiere decir que corre siempre.
  return partes.length ? partes.join(" · ") : "todos los días, cualquier medio";
}

async function vistaPromociones(contenedor) {
  const [promociones, mediosPago] = await Promise.all([
    api.obtenerPromociones(),
    api.obtenerMediosPago(),
  ]);

  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Promociones</h1>
    <p class="mb-5 text-sm text-slate-500">
      ${promociones.filter((p) => p.activa).length} activas de ${promociones.length}.
      No se acumulan: en cada cobro se aplica la que más descuenta.
    </p>

    <div class="grid gap-4 lg:grid-cols-[1fr_340px]">
      <section class="overflow-x-auto rounded border border-slate-300 bg-white">
        <table class="w-full text-sm">
          <thead class="border-b border-slate-300 bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr><th class="p-2">Nombre</th><th>Beneficio</th><th>Vigencia</th><th>Cuándo</th><th></th></tr>
          </thead>
          <tbody>
            ${promociones.length ? promociones.map((p) => `
              <tr class="border-b border-slate-200 ${p.activa ? "" : "text-slate-400"}">
                <td class="p-2 font-medium">${escapar(p.nombre)}</td>
                <td class="whitespace-nowrap font-semibold">${escapar(beneficioDe(p))}</td>
                <td class="whitespace-nowrap text-xs">
                  ${escapar(p.vigenciaDesde)} al ${escapar(p.vigenciaHasta)}
                </td>
                <td class="text-xs">${escapar(condicionesDe(p))}</td>
                <td class="p-2 text-right whitespace-nowrap">
                  <button type="button" data-${p.activa ? "baja" : "alta"}="${p.id}"
                    class="text-xs ${p.activa ? "text-red-700" : "text-emerald-700"} hover:underline">
                    ${p.activa ? "Dar de baja" : "Reactivar"}
                  </button>
                </td>
              </tr>`).join("")
              : '<tr><td colspan="5" class="p-6 text-center text-slate-500">Todavía no hay promociones.</td></tr>'}
          </tbody>
        </table>
        <p class="border-t border-slate-200 p-2 text-xs text-slate-500">
          Las promociones no se borran: se dan de baja. Una que ya se usó en un cobro
          tiene que seguir existiendo para poder explicar por qué se cobró ese monto.
        </p>
      </section>

      <section class="rounded border border-slate-300 bg-white p-4">
        <h2 class="mb-3 font-semibold">Nueva promoción</h2>
        <form id="alta" class="space-y-3">
          <label class="block text-sm">
            <span class="text-slate-600">Nombre</span>
            <input name="nombre" required placeholder="Miércoles 2x1"
              class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
          </label>

          <label class="block text-sm">
            <span class="text-slate-600">Tipo</span>
            <select name="tipo" id="tipo" class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5">
              <option value="PORCENTAJE">Porcentaje</option>
              <option value="MONTO_FIJO">Monto fijo</option>
              <option value="NXM">NxM (2x1)</option>
            </select>
          </label>

          <div data-campos="PORCENTAJE">
            <label class="block text-sm">
              <span class="text-slate-600">Porcentaje de descuento</span>
              <input name="porcentaje" type="number" min="1" max="99" value="30"
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
            </label>
          </div>
          <div data-campos="MONTO_FIJO" class="hidden">
            <label class="block text-sm">
              <span class="text-slate-600">Monto a descontar</span>
              <input name="monto" type="number" min="1" value="2000"
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
            </label>
          </div>
          <div data-campos="NXM" class="hidden grid-cols-2 gap-2">
            <label class="block text-sm">
              <span class="text-slate-600">Lleva</span>
              <input name="lleva" type="number" min="2" value="2"
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
            </label>
            <label class="block text-sm">
              <span class="text-slate-600">Paga</span>
              <input name="paga" type="number" min="1" value="1"
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
            </label>
          </div>

          <div class="grid grid-cols-2 gap-2">
            <label class="block text-sm">
              <span class="text-slate-600">Desde</span>
              <input name="vigenciaDesde" type="date" required value="${hoyISO()}"
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
            </label>
            <label class="block text-sm">
              <span class="text-slate-600">Hasta</span>
              <input name="vigenciaHasta" type="date" required
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
            </label>
          </div>

          <fieldset class="text-sm">
            <legend class="text-slate-600">Días (ninguno = todos)</legend>
            <div class="mt-1 flex flex-wrap gap-x-3 gap-y-1">
              ${DIAS_SEMANA.map((d) => `
                <label class="flex items-center gap-1 text-xs">
                  <input type="checkbox" name="dia" value="${d}" /> ${etiqueta(d).slice(0, 3)}
                </label>`).join("")}
            </div>
          </fieldset>

          <div class="grid grid-cols-2 gap-2">
            <label class="block text-sm">
              <span class="text-slate-600">Desde hora</span>
              <input name="horaDesde" type="time"
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
            </label>
            <label class="block text-sm">
              <span class="text-slate-600">Hasta hora</span>
              <input name="horaHasta" type="time"
                class="mt-1 w-full rounded border border-slate-400 px-2 py-1.5" />
            </label>
          </div>

          <fieldset class="text-sm">
            <legend class="text-slate-600">Medios (ninguno = cualquiera)</legend>
            <div class="mt-1 flex flex-wrap gap-x-3 gap-y-1">
              ${mediosPago.map((m) => `
                <label class="flex items-center gap-1 text-xs">
                  <input type="checkbox" name="medio" value="${m.nombre}" /> ${etiqueta(m.nombre)}
                </label>`).join("")}
            </div>
          </fieldset>

          <button type="submit"
            class="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white">
            Crear promoción
          </button>
          <p id="errorAlta" class="hidden text-sm text-red-700"></p>
        </form>
        <p class="mt-3 border-t border-slate-200 pt-3 text-xs text-slate-500">
          Las condiciones se evalúan contra el horario de la <strong>función</strong>, no
          contra el momento de la compra: un 2x1 de los miércoles vale para la función del
          miércoles aunque las entradas se compren el lunes.
        </p>
      </section>
    </div>
  `;

  const formulario = contenedor.querySelector("#alta");
  const errorAlta = contenedor.querySelector("#errorAlta");
  const selectorTipo = contenedor.querySelector("#tipo");

  // El formulario es uno solo y muestra los campos del tipo elegido, igual que la tabla
  // tiene una columna por beneficio y deja en null las que no aplican.
  function mostrarCamposDelTipo() {
    contenedor.querySelectorAll("[data-campos]").forEach((bloque) => {
      const visible = bloque.dataset.campos === selectorTipo.value;
      bloque.classList.toggle("hidden", !visible);
      if (bloque.dataset.campos === "NXM") bloque.classList.toggle("grid", visible);
    });
  }
  selectorTipo.addEventListener("change", mostrarCamposDelTipo);
  mostrarCamposDelTipo();

  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const datos = new FormData(formulario);
    const tipo = datos.get("tipo");
    const marcados = (nombre) => datos.getAll(nombre);
    try {
      await api.crearPromocion({
        nombre: datos.get("nombre"),
        tipo,
        porcentaje: tipo === "PORCENTAJE" ? Number(datos.get("porcentaje")) : null,
        monto: tipo === "MONTO_FIJO" ? Number(datos.get("monto")) : null,
        lleva: tipo === "NXM" ? Number(datos.get("lleva")) : null,
        paga: tipo === "NXM" ? Number(datos.get("paga")) : null,
        vigenciaDesde: datos.get("vigenciaDesde"),
        vigenciaHasta: datos.get("vigenciaHasta"),
        diasSemana: marcados("dia"),
        horaDesde: datos.get("horaDesde") || null,
        horaHasta: datos.get("horaHasta") || null,
        mediosPago: marcados("medio"),
      });
      avisar("Promoción creada");
      vistaPromociones(contenedor);
    } catch (e) {
      errorAlta.textContent = e.message;
      errorAlta.classList.remove("hidden");
    }
  });

  contenedor.addEventListener("click", async (evento) => {
    const baja = evento.target.closest("button[data-baja]");
    const alta = evento.target.closest("button[data-alta]");
    if (!baja && !alta) return;
    try {
      if (baja) await api.darDeBajaPromocion(baja.dataset.baja);
      else await api.darDeAltaPromocion(alta.dataset.alta);
      vistaPromociones(contenedor);
    } catch (e) {
      avisar(e.message, "error");
    }
  });
}

/* -------------------------------------------------------------------- puerta */

/**
 * CU-18: lo que usa el acomodador. Se escanea o se tipea el código de la reserva y se
 * marca la entrada como usada.
 *
 * El foco vuelve al campo después de cada validación porque en la puerta se encadenan
 * una atrás de otra: obligar a hacer clic entre persona y persona sería insufrible.
 */
async function vistaPuerta(contenedor) {
  contenedor.innerHTML = `
    <h1 class="mb-1 text-2xl font-bold">Validar entrada</h1>
    <p class="mb-5 text-sm text-slate-500">
      Escaneá el código del ticket o tipealo. Cada entrada sirve una sola vez.
    </p>

    <form id="validar" class="flex flex-wrap gap-2">
      <input name="codigo" required autocomplete="off" autofocus placeholder="A1B2C3D4"
        class="w-48 rounded border border-slate-400 px-3 py-2 font-mono text-lg uppercase tracking-widest" />
      <button type="submit" class="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white">
        Validar
      </button>
    </form>

    <div id="resultado" class="mt-5"></div>
  `;

  const formulario = contenedor.querySelector("#validar");
  const campoCodigo = formulario.querySelector("input[name=codigo]");
  const resultado = contenedor.querySelector("#resultado");

  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const codigo = campoCodigo.value.trim().toUpperCase();
    if (!codigo) return;
    try {
      const reserva = await api.validarEntrada(codigo);
      resultado.innerHTML = entradaValida(reserva);
    } catch (e) {
      // Los tres motivos —código inexistente, sin pagar y ya usada— se muestran igual
      // de fuerte: en la puerta lo único que importa es que no pasa.
      resultado.innerHTML = `
        <div class="rounded border-2 border-red-400 bg-red-50 p-4">
          <p class="text-lg font-bold text-red-900">NO PASA</p>
          <p class="mt-1 text-sm text-red-900">${escapar(e.message)}</p>
        </div>`;
    }
    campoCodigo.value = "";
    campoCodigo.focus();
  });
}

function entradaValida(reserva) {
  const butacas = reserva.entradas.map((e) => {
    const pideCarnet = e.tarifa && e.tarifa !== "GENERAL";
    return `
      <li class="flex items-center justify-between border-t border-emerald-200 py-1">
        <span class="font-mono font-semibold">${escapar(e.codigo)}</span>
        <span class="${pideCarnet ? "font-semibold text-amber-800" : "text-slate-600"}">
          ${etiqueta(e.tarifa || "GENERAL")}${pideCarnet ? " · pedir carnet" : ""}
        </span>
      </li>`;
  }).join("");

  return `
    <div class="rounded border-2 border-emerald-500 bg-emerald-50 p-4">
      <p class="text-lg font-bold text-emerald-900">ADELANTE</p>
      <p class="mt-2 font-semibold">${escapar(reserva.pelicula?.titulo || "")}</p>
      <p class="text-sm text-slate-700">
        ${escapar(reserva.sala?.nombre || "")} ·
        ${escapar(fechaHora(reserva.funcion?.inicio))}
      </p>
      <ul class="mt-3 text-sm">${butacas}</ul>
      <p class="mt-3 text-xs text-slate-600">
        ${reserva.entradas.length} persona${reserva.entradas.length === 1 ? "" : "s"} ·
        ingreso registrado ${escapar(fechaHora(reserva.ingresadaEn))}
      </p>
    </div>`;
}

/* ------------------------------------------------------------------- arranque */

document.getElementById("salir").addEventListener("click", cerrarSesion);
pintarEncabezado();

iniciarRouter({
  contenedor: document.getElementById("app"),
  inicial: sesionActual() ? (esAdministrador() ? "peliculas" : "puerta") : "login",
  guardia: (ruta) => {
    if (!sesionActual() && ruta !== "login") return "#/login";
    if (sesionActual() && ruta === "login") return esAdministrador() ? "#/peliculas" : "#/puerta";
    // El rol no es solo cosmético: escondemos el menú y además cerramos la ruta.
    if (sesionActual() && !esAdministrador() && ruta !== "puerta") return "#/puerta";
    return null;
  },
  rutas: {
    login: vistaLogin,
    peliculas: vistaPeliculas,
    salas: vistaSalas,
    funciones: vistaFunciones,
    programaciones: vistaProgramaciones,
    reservas: vistaReservas,
    cobrar: vistaCobrar,
    caja: vistaCaja,
    promociones: vistaPromociones,
    puerta: vistaPuerta,
  },
});
