// Implementación contra la API REST del backend, siguiendo API.md.
//
// La base es relativa: el navegador pide /api al mismo origen del que bajó la página y
// nginx lo reenvía al backend por la red interna de Docker. Por eso acá no aparece ni
// el host ni el puerto del backend, y no hace falta CORS.

const BASE = "/api";

/**
 * Los errores de validación vienen con 400 y {"error": "..."}: ese texto es el mensaje
 * que tiran los gestores y se muestra tal cual, así que se propaga como Error.
 */
async function pedir(ruta, opciones = {}) {
  let respuesta;
  try {
    respuesta = await fetch(BASE + ruta, {
      headers: opciones.cuerpo ? { "Content-Type": "application/json" } : {},
      method: opciones.metodo || "GET",
      body: opciones.cuerpo ? JSON.stringify(opciones.cuerpo) : undefined,
    });
  } catch {
    throw new Error("No se pudo conectar con el servidor");
  }

  if (respuesta.status === 204) return null;

  let datos = null;
  const texto = await respuesta.text();
  if (texto) {
    try {
      datos = JSON.parse(texto);
    } catch {
      if (!respuesta.ok) throw new Error(texto);
      throw new Error("El servidor devolvió una respuesta que no se pudo leer");
    }
  }

  if (!respuesta.ok) {
    const error = new Error(datos?.error || `Error ${respuesta.status} del servidor`);
    // 409 es una carrera: otro se quedó con la butaca mientras el cliente confirmaba.
    // Distinto del 400, que es "esa butaca ya estaba tomada, elegí otra".
    error.status = respuesta.status;
    throw error;
  }
  return datos;
}

const get = (ruta) => pedir(ruta);
const post = (ruta, cuerpo) => pedir(ruta, { metodo: "POST", cuerpo });
const put = (ruta, cuerpo) => pedir(ruta, { metodo: "PUT", cuerpo });
const borrar = (ruta) => pedir(ruta, { metodo: "DELETE" });

/* ---------------------------------------------------------------- catálogos */

export const obtenerGeneros = () => get("/generos");
export const obtenerClasificaciones = () => get("/clasificaciones");
export const obtenerTiposSala = () => get("/tipos-sala");
export const obtenerIdiomas = () => get("/idiomas");
export const obtenerProyecciones = () => get("/proyecciones");
export const obtenerMediosPago = () => get("/medios-pago");
export const obtenerTarifas = () => get("/tarifas");

/* ------------------------------------------------------------------ cliente */

export const obtenerCartelera = (genero) =>
  get("/cartelera" + (genero ? `?genero=${encodeURIComponent(genero)}` : ""));

export const obtenerPelicula = (id) => get(`/peliculas/${id}`);
/**
 * Arma el `?a=1&b=2` de un objeto de filtros, salteando lo vacío.
 *
 * Saltear importa: mandar `?estado=` en vez de omitirlo obliga al backend a distinguir
 * "vacío" de "ausente", que son lo mismo para quien filtra. Y encodeURIComponent no es
 * opcional — el texto libre puede traer un `&` o un `#`.
 */
function consulta(filtros) {
  const partes = Object.entries(filtros || {})
    .filter(([, valor]) => valor !== null && valor !== undefined && String(valor).trim() !== "")
    .map(([clave, valor]) => `${clave}=${encodeURIComponent(String(valor).trim())}`);
  return partes.length ? `?${partes.join("&")}` : "";
}

export const obtenerFuncionesDePelicula = (peliculaId) => get(`/peliculas/${peliculaId}/funciones`);
export const obtenerFuncion = (id) => get(`/funciones/${id}`);

export const registrarCliente = ({ nombre, email }) => post("/clientes", { nombre, email });

export const buscarClientePorEmail = (email) =>
  get(`/clientes?email=${encodeURIComponent(String(email || "").trim())}`);

// butacas es { "C5": "GENERAL", "C6": "JUBILADO" }: la tarifa es por persona, así que
// va por butaca y no por reserva.
export const crearReserva = ({ funcionId, nombre, email, butacas }) =>
  post("/reservas", { funcionId: Number(funcionId), nombre, email, butacas });

export const obtenerReserva = (id) => get(`/reservas/${id}`);

export const obtenerReservasDe = (email) =>
  get(`/reservas?email=${encodeURIComponent(String(email || "").trim())}`);

/* ---------------------------------------------------------------- encargado */

export const login = (email, password) => post("/sesion", { email, password });

/** @param filtros {q, genero, publicada} */
export const obtenerPeliculas = (filtros) => get(`/peliculas${consulta(filtros)}`);

/** El buzón: lo que trajo el importador y todavía nadie miró. */
export const obtenerPeliculasPendientes = () => get("/peliculas/pendientes");

