const BASE = "/api";

async function pedir(ruta, opciones = {}) {
  const cabeceras = opciones.cuerpo ? { "Content-Type": "application/json" } : {};

  let respuesta;
  try {
    respuesta = await fetch(BASE + ruta, {
      headers: cabeceras,
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
      // Un error sin JSON lo resuelve mensajeDeError; un 2xx que no se puede leer no tiene con qué seguir.
      if (respuesta.ok) throw new Error("El servidor devolvió una respuesta que no se pudo leer");
    }
  }

  if (!respuesta.ok) {
    const error = new Error(mensajeDeError(respuesta.status, datos, texto));
    // La pantalla decide por el código, no por el texto. Al reservar, el 409 es solo la butaca que ganó otra
    // compra (vendida o bloqueada por otra sesión): el resto de lo que se rechaza, como el email de un empleado, es 400.
    error.status = respuesta.status;
    throw error;
  }
  return datos;
}

// El {"error"} del backend va tal cual. Otra cosa (el HTML de nginx con el backend reiniciando, texto, cuerpo vacío)
// no se muestra: va a la consola para depurar y en pantalla queda un mensaje según el código, como en Swing.
function mensajeDeError(status, datos, texto) {
  if (datos?.error) return datos.error;
  if (texto) console.error(`Respuesta ${status} sin mensaje de error en JSON:`, texto);
  if (status >= 502 && status <= 504) return "El servidor no está disponible: probá de nuevo en un momento";
  if (status >= 500) return "Algo salió mal en el servidor: probá de nuevo en un momento";
  return `El servidor respondió con un error (código ${status})`;
}

const get = (ruta) => pedir(ruta);
const post = (ruta, cuerpo) => pedir(ruta, { metodo: "POST", cuerpo });

export const obtenerGeneros = () => get("/generos");
export const obtenerTarifas = () => get("/tarifas");

export const obtenerCartelera = (genero) =>
  get("/cartelera" + (genero ? `?genero=${encodeURIComponent(genero)}` : ""));

export const obtenerPelicula = (id) => get(`/peliculas/${id}`);
function consulta(filtros) {
  const partes = Object.entries(filtros || {})
    .filter(([, valor]) => valor !== null && valor !== undefined && String(valor).trim() !== "")
    .map(([clave, valor]) => `${clave}=${encodeURIComponent(String(valor).trim())}`);
  return partes.length ? `?${partes.join("&")}` : "";
}

export const obtenerFuncionesDePelicula = (peliculaId) => get(`/peliculas/${peliculaId}/funciones`);

export const obtenerFuncion = (id, sesion) => get(`/funciones/${id}${consulta({ sesion })}`);

export const bloquearButacas = ({ funcionId, sesion, butacas }) =>
  post(`/funciones/${Number(funcionId)}/bloqueos`, { sesion, butacas });

export const registrarCliente = ({ nombre, email }) => post("/clientes", { nombre, email });

export const crearReserva = ({ funcionId, nombre, email, butacas, sesion }) =>
  post("/reservas", { funcionId: Number(funcionId), nombre, email, butacas, sesion });

export const obtenerReservaPorCodigo = (codigo) =>
  get(`/reservas/codigo/${encodeURIComponent(codigo)}`);

export const cancelarReservaPorCodigo = (codigo) =>
  post(`/reservas/codigo/${encodeURIComponent(codigo)}/cancelacion`);

export const obtenerProductosCandy = (todos = false) =>
  get(`/candy/productos${todos ? "?todos=true" : ""}`);
