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
    throw new Error(datos?.error || `Error ${respuesta.status} del servidor`);
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

/* ------------------------------------------------------------------ cliente */

export const obtenerCartelera = (genero) =>
  get("/cartelera" + (genero ? `?genero=${encodeURIComponent(genero)}` : ""));

export const obtenerPelicula = (id) => get(`/peliculas/${id}`);
export const obtenerFuncionesDePelicula = (peliculaId) => get(`/peliculas/${peliculaId}/funciones`);
export const obtenerFuncion = (id) => get(`/funciones/${id}`);

export const crearReserva = ({ funcionId, nombre, email, codigos }) =>
  post("/reservas", { funcionId: Number(funcionId), nombre, email, codigos });

export const obtenerReserva = (id) => get(`/reservas/${id}`);

/* ---------------------------------------------------------------- encargado */

export const login = (email, password) => post("/sesion", { email, password });

export const obtenerPeliculas = () => get("/peliculas");

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

export const crearSala = ({ nombre, tipo, butacasPorFila, codigosVip, codigosAccesibles }) =>
  post("/salas", { nombre, tipo, butacasPorFila, codigosVip, codigosAccesibles });

export const eliminarSala = (id) => borrar(`/salas/${id}`);

export const cambiarEstadoAsiento = (salaId, codigo, estado) =>
  put(`/salas/${salaId}/asientos/${encodeURIComponent(String(codigo).trim().toUpperCase())}`,
    { estado });

export const obtenerFunciones = () => get("/funciones");

export const programarFuncion = ({ peliculaId, salaId, inicio, idioma, proyeccion, precio }) =>
  post("/funciones", {
    peliculaId: Number(peliculaId),
    salaId: Number(salaId),
    // el input datetime-local no manda los segundos y el backend espera ISO completo
    inicio: inicio && inicio.length === 16 ? `${inicio}:00` : inicio,
    idioma, proyeccion, precio: Number(precio),
  });

export const eliminarFuncion = (id) => borrar(`/funciones/${id}`);

export const obtenerReservas = () => get("/reservas");
export const cancelarReserva = (id) => post(`/reservas/${id}/cancelacion`);

export const cobrar = (reservaId, medio, codigoAutorizacion) =>
  post(`/reservas/${reservaId}/pago`, { medio, codigoAutorizacion });

export const obtenerPagoDeReserva = (reservaId) => get(`/reservas/${reservaId}/pago`);

export const obtenerArqueo = (fecha) => get(`/arqueo?fecha=${encodeURIComponent(fecha)}`);
