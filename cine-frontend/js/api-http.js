// Implementación contra la API REST del backend, siguiendo API.md.
//
// La base es relativa: el navegador pide /api al mismo origen del que bajó la página y
// nginx lo reenvía al backend por la red interna de Docker. Por eso acá no aparece ni
// el host ni el puerto del backend, y no hace falta CORS.

const BASE = "/api";

/* ------------------------------------------------------------- credenciales */

// El backend no guarda sesión: cada pedido del encargado lleva `Authorization: Basic`.
// Se guardan acá y no en admin/sesion.js porque el único que las usa es `pedir`, y así
// ningún módulo del panel tiene que acordarse de mandarlas. sessionStorage y no
// localStorage: se van al cerrar la pestaña, igual que la sesión.
const CLAVE_CREDENCIALES = "cine.credenciales";

// btoa solo acepta Latin-1: una contraseña con "ñ" rompería sin pasar antes por UTF-8,
// que es como la decodifica Spring.
const base64 = (texto) => btoa(String.fromCharCode(...new TextEncoder().encode(texto)));

export function olvidarCredenciales() {
  sessionStorage.removeItem(CLAVE_CREDENCIALES);
}

/**
 * Los errores de validación vienen con 400 y {"error": "..."}: ese texto es el mensaje
 * que tiran los gestores y se muestra tal cual, así que se propaga como Error.
 */