export const confirmarPelicula = (id) => post(`/peliculas/${id}/confirmacion`, {});

export const descartarPelicula = (id) => post(`/peliculas/${id}/descarte`, {});

export const crearPelicula = ({ titulo, duracionMinutos, generos, clasificacion,
                                posterUrl, ...catalogo }) =>
  post("/peliculas", {
    titulo, generos, clasificacion, posterUrl,
    duracionMinutos: Number(duracionMinutos),
    ...catalogo,
    anio: catalogo.anio === undefined ? undefined : Number(catalogo.anio) || 0,
  });

export const actualizarPelicula = (id, cambios) => put(`/peliculas/${id}`, cambios);
export const eliminarPelicula = (id) => borrar(`/peliculas/${id}`);

export const obtenerSalas = () => get("/salas");
export const obtenerSala = (id) => get(`/salas/${id}`);

export const crearSala = ({ nombre, tipo, butacasPorFila,
                            codigosVip, codigosPareja, codigosAccesibles }) =>
  post("/salas", { nombre, tipo, butacasPorFila,
                   codigosVip, codigosPareja, codigosAccesibles });

export const eliminarSala = (id) => borrar(`/salas/${id}`);

export const cambiarEstadoAsiento = (salaId, codigo, estado) =>
  put(`/salas/${salaId}/asientos/${encodeURIComponent(String(codigo).trim().toUpperCase())}`,
    { estado });

/**
 * @param filtros {peliculaId, salaId, desde, hasta}; las claves vacías no viajan, así que
 *                sin filtros la URL queda igual que antes.
 */
export const obtenerFunciones = (filtros) => get(`/funciones${consulta(filtros)}`);

export const programarFuncion = ({ peliculaId, salaId, inicio, idioma, proyeccion, precio }) =>
  post("/funciones", {
    peliculaId: Number(peliculaId),
    salaId: Number(salaId),
    // el input datetime-local no manda los segundos y el backend espera ISO completo
    inicio: inicio && inicio.length === 16 ? `${inicio}:00` : inicio,
    idioma, proyeccion, precio: Number(precio),
  });

export const eliminarFuncion = (id) => borrar(`/funciones/${id}`);

/* ------------------------------------------------------------ programaciones */

/** @param filtros {peliculaId, salaId, activa} */
export const obtenerProgramaciones = (filtros) => get(`/programaciones${consulta(filtros)}`);
export const obtenerProgramacion = (id) => get(`/programaciones/${id}`);

// Las dos mandan exactamente el mismo cuerpo y solo cambian de ruta: previsualizar
// tiene que predecir lo que hace el alta, y armar el pedido de dos maneras distintas
// sería la forma más fácil de que dejara de hacerlo.
const cuerpoProgramacion = ({ peliculaId, salaId, desde, hasta, horaInicio,
                              diasSemana, idioma, proyeccion, precio }) => ({
  peliculaId: Number(peliculaId),
  salaId: Number(salaId),
  desde, hasta, horaInicio,
  diasSemana: diasSemana || [],
  idioma, proyeccion, precio: Number(precio),
});

export const previsualizarProgramacion = (grilla) =>
  post("/programaciones/previsualizar", cuerpoProgramacion(grilla));

export const crearProgramacion = (grilla) =>
  post("/programaciones", cuerpoProgramacion(grilla));

export const darDeBajaProgramacion = (id) => post(`/programaciones/${id}/baja`);
export const darDeAltaProgramacion = (id) => post(`/programaciones/${id}/alta`);

/** @param filtros {estado, dia, q} */
export const obtenerReservas = (filtros) => get(`/reservas${consulta(filtros)}`);
export const cancelarReserva = (id) => post(`/reservas/${id}/cancelacion`);

export const cobrar = (reservaId, medio, codigoAutorizacion) =>
  post(`/reservas/${reservaId}/pago`, { medio, codigoAutorizacion });

export const obtenerPagoDeReserva = (reservaId) => get(`/reservas/${reservaId}/pago`);

export const obtenerArqueo = (fecha) => get(`/arqueo?fecha=${encodeURIComponent(fecha)}`);

/* -------------------------------------------------------------- promociones */

export const obtenerPromociones = () => get("/promociones");
export const crearPromocion = (promocion) => post("/promociones", promocion);
export const darDeBajaPromocion = (id) => post(`/promociones/${id}/baja`);
export const darDeAltaPromocion = (id) => post(`/promociones/${id}/alta`);

/* ----------------------------------------------------------- control de acceso */

// POST y no GET porque marca la entrada como usada: repetirlo falla a propósito.
export const validarEntrada = (codigo) => post("/acceso", { codigo });
