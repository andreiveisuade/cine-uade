const BASE = "/api";

async function pedir(ruta, opciones = {}) {
  const cabeceras = { ...(opciones.cuerpo ? { "Content-Type": "application/json" } : {}), ...opciones.cabeceras };

  let respuesta;
  try {
    // Sin `credentials`: el default (same-origin) manda la cookie de la sesión del cliente.
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

// El {"error"} del backend (y de nginx, que también contesta en JSON) va tal cual. Otra cosa (texto, cuerpo vacío)
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

export const obtenerFuncionesDePelicula = (peliculaId) => get(`/peliculas/${peliculaId}/funciones`);

export const obtenerFuncion = (id, sesion) =>
  get(`/funciones/${id}` + (sesion ? `?sesion=${encodeURIComponent(sesion)}` : ""));

export const bloquearButacas = ({ funcionId, sesion, butacas }) =>
  post(`/funciones/${Number(funcionId)}/bloqueos`, { sesion, butacas });

export const registrarCliente = ({ nombre, email, password, codigoReserva }) =>
  post("/clientes", { nombre, email, password, codigoReserva });

// Basic solo para entrar: después la sesión vive en la cookie HttpOnly y el front no guarda la clave.
// TextEncoder porque btoa no acepta una clave con tildes o eñes.
export const ingresar = (email, clave) =>
  pedir("/sesion", {
    metodo: "POST",
    cabeceras: { Authorization: "Basic " + btoa(String.fromCharCode(...new TextEncoder().encode(`${email}:${clave}`))) },
  });

// Sin Basic, el backend contesta con quien tenga la cookie, o 401 si no hay sesión.
export const sesionActual = () => post("/sesion");

export const salir = () => pedir("/sesion", { metodo: "DELETE" });

export const modificarCuenta = ({ nombre }) => pedir("/cuenta", { metodo: "PUT", cuerpo: { nombre } });

// El backend rota la cookie al cambiar la clave: la sesión sigue sin volver a entrar.
export const cambiarClave = ({ actual, nueva }) => post("/cuenta/clave", { actual, nueva });

export const darDeBaja = ({ actual }) => pedir("/cuenta", { metodo: "DELETE", cuerpo: { actual } });

export const recuperarClave = ({ email, codigoReserva, nueva }) =>
  post("/cuenta/recuperacion", { email, codigoReserva, nueva });

export const obtenerMisReservas = () => get("/reservas/mias");

export const obtenerMediosDePago = () => get("/medios-pago");

export const abrirCheckout = (codigo, medio) =>
  post(`/reservas/codigo/${encodeURIComponent(codigo)}/checkout`, { medio });

// La vuelta del comprador sin salir del ticket: el backend consulta MP y cobra si aprobó. El 302 al ticket no se
// sigue (ya estamos ahí) y una falla no importa: la conciliación cobra igual.
export const avisarVueltaDelPago = (checkoutId) =>
  fetch(`${BASE}/checkouts/${encodeURIComponent(checkoutId)}/retorno`, { redirect: "manual" })
    .then(() => {}, () => {});

export const crearReserva = ({ funcionId, nombre, email, butacas, sesion }) =>
  post("/reservas", { funcionId: Number(funcionId), nombre, email, butacas, sesion });

export const obtenerReservaPorCodigo = (codigo) =>
  get(`/reservas/codigo/${encodeURIComponent(codigo)}`);

export const cancelarReservaPorCodigo = (codigo) =>
  post(`/reservas/codigo/${encodeURIComponent(codigo)}/cancelacion`);

export const obtenerPromocionesVigentes = () => get("/promociones/vigentes");

export const obtenerProductosCandy = (todos = false) =>
  get(`/candy/productos${todos ? "?todos=true" : ""}`);