async function pedir(ruta, opciones = {}) {
  // El login no manda las viejas: si quedaron vencidas, el filtro lo rechazaría antes de
  // llegar a comprobar las nuevas.
  const credenciales = ruta === "/sesion" ? null : sessionStorage.getItem(CLAVE_CREDENCIALES);
  const cabeceras = opciones.cuerpo ? { "Content-Type": "application/json" } : {};
  if (credenciales) cabeceras.Authorization = `Basic ${credenciales}`;

  let respuesta;
  try {
    respuesta = await fetch(BASE + ruta, {
      headers: cabeceras,
      method: opciones.metodo || "GET",
      body: opciones.cuerpo ? JSON.stringify(opciones.cuerpo) : undefined,
      // `fetch` no tiene timeout propio: sin esto, un pedido que no vuelve deja el botón
      // girando para siempre. Solo lo usa la importación, que es la única que puede
      // tardar minutos; el resto contesta o falla enseguida.
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

  // Un 401 fuera del login es que no hay credenciales válidas: vencieron —la clave cambió,
  // el empleado se borró— o la sesión es de antes de que el backend las pidiera. Se olvidan
  // y se avisa; el panel escucha el evento y vuelve al login. El 401 del propio login es
  // una clave mal tipeada y se muestra como cualquier error.
  if (respuesta.status === 401 && ruta !== "/sesion") {
    olvidarCredenciales();
    window.dispatchEvent(new Event("cine:sesion-vencida"));
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

// `sesion` es opcional y solo cambia una cosa: las butacas que esa sesión tiene
// bloqueadas mientras elige no vuelven marcadas como ocupadas para ella misma. Se
// resuelve en el backend y no acá para que "ocupado" tenga una sola definición.
export const obtenerFuncion = (id, sesion) => get(`/funciones/${id}${consulta({ sesion })}`);

// La selección entera, no una butaca suelta: una sola llamada toma lo nuevo, renueva lo
// que sigue elegido y suelta lo que se deseleccionó. Con butacas en [] suelta todo.
export const bloquearButacas = ({ funcionId, sesion, butacas }) =>
  post(`/funciones/${Number(funcionId)}/bloqueos`, { sesion, butacas });

export const registrarCliente = ({ nombre, email }) => post("/clientes", { nombre, email });

export const buscarClientePorEmail = (email) =>
  get(`/clientes?email=${encodeURIComponent(String(email || "").trim())}`);

// butacas es { "C5": "GENERAL", "C6": "JUBILADO" }: la tarifa es por persona, así que
// va por butaca y no por reserva.
export const crearReserva = ({ funcionId, nombre, email, butacas, sesion }) =>
  post("/reservas", { funcionId: Number(funcionId), nombre, email, butacas, sesion });

export const obtenerReserva = (id) => get(`/reservas/${id}`);

export const obtenerReservasDe = (email) =>
  get(`/reservas?email=${encodeURIComponent(String(email || "").trim())}`);

/* ---------------------------------------------------------------- encargado */

// Si el backend acepta, las mismas credenciales van en cada pedido que siga.
export async function login(email, password) {
  const empleado = await post("/sesion", { email, password });
  sessionStorage.setItem(CLAVE_CREDENCIALES, base64(`${email}:${password}`));
  return empleado;
}

/** @param filtros {q, genero, publicada} */
export const obtenerPeliculas = (filtros) => get(`/peliculas${consulta(filtros)}`);

/** El buzón: lo que trajo el importador y todavía nadie miró. */
export const obtenerPeliculasPendientes = () => get("/peliculas/pendientes");

export const confirmarPelicula = (id) => post(`/peliculas/${id}/confirmacion`, {});

export const descartarPelicula = (id) => post(`/peliculas/${id}/descarte`, {});

/* ------------------------------------------------------------- el importador */

/**
 * Trae cartelera nueva de TMDB, ahora. La respuesta llega recién cuando la corrida
 * terminó —diez o quince segundos— y ya trae los contadores: no hay que volver a
 * preguntar. Tres minutos de paciencia, uno más que el techo del backend.
 */
export const importarAhora = (paginas) =>
  pedir("/importaciones", { metodo: "POST", cuerpo: { paginas: Number(paginas) || 1 },
                            espera: 180000 });

/** Las últimas corridas, de la más nueva a la más vieja. */
export const obtenerImportaciones = () => get("/importaciones");

/** Si el importador está levantado, para avisar antes de que alguien apriete el botón. */
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

/* -------------------------------------------- armado automático de la grilla */

// Mismo cuerpo para los dos, igual que en las programaciones: la propuesta tiene que
// predecir lo que hace el alta. Los campos vacíos no viajan —el backend tiene un default
// para cada uno— y por eso van sin Number() cuando están en blanco.
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

/* ------------------------------------------------------ informes por función */

export const obtenerBordero = (funcionId) => get(`/funciones/${funcionId}/bordero`);

// POST porque escribe: emite el archivo que se sube al INCAA. Consultar el borderó dos
// veces no es lo mismo que declararlo dos veces.
export const emitirBordero = (funcionId) => post(`/funciones/${funcionId}/bordero`);

export const obtenerInformeDeFuncion = (funcionId) => get(`/funciones/${funcionId}/informe`);

/** @param filtros {estado, dia, q} */
export const obtenerReservas = (filtros) => get(`/reservas${consulta(filtros)}`);
export const cancelarReserva = (id) => post(`/reservas/${id}/cancelacion`);

export const cobrar = (reservaId, medio, codigoAutorizacion) =>
  post(`/reservas/${reservaId}/pago`, { medio, codigoAutorizacion });

export const obtenerPagoDeReserva = (reservaId) => get(`/reservas/${reservaId}/pago`);

// El camino de los medios electrónicos: el código de autorización que pide R11 no lo
// tipea nadie, lo devuelve el procesador al confirmar. El efectivo no pasa por acá.
export const abrirCheckout = (reservaId, medio) =>
  post(`/reservas/${reservaId}/checkout`, { medio });

// Qué se está pagando sale del checkout y no de quien confirma: si la reserva fuera dato
// de entrada, se podría autorizar un checkout y aplicarlo a otra reserva.
export const confirmarCheckout = (checkoutId) =>
  post(`/checkouts/${encodeURIComponent(checkoutId)}/confirmacion`);

export const obtenerArqueo = (fecha) => get(`/arqueo?fecha=${encodeURIComponent(fecha)}`);

/* -------------------------------------------------------------- promociones */

export const obtenerPromociones = () => get("/promociones");
export const crearPromocion = (promocion) => post("/promociones", promocion);
export const darDeBajaPromocion = (id) => post(`/promociones/${id}/baja`);
export const darDeAltaPromocion = (id) => post(`/promociones/${id}/alta`);

/* -------------------------------------------------------------------- candy */

// Sin `todos` es la carta que ve el cliente: solo lo que está a la venta.
export const obtenerProductosCandy = (todos = false) =>
  get(`/candy/productos${todos ? "?todos=true" : ""}`);

export const crearProductoCandy = ({ nombre, tipo, precio }) =>
  post("/candy/productos", { nombre, tipo, precio: Number(precio) });

// componentes es { productoId: cantidad }. Si el combo no sale menos que sus componentes
// sueltos (R14), el 400 trae el precio de referencia en el mensaje.
export const armarComboCandy = ({ nombre, precio, componentes }) =>
  post("/candy/combos", { nombre, precio: Number(precio), componentes });

export const editarProductoCandy = (id, { nombre, precio }) =>
  put(`/candy/productos/${id}`, { nombre, precio: Number(precio) });

export const cambiarDisponibilidadCandy = (id, disponible) =>
  put(`/candy/productos/${id}/disponibilidad`, { disponible });

// El total no viaja: lo calcula el backend desde la carta, igual que el monto de un pago.
export const venderCandy = ({ clienteId, reservaId, cantidades, medio, codigoAutorizacion }) =>
  post("/candy/compras", {
    clienteId: clienteId ? Number(clienteId) : null,
    reservaId: reservaId ? Number(reservaId) : null,
    cantidades, medio, codigoAutorizacion,
  });

/** @param filtros {fecha, clienteId}; con clienteId gana el cliente. */
export const obtenerComprasCandy = (filtros) => get(`/candy/compras${consulta(filtros)}`);

export const obtenerArqueoCandy = (fecha) => get(`/candy/arqueo?fecha=${encodeURIComponent(fecha)}`);

/* ----------------------------------------------------------- control de acceso */

// POST y no GET porque marca la entrada como usada: repetirlo falla a propósito.
export const validarEntrada = (codigo) => post("/acceso", { codigo });
