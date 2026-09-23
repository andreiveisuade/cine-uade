const BASE = "/api";

// El backend no guarda sesión: cada pedido lleva Authorization: Basic. sessionStorage: se va al cerrar la pestaña.
const CLAVE_CREDENCIALES = "cine.credenciales";

// btoa solo acepta Latin-1: una contraseña con "ñ" rompería sin pasar antes por UTF-8.
const base64 = (texto) => btoa(String.fromCharCode(...new TextEncoder().encode(texto)));

export function olvidarCredenciales() {
  sessionStorage.removeItem(CLAVE_CREDENCIALES);
}

async function pedir(ruta, opciones = {}) {
  // El login no manda las viejas: si vencieron, el filtro lo rechazaría antes de comprobar las nuevas.
  const credenciales = ruta === "/sesion" ? null : sessionStorage.getItem(CLAVE_CREDENCIALES);
  const cabeceras = opciones.cuerpo ? { "Content-Type": "application/json" } : {};
  if (credenciales) cabeceras.Authorization = `Basic ${credenciales}`;

  let respuesta;
  try {
    respuesta = await fetch(BASE + ruta, {
      headers: cabeceras,
      method: opciones.metodo || "GET",
      body: opciones.cuerpo ? JSON.stringify(opciones.cuerpo) : undefined,
      // fetch no tiene timeout propio; solo lo usa la importación, que puede tardar minutos.
      signal: opciones.espera ? AbortSignal.timeout(opciones.espera) : undefined,
    });
  } catch (e) {
    if (e.name === "TimeoutError") {
      throw new Error("El servidor tardó demasiado en responder");
    }
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

  // 401 fuera del login: credenciales vencidas. Se olvidan y el panel vuelve al login.
  if (respuesta.status === 401 && ruta !== "/sesion") {
    olvidarCredenciales();
    window.dispatchEvent(new Event("cine:sesion-vencida"));
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
const put = (ruta, cuerpo) => pedir(ruta, { metodo: "PUT", cuerpo });
const borrar = (ruta) => pedir(ruta, { metodo: "DELETE" });

export const obtenerGeneros = () => get("/generos");
export const obtenerClasificaciones = () => get("/clasificaciones");
export const obtenerTiposSala = () => get("/tipos-sala");
export const obtenerIdiomas = () => get("/idiomas");
export const obtenerProyecciones = () => get("/proyecciones");
export const obtenerMediosPago = () => get("/medios-pago");
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

export const buscarClientePorEmail = (email) =>
  get(`/clientes?email=${encodeURIComponent(String(email || "").trim())}`);

export const crearReserva = ({ funcionId, nombre, email, butacas, sesion }) =>
  post("/reservas", { funcionId: Number(funcionId), nombre, email, butacas, sesion });

export const obtenerReservaPorCodigo = (codigo) =>
  get(`/reservas/codigo/${encodeURIComponent(codigo)}`);

export const cancelarReservaPorCodigo = (codigo) =>
  post(`/reservas/codigo/${encodeURIComponent(codigo)}/cancelacion`);

export const obtenerReservasDe = (email) =>
  get(`/reservas?email=${encodeURIComponent(String(email || "").trim())}`);

export async function login(email, password) {
  const empleado = await post("/sesion", { email, password });
  sessionStorage.setItem(CLAVE_CREDENCIALES, base64(`${email}:${password}`));
  return empleado;
}

export const obtenerPeliculas = (filtros) => get(`/peliculas${consulta(filtros)}`);

export const obtenerPeliculasPendientes = () => get("/peliculas/pendientes");

export const confirmarPelicula = (id) => post(`/peliculas/${id}/confirmacion`, {});

export const descartarPelicula = (id) => post(`/peliculas/${id}/descarte`, {});

export const importarAhora = (paginas) =>
  pedir("/importaciones", { metodo: "POST", cuerpo: { paginas: Number(paginas) || 1 },
                            espera: 180000 });

export const obtenerImportaciones = () => get("/importaciones");

export const estadoImportador = () => get("/importaciones/estado");

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
                            codigosVip, codigosPareja, codigosAccesibles, minutosLimpieza }) =>
  post("/salas", { nombre, tipo, butacasPorFila,
                   codigosVip, codigosPareja, codigosAccesibles, minutosLimpieza });

export const eliminarSala = (id) => borrar(`/salas/${id}`);

export const cambiarEstadoAsiento = (salaId, codigo, estado) =>
  put(`/salas/${salaId}/asientos/${encodeURIComponent(String(codigo).trim().toUpperCase())}`,
    { estado });

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

export const obtenerProgramaciones = (filtros) => get(`/programaciones${consulta(filtros)}`);
export const obtenerProgramacion = (id) => get(`/programaciones/${id}`);

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

const cuerpoGrilla = ({ desde, dias, apertura, cierre,
                        cuantasPeliculas, precio, idioma, proyeccion }) => ({
  desde: desde || null,
  dias: dias ? Number(dias) : null,
  apertura: apertura || null,
  cierre: cierre || null,
  cuantasPeliculas: cuantasPeliculas ? Number(cuantasPeliculas) : null,
  precio: precio ? Number(precio) : null,
  idioma, proyeccion,
});

export const proponerGrilla = (criterios) => post("/grilla/propuesta", cuerpoGrilla(criterios));
export const armarGrilla = (criterios) => post("/grilla", cuerpoGrilla(criterios));

export const obtenerBordero = (funcionId) => get(`/funciones/${funcionId}/bordero`);

export const emitirBordero = (funcionId) => post(`/funciones/${funcionId}/bordero`);

export const obtenerInformeDeFuncion = (funcionId) => get(`/funciones/${funcionId}/informe`);

export const obtenerReservas = (filtros) => get(`/reservas${consulta(filtros)}`);
export const cancelarReserva = (id) => post(`/reservas/${id}/cancelacion`);

export const cobrar = (reservaId, medio, codigoAutorizacion) =>
  post(`/reservas/${reservaId}/pago`, { medio, codigoAutorizacion });

export const obtenerPagoDeReserva = (reservaId) => get(`/reservas/${reservaId}/pago`);

export const abrirCheckout = (reservaId, medio) =>
  post(`/reservas/${reservaId}/checkout`, { medio });

export const confirmarCheckout = (checkoutId) =>
  post(`/checkouts/${encodeURIComponent(checkoutId)}/confirmacion`);

export const obtenerArqueo = (fecha) => get(`/arqueo?fecha=${encodeURIComponent(fecha)}`);

export const obtenerPromociones = () => get("/promociones");
export const crearPromocion = (promocion) => post("/promociones", promocion);
export const darDeBajaPromocion = (id) => post(`/promociones/${id}/baja`);
export const darDeAltaPromocion = (id) => post(`/promociones/${id}/alta`);

export const obtenerProductosCandy = (todos = false) =>
  get(`/candy/productos${todos ? "?todos=true" : ""}`);

export const crearProductoCandy = ({ nombre, tipo, precio }) =>
  post("/candy/productos", { nombre, tipo, precio: Number(precio) });

export const armarComboCandy = ({ nombre, precio, componentes }) =>
  post("/candy/combos", { nombre, precio: Number(precio), componentes });

export const editarProductoCandy = (id, { nombre, precio }) =>
  put(`/candy/productos/${id}`, { nombre, precio: Number(precio) });

export const cambiarDisponibilidadCandy = (id, disponible) =>
  put(`/candy/productos/${id}/disponibilidad`, { disponible });

export const venderCandy = ({ clienteId, reservaId, cantidades, medio, codigoAutorizacion }) =>
  post("/candy/compras", {
    clienteId: clienteId ? Number(clienteId) : null,
    reservaId: reservaId ? Number(reservaId) : null,
    cantidades, medio, codigoAutorizacion,
  });

export const obtenerComprasCandy = (filtros) => get(`/candy/compras${consulta(filtros)}`);

export const obtenerArqueoCandy = (fecha) => get(`/candy/arqueo?fecha=${encodeURIComponent(fecha)}`);

export const validarEntrada = (codigo) => post("/acceso", { codigo });
