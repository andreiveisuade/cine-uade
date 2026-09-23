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
      if (!respuesta.ok) throw new Error(texto);
      throw new Error("El servidor devolvió una respuesta que no se pudo leer");
    }
  }

  if (!respuesta.ok) {
    const error = new Error(datos?.error || `Error ${respuesta.status} del servidor`);
    // 409 es una carrera por la butaca, distinto del 400.
    error.status = respuesta.status;
    throw error;
  }
  return datos;
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
