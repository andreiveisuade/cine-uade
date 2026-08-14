// Único punto de acceso a datos. Hoy resuelve contra el mock en memoria;
// cuando exista la API REST se reemplaza el cuerpo de estas funciones por fetch
// y no cambia nada del resto del frontend.

import * as datos from "../mock/datos.js";

const DEMORA_MS = 120; // simula la latencia de una llamada real

function responder(valor) {
  return new Promise((resolve) => {
    setTimeout(() => resolve(structuredClone(valor)), DEMORA_MS);
  });
}

function fallar(mensaje) {
  return Promise.reject(new Error(mensaje));
}

function siguienteId(lista) {
  return lista.reduce((max, item) => Math.max(max, item.id), 0) + 1;
}

// precio base de la función × multiplicador de sala × multiplicador de butaca
const TARIFAS = {
  GENERAL: { multiplicador: 1.0, requiereAcreditacion: false },
  MENOR: { multiplicador: 0.6, requiereAcreditacion: true },
  JUBILADO: { multiplicador: 0.5, requiereAcreditacion: true },
  ESTUDIANTE: { multiplicador: 0.7, requiereAcreditacion: true },
};

function precioDeAsiento(funcion, sala, asiento, tarifa = "GENERAL") {
  return redondear(funcion.precio
    * datos.TIPOS_SALA[sala.tipo].multiplicador
    * datos.TIPOS_ASIENTO[asiento.tipo].multiplicador
    * (TARIFAS[tarifa] || TARIFAS.GENERAL).multiplicador);
}

function redondear(monto) {
  return Math.round(monto * 100) / 100;
}

const dosDigitos = (n) => String(n).padStart(2, "0");

/** El momento actual como lo manda el backend: ISO local sin zona. */
function ahoraISO() {
  const d = new Date();
  return `${d.getFullYear()}-${dosDigitos(d.getMonth() + 1)}-${dosDigitos(d.getDate())}`
    + `T${dosDigitos(d.getHours())}:${dosDigitos(d.getMinutes())}:${dosDigitos(d.getSeconds())}`;
}

// Mismo alfabeto que el backend: sin O, I, 0 ni 1, que se confunden al tipearlos.
function generarCodigo() {
  const alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let codigo = "";
  for (let i = 0; i < 8; i++) {
    codigo += alfabeto.charAt(Math.floor(Math.random() * alfabeto.length));
  }
  return codigo;
}

/*
 * Los bloqueos de mientras alguien elige, que en el backend viven en Redis. Acá van en
 * memoria porque el mock es un solo navegador: no alcanza para dos personas peleando una
 * butaca, pero sí para que la pantalla se comporte igual que contra la API, que es para
 * lo que el modo mock existe.
 */
const SEGUNDOS_ELIGIENDO = 180;
const bloqueos = new Map(); // "funcionId:asientoId" -> { sesion, vence }

/** Las butacas tomadas en esa función, con la sesión que tiene cada una. */
function bloqueadas(funcionId) {
  const tomadas = new Map();
  for (const [clave, bloqueo] of bloqueos) {
    if (bloqueo.vence <= Date.now()) {
      bloqueos.delete(clave);
    } else if (clave.startsWith(`${funcionId}:`)) {
      tomadas.set(Number(clave.slice(String(funcionId).length + 1)), bloqueo.sesion);
    }
  }
  return tomadas;
}

// Un asiento no está ocupado en sí mismo: lo está en una función, si alguna
// reserva no cancelada de esa función lo tomó, o si alguien la está eligiendo.
// Las que bloqueó `sesion` no le están ocupadas a ella.
function asientosOcupados(funcionId, sesion) {
  const ocupados = new Set(
    datos.reservas
      .filter((r) => r.funcionId === funcionId && r.estado !== "CANCELADA")
      .flatMap((r) => r.entradas.map((e) => e.asientoId)),
  );
  for (const [asientoId, duenio] of bloqueadas(funcionId)) {
    if (duenio !== sesion) ocupados.add(asientoId);
  }
  return ocupados;
}

function salaDe(salaId) {
  return datos.salas.find((s) => s.id === salaId);
}

function peliculaDe(peliculaId) {
  return datos.peliculas.find((p) => p.id === peliculaId);
}

function conPeliculaYSala(funcion) {
  return {
    ...funcion,
    pelicula: peliculaDe(funcion.peliculaId),
    sala: salaDe(funcion.salaId),
    // la butaca más barata de la sala, para el "desde" de la cartelera
    precioDesde: funcion.precio * datos.TIPOS_SALA[salaDe(funcion.salaId).tipo].multiplicador,
  };
}

/* ---------------------------------------------------------------- catálogos */

export function obtenerGeneros() {
  return responder(datos.GENEROS);
}

/** Cada clasificación con su edad mínima. */
export function obtenerClasificaciones() {
  return responder(Object.entries(datos.CLASIFICACIONES)
    .map(([nombre, edadMinima]) => ({ nombre, edadMinima })));
}

/** Cada tipo con su multiplicador y si puede proyectar en 3D, para anticipar R8. */
export function obtenerTiposSala() {
  return responder(Object.entries(datos.TIPOS_SALA)
    .map(([nombre, tipo]) => ({ nombre, ...tipo })));
}

export function obtenerIdiomas() {
  return responder(datos.IDIOMAS);
}

export function obtenerProyecciones() {
  return responder(datos.PROYECCIONES);
}

/* ------------------------------------------------------------------ cliente */

/**
 * Solo lo que está en exhibición: una película cargada no está necesariamente en
 * cartelera. Opcionalmente filtra por género (CU-01b).
 */
export function obtenerCartelera(genero) {
  let lista = datos.peliculas.filter((p) => p.enCartelera);
  if (genero) lista = lista.filter((p) => p.generos.includes(genero));
  return responder(lista);
}

export function obtenerPelicula(id) {
  const pelicula = peliculaDe(Number(id));
  return pelicula ? responder(pelicula) : fallar(`No existe la película ${id}`);
}

export function obtenerFuncionesDePelicula(peliculaId) {
  const lista = datos.funciones
    .filter((f) => f.peliculaId === Number(peliculaId))
    .map(conPeliculaYSala)
    .sort((a, b) => a.inicio.localeCompare(b.inicio));
  return responder(lista);
}

/** La función con su sala dibujable: cada butaca con su tipo, estado, precio y si está ocupada. */
export function obtenerFuncion(id, sesion) {
  const funcion = datos.funciones.find((f) => f.id === Number(id));
  if (!funcion) return fallar(`No existe la función ${id}`);

  const sala = salaDe(funcion.salaId);
  const ocupados = asientosOcupados(funcion.id, sesion);
  const butacas = datos.asientos
    .filter((a) => a.salaId === sala.id)
    .map((a) => ({
      ...a,
      ocupado: ocupados.has(a.id),
      precio: precioDeAsiento(funcion, sala, a),
    }));

  return responder({
    ...conPeliculaYSala(funcion),
    asientos: butacas,
    libres: butacas.filter((a) => !a.ocupado && a.estado !== "FUERA_DE_SERVICIO").length,
  });
}

/** Alta de cliente (CU-05). El email identifica al cliente, así que es único. */
export function registrarCliente({ nombre, email }) {
  const nombreLimpio = (nombre || "").trim();
  const emailLimpio = (email || "").trim();
  if (!nombreLimpio) return fallar("El nombre no puede estar vacío");
  if (!emailLimpio.includes("@")) return fallar("El email no es válido");
  if (datos.clientes.some((c) => c.email.toLowerCase() === emailLimpio.toLowerCase())) {
    return fallar("Ya hay un cliente registrado con ese email");
  }
  const cliente = {
    id: siguienteId([...datos.clientes, ...datos.administradores]),
    nombre: nombreLimpio,
    email: emailLimpio,
    rol: "CLIENTE",
  };
  datos.clientes.push(cliente);
  return responder(cliente);
}

/** Para reconocer a quien ya se registró y no pedirle los datos de nuevo. */
export function buscarClientePorEmail(email) {
  const buscado = String(email || "").trim().toLowerCase();
  return responder(
    datos.clientes.find((c) => c.email.toLowerCase() === buscado) || null);
}

/**
 * Crea la reserva con las butacas elegidas. Si el email no existe, da de alta el cliente.
 * Replica las validaciones de GestorReservas.
 */
export function crearReserva({ funcionId, nombre, email, butacas, sesion }) {
  const funcion = datos.funciones.find((f) => f.id === Number(funcionId));
  if (!funcion) return fallar(`No existe la función ${funcionId}`);
  if (!nombre || !nombre.trim()) return fallar("Falta el nombre del cliente");
  if (!email || !email.includes("@")) return fallar("El email no es válido");
  const pedido = Object.entries(butacas || {});
  if (pedido.length === 0) return fallar("Hay que elegir al menos una butaca");

  const sala = salaDe(funcion.salaId);
  const deLaSala = datos.asientos.filter((a) => a.salaId === sala.id);
  const ocupados = asientosOcupados(funcion.id, sesion);

  // Al venir como mapa, una butaca repetida es imposible de expresar: no hay que validarla.
  const entradas = [];
  for (const [codigo, tarifa] of pedido) {
    const buscado = String(codigo).trim().toUpperCase();
    const asiento = deLaSala.find((a) => a.codigo === buscado);
    if (!asiento) return fallar(`La butaca ${buscado} no existe en esa sala`);
    if (asiento.estado === "FUERA_DE_SERVICIO") return fallar(`La butaca ${buscado} está fuera de servicio`);
    if (ocupados.has(asiento.id)) return fallar(`La butaca ${buscado} ya está ocupada`);
    entradas.push({
      asientoId: asiento.id,
      codigo: asiento.codigo,
      tarifa: tarifa || "GENERAL",
      precio: precioDeAsiento(funcion, sala, asiento, tarifa),
    });
  }

  let cliente = datos.clientes.find((c) => c.email.toLowerCase() === email.trim().toLowerCase());
  if (!cliente) {
    cliente = {
      id: siguienteId([...datos.clientes, ...datos.administradores]),
      nombre: nombre.trim(),
      email: email.trim(),
      rol: "CLIENTE",
    };
    datos.clientes.push(cliente);
  }

  const reserva = {
    id: siguienteId(datos.reservas),
    funcionId: funcion.id,
    clienteId: cliente.id,
    estado: "RESERVADA",
    codigo: generarCodigo(),
    ingresadaEn: null,
    entradas,
  };
  datos.reservas.push(reserva);
  // Guardada la reserva, el bloqueo cumplió su etapa: de acá en adelante la butaca la
  // retiene la reserva. Suelta también las que se miraron y no se compraron.
  liberarBloqueos(funcion.id, sesion);
  return responder(reserva);
}

/**
 * Lo que esa sesión tiene elegido en el mapa. Toma lo nuevo, renueva lo que sigue elegido
 * y suelta lo que se deseleccionó; con `butacas` en [] suelta todo.
 *
 * Que una butaca no se consiga no es un error: es que otro llegó primero, y las demás sí
 * se consiguieron. Por eso las rechazadas viajan en la misma respuesta.
 */
export function bloquearButacas({ funcionId, sesion, butacas }) {
  const funcion = datos.funciones.find((f) => f.id === Number(funcionId));
  if (!funcion) return fallar(`No existe la función ${funcionId}`);
  if (!sesion) return fallar("Hace falta una sesión para bloquear butacas");

  const deLaSala = datos.asientos.filter((a) => a.salaId === funcion.salaId);
  const ocupados = asientosOcupados(funcion.id, sesion);
  const pedidas = (butacas || []).map((codigo) => String(codigo).trim().toUpperCase());

  const conseguidas = [];
  const sigueEligiendo = new Set();
  for (const codigo of pedidas) {
    const asiento = deLaSala.find((a) => a.codigo === codigo);
    if (!asiento) return fallar(`La butaca ${codigo} no existe en esa sala`);
    sigueEligiendo.add(asiento.id);
    if (ocupados.has(asiento.id)) continue;
    bloqueos.set(`${funcion.id}:${asiento.id}`,
      { sesion, vence: Date.now() + SEGUNDOS_ELIGIENDO * 1000 });
    conseguidas.push(codigo);
  }
  liberarBloqueos(funcion.id, sesion, (asientoId) => !sigueEligiendo.has(asientoId));

  return responder({
    sesion,
    butacas: conseguidas,
    rechazadas: pedidas.filter((codigo) => !conseguidas.includes(codigo)),
    vencenEnSegundos: SEGUNDOS_ELIGIENDO,
  });
}

/** Suelta los bloqueos de esa sesión en esa función, solo los suyos. */
function liberarBloqueos(funcionId, sesion, corresponde = () => true) {
  for (const [asientoId, duenio] of bloqueadas(funcionId)) {
    if (duenio === sesion && corresponde(asientoId)) bloqueos.delete(`${funcionId}:${asientoId}`);
  }
}

/** Todo lo que necesita el ticket, igual que el comprobante .txt del backend. */
export function obtenerReserva(id) {
  const reserva = datos.reservas.find((r) => r.id === Number(id));
  if (!reserva) return fallar(`No existe la reserva ${id}`);
  const funcion = datos.funciones.find((f) => f.id === reserva.funcionId);
  return responder({
    ...reserva,
    funcion,
    pelicula: peliculaDe(funcion.peliculaId),
    sala: salaDe(funcion.salaId),
    cliente: datos.clientes.find((c) => c.id === reserva.clienteId),
    total: reserva.entradas.reduce((suma, e) => suma + e.precio, 0),
  });
}

/* ----------------------------------------------------------------- encargado */

/**
 * El mensaje de error es el mismo para email inexistente y contraseña equivocada:
 * decir cuál de los dos falló confirma qué emails están registrados.
 */
export function login(email, password) {
  const admin = datos.administradores.find(
    (a) => a.email.toLowerCase() === String(email || "").trim().toLowerCase());
  if (!admin || admin.password !== password) {
    return fallar("Email o contraseña incorrectos");
  }
  const { password: _, ...sinCredenciales } = admin;
  return responder(sinCredenciales);
}

export function obtenerPeliculas() {
  return responder(datos.peliculas);
}

export function crearPelicula({ titulo, duracionMinutos, generos, clasificacion,
                                posterUrl, ...catalogo }) {
  const nombre = (titulo || "").trim();
  if (!nombre) return fallar("El título no puede estar vacío");
  if (datos.peliculas.some((p) => p.titulo.toLowerCase() === nombre.toLowerCase())) {
    return fallar("Ya existe una película con ese título");
  }
  if (!(duracionMinutos > 0)) return fallar("La duración debe ser mayor a cero");
  if (!generos || generos.length === 0) return fallar("La película necesita al menos un género");
  // R10
  if (!clasificacion || !(clasificacion in datos.CLASIFICACIONES)) {
    return fallar("Falta la clasificación por edad");
  }

  const pelicula = {
    id: siguienteId(datos.peliculas),
    titulo: nombre,
    duracionMinutos: Number(duracionMinutos),
    generos: [...generos],
    clasificacion,
    posterUrl: (posterUrl || "").trim(),
    director: (catalogo.director || "").trim(),
    anio: Number(catalogo.anio) || 0,
    idiomaOriginal: (catalogo.idiomaOriginal || "").trim(),
    sinopsis: (catalogo.sinopsis || "").trim(),
    enCartelera: catalogo.enCartelera !== false,
    // El alta del encargado nace confirmada: cargarla ya es haberla decidido.
    estadoRevision: "CONFIRMADA",
  };
  datos.peliculas.push(pelicula);
  return responder(pelicula);
}

/** El buzón: lo que trajo el importador y todavía nadie miró. */
export function obtenerPeliculasPendientes() {
  return responder(datos.peliculas.filter((p) => p.estadoRevision === "PENDIENTE"));
}

export function confirmarPelicula(id) {
  const pelicula = datos.peliculas.find((p) => p.id === Number(id));
  if (!pelicula) return fallar("No existe la película " + id);
  pelicula.estadoRevision = "CONFIRMADA";
  pelicula.enCartelera = true;
  return responder(pelicula);
}

export function descartarPelicula(id) {
  const pelicula = datos.peliculas.find((p) => p.id === Number(id));
  if (!pelicula) return fallar("No existe la película " + id);
  if (datos.funciones.some((f) => f.peliculaId === pelicula.id)) {
    return fallar("No se puede descartar una película que ya tiene funciones programadas");
  }
  // Descartada, no borrada: si se borrara, la próxima corrida del importador la traería
  // de nuevo y habría que descartarla otra vez.
  pelicula.estadoRevision = "DESCARTADA";
  pelicula.enCartelera = false;
  return responder(pelicula);
}

/** Las mismas reglas que el alta, salvo el título repetido, que se compara contra las otras. */
export function actualizarPelicula(id, cambios) {
  const pelicula = peliculaDe(Number(id));
  if (!pelicula) return fallar(`No existe la película ${id}`);

  const nombre = (cambios.titulo ?? pelicula.titulo).trim();
  if (!nombre) return fallar("El título no puede estar vacío");
  if (datos.peliculas.some((p) => p.id !== pelicula.id
      && p.titulo.toLowerCase() === nombre.toLowerCase())) {
    return fallar("Ya existe una película con ese título");
  }
  const duracion = Number(cambios.duracionMinutos ?? pelicula.duracionMinutos);
  if (!(duracion > 0)) return fallar("La duración debe ser mayor a cero");
  const generos = cambios.generos ?? pelicula.generos;
  if (!generos.length) return fallar("La película necesita al menos un género");
  const clasificacion = cambios.clasificacion ?? pelicula.clasificacion;
  if (!(clasificacion in datos.CLASIFICACIONES)) return fallar("Falta la clasificación por edad");

  Object.assign(pelicula, {
    titulo: nombre,
    duracionMinutos: duracion,
    generos: [...generos],
    clasificacion,
    posterUrl: (cambios.posterUrl ?? pelicula.posterUrl).trim(),
    director: (cambios.director ?? pelicula.director).trim(),
    anio: Number(cambios.anio ?? pelicula.anio) || 0,
    idiomaOriginal: (cambios.idiomaOriginal ?? pelicula.idiomaOriginal).trim(),
    sinopsis: (cambios.sinopsis ?? pelicula.sinopsis).trim(),
    enCartelera: cambios.enCartelera ?? pelicula.enCartelera,
  });
  return responder(pelicula);
}

export function eliminarPelicula(id) {
  const indice = datos.peliculas.findIndex((p) => p.id === Number(id));
  if (indice === -1) return fallar(`No existe la película ${id}`);
  if (datos.funciones.some((f) => f.peliculaId === Number(id))) {
    return fallar("No se puede borrar: la película tiene funciones programadas");
  }
  datos.peliculas.splice(indice, 1);
  return responder(true);
}

export function obtenerSalas() {
  return responder(datos.salas);
}

/** La sala con todas sus butacas, para dibujar el mapa del ABM. */
export function obtenerSala(id) {
  const sala = salaDe(Number(id));
  if (!sala) return fallar(`No existe la sala ${id}`);
  return responder({
    ...sala,
    asientos: datos.asientos.filter((a) => a.salaId === sala.id),
  });
}

export function crearSala({ nombre, tipo, butacasPorFila,
                            codigosVip, codigosPareja, codigosAccesibles, minutosLimpieza }) {
  const nombreSala = (nombre || "").trim();
  if (!nombreSala) return fallar("El nombre no puede estar vacío");
  if (!datos.TIPOS_SALA[tipo]) return fallar("Falta el tipo de sala");
  if (!butacasPorFila || butacasPorFila.length === 0) return fallar("La sala necesita al menos una fila");
  if (butacasPorFila.length > 26) return fallar("Máximo 26 filas: se identifican con una letra");
  if (butacasPorFila.some((b) => !(b > 0))) return fallar("Cada fila debe tener al menos una butaca");
  if (minutosLimpieza < 0) return fallar("Los minutos de limpieza no pueden ser negativos");
  if (datos.salas.some((s) => s.nombre.toLowerCase() === nombreSala.toLowerCase())) {
    return fallar("Ya existe una sala con ese nombre");
  }

  const sala = {
    id: siguienteId(datos.salas),
    nombre: nombreSala,
    tipo,
    butacasPorFila: [...butacasPorFila],
    filas: butacasPorFila.length,
    capacidadSala: butacasPorFila.reduce((a, b) => a + b, 0),
    // Ausente es el default del backend, no cero: cero significaría "encadena sin corte".
    minutosLimpieza: minutosLimpieza == null ? 15 : minutosLimpieza,
  };
  datos.salas.push(sala);

  const primerId = datos.asientos.reduce((max, a) => Math.max(max, a.id), 0) + 1;
  datos.asientos.push(...datos.generarAsientos(
    sala, codigosVip || [], codigosPareja || [], codigosAccesibles || [], primerId));
  return responder(sala);
}

export function eliminarSala(id) {
  const indice = datos.salas.findIndex((s) => s.id === Number(id));
  if (indice === -1) return fallar(`No existe la sala ${id}`);
  if (datos.funciones.some((f) => f.salaId === Number(id))) {
    return fallar("No se puede borrar: la sala tiene funciones programadas");
  }
  datos.salas.splice(indice, 1);
  for (let i = datos.asientos.length - 1; i >= 0; i--) {
    if (datos.asientos[i].salaId === Number(id)) datos.asientos.splice(i, 1);
  }
  return responder(true);
}

/** Una butaca rota deja de venderse en todas las funciones, presentes y futuras. */
export function cambiarEstadoAsiento(salaId, codigo, estado) {
  const asiento = datos.asientos.find(
    (a) => a.salaId === Number(salaId) && a.codigo === String(codigo).trim().toUpperCase());
  if (!asiento) return fallar(`La butaca ${codigo} no existe en la sala ${salaId}`);
  if (estado !== "HABILITADO" && estado !== "FUERA_DE_SERVICIO") {
    return fallar("Estado de butaca inválido");
  }
  asiento.estado = estado;
  return responder(asiento);
}

export function obtenerFunciones() {
  const lista = datos.funciones
    .map(conPeliculaYSala)
    .sort((a, b) => a.inicio.localeCompare(b.inicio));
  return responder(lista);
}

export function programarFuncion({ peliculaId, salaId, inicio, idioma, proyeccion, precio }) {
  const pelicula = peliculaDe(Number(peliculaId));
  if (!pelicula) return fallar(`No existe la película ${peliculaId}`);
  const sala = salaDe(Number(salaId));
  if (!sala) return fallar(`No existe la sala ${salaId}`);
  if (!idioma || !proyeccion) return fallar("Falta el idioma o el formato de proyección");
  // R8
  if (proyeccion === "TRES_D" && !datos.TIPOS_SALA[sala.tipo].soportaTresD) {
    return fallar(`La sala ${sala.nombre} no puede proyectar en 3D`);
  }
  if (!inicio) return fallar("Falta la fecha y hora de la función");
  if (!(precio > 0)) return fallar("El precio debe ser mayor a cero");

  // R3: dos rangos se pisan si cada uno empieza antes de que termine el otro.
  const desde = new Date(inicio);
  const hasta = new Date(desde.getTime() + pelicula.duracionMinutos * 60000);
  const pisada = datos.funciones
    .filter((f) => f.salaId === sala.id)
    .find((f) => {
      const otraDesde = new Date(f.inicio);
      const duracion = peliculaDe(f.peliculaId)?.duracionMinutos || 0;
      const otraHasta = new Date(otraDesde.getTime() + duracion * 60000);
      return desde < otraHasta && otraDesde < hasta;
    });
  if (pisada) return fallar(`La sala ${sala.nombre} ya tiene una función en ese horario`);

  const funcion = {
    id: siguienteId(datos.funciones),
    peliculaId: pelicula.id,
    salaId: sala.id,
    inicio: inicio.length === 16 ? `${inicio}:00` : inicio,
    idioma,
    proyeccion,
    precio: Number(precio),
  };
  datos.funciones.push(funcion);
  return responder(funcion);
}

export function eliminarFuncion(id) {
  const indice = datos.funciones.findIndex((f) => f.id === Number(id));
  if (indice === -1) return fallar(`No existe la función ${id}`);
  if (datos.reservas.some((r) => r.funcionId === Number(id) && r.estado !== "CANCELADA")) {
    return fallar("No se puede borrar: la función tiene reservas activas");
  }
  datos.funciones.splice(indice, 1);
  return responder(true);
}

/* ------------------------------------------------------------ programaciones */

const DIAS_ISO = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];

/**
 * Las fechas y horas que la grilla quiere ocupar. Mismo recorrido que
 * ProgramacionImpl.horarios(): día por día, quedándose con los que la grilla habilita.
 * Sin días elegidos son todos, no ninguno.
 */
/**
 * Sin `hasta` la grilla es abierta y no tiene final: se materializa hasta el horizonte,
 * los mismos 14 dias que usa GestorProgramaciones. Una grilla cerrada genera su rango
 * entero, porque el administrador ya dijo hasta cuando.
 */
function horariosDe({ desde, hasta, horaInicio, diasSemana }) {
  const momentos = [];
  const horizonte = new Date();
  horizonte.setDate(horizonte.getDate() + 14);
  const fin = hasta ? new Date(`${hasta}T00:00:00`) : horizonte;
  for (let dia = new Date(`${desde}T00:00:00`); dia <= fin; dia.setDate(dia.getDate() + 1)) {
    if (!diasSemana?.length || diasSemana.includes(DIAS_ISO[dia.getDay()])) {
      const iso = `${dia.getFullYear()}-${dos(dia.getMonth() + 1)}-${dos(dia.getDate())}`;
      momentos.push(`${iso}T${horaInicio.length === 5 ? horaInicio : horaInicio.slice(0, 5)}:00`);
    }
  }
  return momentos;
}

const dos = (n) => String(n).padStart(2, "0");

/** R3, igual que en programarFuncion: devuelve la función que se pisa, o undefined. */
function funcionSuperpuesta(salaId, inicio, duracionMinutos) {
  const desde = new Date(inicio);
  const hasta = new Date(desde.getTime() + duracionMinutos * 60000);
  return datos.funciones.filter((f) => f.salaId === salaId).find((f) => {
    const otraDesde = new Date(f.inicio);
    const otraHasta = new Date(otraDesde.getTime()
      + (peliculaDe(f.peliculaId)?.duracionMinutos || 0) * 60000);
    return desde < otraHasta && otraDesde < hasta;
  });
}

/**
 * La cuenta, una sola vez, igual que GestorProgramaciones.planificar: lo único que
 * cambia entre previsualizar y crear es si persiste.
 */
function planificar(grilla, persistir) {
  const pelicula = peliculaDe(Number(grilla.peliculaId));
  if (!pelicula) return fallar(`No existe la película ${grilla.peliculaId}`);
  const sala = salaDe(Number(grilla.salaId));
  if (!sala) return fallar(`No existe la sala ${grilla.salaId}`);
  if (!grilla.idioma || !grilla.proyeccion) return fallar("Falta el idioma o el formato de proyección");
  // R8 vale para toda la grilla: si la sala no proyecta en 3D, no hay ninguna fecha
  // del rango en la que sí, y previsualizar tiene que fallar igual que el alta.
  if (grilla.proyeccion === "TRES_D" && !datos.TIPOS_SALA[sala.tipo].soportaTresD) {
    return fallar(`La sala ${sala.nombre} no puede proyectar en 3D`);
  }
  if (!(grilla.precio > 0)) return fallar("El precio debe ser mayor a cero");
  if (!grilla.desde) return fallar("Falta la fecha de inicio");
  // hasta vacio es una grilla abierta y es valido; lo que no se admite es un rango dado
  // vuelta.
  if (grilla.hasta && grilla.hasta < grilla.desde) {
    return fallar("El rango tiene que empezar antes de terminar");
  }
  if (!grilla.horaInicio) return fallar("Falta la hora de la función");

  const horarios = horariosDe(grilla);
  if (!horarios.length) {
    return fallar("Ningún día del rango cae en los días elegidos: la grilla no generaría funciones");
  }

  const guardada = { ...grilla, id: 0, activa: true,
                     peliculaId: pelicula.id, salaId: sala.id,
                     diasSemana: grilla.diasSemana || [], precio: Number(grilla.precio) };
  if (persistir) {
    guardada.id = siguienteId(datos.programaciones);
    datos.programaciones.push(guardada);
  }

  const funciones = horarios.map((inicio) => {
    const choque = funcionSuperpuesta(sala.id, inicio, pelicula.duracionMinutos);
    if (choque) {
      return { inicio, choca: true,
               motivo: `la sala ya tiene la función ${choque.id} a las ${choque.inicio.slice(8, 10)}/${choque.inicio.slice(5, 7)} ${choque.inicio.slice(11, 16)}` };
    }
    if (persistir) {
      datos.funciones.push({
        id: siguienteId(datos.funciones),
        peliculaId: pelicula.id,
        salaId: sala.id,
        programacionId: guardada.id,
        inicio,
        idioma: grilla.idioma,
        proyeccion: grilla.proyeccion,
        precio: Number(grilla.precio),
      });
    }
    return { inicio, choca: false };
  });

  return responder({
    programacion: guardada,
    funciones,
    generadas: funciones.filter((f) => !f.choca).length,
    salteadas: funciones.filter((f) => f.choca).length,
  });
}

export function obtenerProgramaciones() {
  return responder(datos.programaciones.map((p) => ({ ...p })));
}

export function obtenerProgramacion(id) {
  const grilla = datos.programaciones.find((p) => p.id === Number(id));
  if (!grilla) return fallar(`No existe la programación ${id}`);
  return responder({
    ...grilla,
    funciones: datos.funciones
      .filter((f) => f.programacionId === grilla.id)
      .map((f) => ({ id: f.id, inicio: f.inicio })),
  });
}

export const previsualizarProgramacion = (grilla) => planificar(grilla, false);
export const crearProgramacion = (grilla) => planificar(grilla, true);

// Dar de baja no toca las funciones ya generadas: pueden tener entradas vendidas.
const cambiarEstadoProgramacion = (id, activa) => {
  const grilla = datos.programaciones.find((p) => p.id === Number(id));
  if (!grilla) return fallar(`No existe la programación ${id}`);
  grilla.activa = activa;
  return responder({ ...grilla });
};

export const darDeBajaProgramacion = (id) => cambiarEstadoProgramacion(id, false);
export const darDeAltaProgramacion = (id) => cambiarEstadoProgramacion(id, true);

/* -------------------------------------------- armado automático de la grilla */

/*
 * El planificador de la semana, con el mismo algoritmo que PlanificadorGrilla: elige el
 * elenco con un goloso que mira puntaje y géneros a la vez, y reparte los pases
 * proporcionalmente al puntaje. Se replica en vez de devolver una propuesta fija porque
 * lo que la pantalla tiene que mostrar es justamente el razonamiento —por qué entró cada
 * película y cuántos pases se lleva—, y con datos inventados los indicadores no lo dirían.
 */

/** Lo habitual, igual que los defaults de la ruta: una semana, de 14 a 24, ocho títulos. */
const GRILLA_POR_DEFECTO = {
  dias: 7, apertura: "14:00", cierre: "00:00", cuantasPeliculas: 8,
  idioma: "SUBTITULADA", proyeccion: "DOS_D",
};

const BONO_GENERO_NUEVO = 2.0;

const puntajeDe = (pelicula) => datos.PUNTAJES[pelicula.id] || 0;

const minutosDe = (hora) => Number(hora.slice(0, 2)) * 60 + Number(hora.slice(3, 5));

const enHoras = (minutos) => `${dosDigitos(Math.floor(minutos / 60))}:${dosDigitos(minutos % 60)}`;

/** El cierre a las 00:00 es el final del día, no su principio. */
const cierreEfectivo = (cierre) => (minutosDe(cierre) === 0 ? 23 * 60 + 59 : minutosDe(cierre));

/**
 * El elenco de la semana. La primera elección es la mejor película a secas; de ahí en
 * adelante entra la de mayor puntaje + bono por los géneros que todavía no están
 * cubiertos, así que cada una se gana el lugar contra lo que ya hay.
 *
 * Con el elenco vacío no hay bono —mide lo que una película *agrega*— y el bono crece
 * cada vez menos, para que cuatro etiquetas de género no valgan cuatro veces lo que una.
 */
function elegirElenco(cuantas) {
  const candidatas = datos.peliculas.filter(
    (p) => p.estadoRevision === "CONFIRMADA" && p.duracionMinutos > 0);
  const elenco = [];
  const cubiertos = new Set();

  while (elenco.length < cuantas && candidatas.length) {
    const valor = (p) => puntajeDe(p) + (elenco.length === 0 ? 0
      : BONO_GENERO_NUEVO * Math.sqrt(p.generos.filter((g) => !cubiertos.has(g)).length));
    // Desempate por título: sin esto, dos películas con el mismo valor podrían salir en
    // cualquier orden y la propuesta dejaría de ser reproducible.
    const mejor = candidatas.reduce((a, b) => {
      const diferencia = valor(b) - valor(a);
      return diferencia > 0 || (diferencia === 0 && b.titulo < a.titulo) ? b : a;
    });
    elenco.push(mejor);
    mejor.generos.forEach((g) => cubiertos.add(g));
    candidatas.splice(candidatas.indexOf(mejor), 1);
  }
  return elenco;
}

/**
 * La que más lejos está de los pases que le corresponden por puntaje. La deuda es
 * asignados / puntaje: cuanto más chico ese número, más le deben. Con eso la mejor de la
 * semana se lleva cuatro pases por día y la última uno, sin escribir esa tabla a mano.
 */
function conMasDeuda(elenco, asignados) {
  // Nunca cero: una película sin valorar quedaría con deuda infinita y se llevaría la
  // grilla entera.
  const deuda = (p) => asignados.get(p.id) / Math.max(puntajeDe(p), 0.1);
  return elenco.reduce((a, b) => {
    const diferencia = deuda(b) - deuda(a);
    return diferencia < 0 || (diferencia === 0 && b.titulo < a.titulo) ? b : a;
  });
}

/** Llena cada sala, día por día, desde la apertura hasta que no entre una función más. */
function repartir(elenco, criterios) {
  const apertura = minutosDe(criterios.apertura);
  const cierre = cierreEfectivo(criterios.cierre);
  const asignados = new Map(elenco.map((p) => [p.id, 0]));
  const pases = [];

  for (let dia = 0; dia < criterios.dias; dia++) {
    const fecha = new Date(`${criterios.desde}T00:00:00`);
    fecha.setDate(fecha.getDate() + dia);
    const iso = `${fecha.getFullYear()}-${dosDigitos(fecha.getMonth() + 1)}-${dosDigitos(fecha.getDate())}`;

    for (const sala of datos.salas) {
      let momento = apertura;
      while (momento < cierre) {
        const elegida = conMasDeuda(elenco, asignados);
        const fin = momento + elegida.duracionMinutos;
        // No entra completa antes de cerrar: la sala se da por llena. No se prueba con
        // una más corta a propósito, porque eso dejaría el último turno del día siempre
        // para la película de menor duración.
        if (fin > cierre) break;

        const inicio = `${iso}T${enHoras(momento)}:00`;
        // R3 se pregunta, no se reescribe: si la sala ya tiene algo cargado ahí, el pase
        // se corre media hora en vez de pisarlo.
        if (funcionSuperpuesta(sala.id, inicio, elegida.duracionMinutos)) {
          momento += 30;
          continue;
        }
        pases.push({
          peliculaId: elegida.id, titulo: elegida.titulo,
          salaId: sala.id, sala: sala.nombre,
          inicio, duracionMinutos: elegida.duracionMinutos,
        });
        asignados.set(elegida.id, asignados.get(elegida.id) + 1);
        momento = fin + sala.minutosLimpieza;
      }
    }
  }
  return pases;
}

/** Los números con los que se juzga la grilla, para poder comparar dos corridas. */
function medirGrilla(elenco, pases, criterios) {
  const porId = new Map(elenco.map((p) => [p.id, p]));
  const minutosPorDia = cierreEfectivo(criterios.cierre) - minutosDe(criterios.apertura);
  const pasesPorGenero = {};
  const cubiertos = new Set();

  for (const pase of pases) {
    for (const genero of porId.get(pase.peliculaId).generos) {
      pasesPorGenero[genero] = (pasesPorGenero[genero] || 0) + 1;
    }
  }
  elenco.forEach((p) => p.generos.forEach((g) => cubiertos.add(g)));

  const programados = pases.reduce((suma, p) => suma + p.duracionMinutos, 0);
  const disponibles = minutosPorDia * criterios.dias * datos.salas.length;

  return {
    minutosProgramados: programados,
    minutosDisponibles: disponibles,
    ocupacion: disponibles ? programados / disponibles : 0,
    puntajePromedio: pases.length
      ? pases.reduce((suma, p) => suma + puntajeDe(porId.get(p.peliculaId)), 0) / pases.length
      : 0,
    generosCubiertos: cubiertos.size,
    generosTotales: datos.GENEROS.length,
    pasesPorGenero,
  };
}

/**
 * La cuenta, una sola vez: lo único que cambia entre proponer y aplicar es si escribe las
 * funciones. Por eso `funcionesCreadas` es el único campo distinto entre las dos
 * respuestas — los pases son los mismos y no alcanzan para distinguirlas.
 */
function planificarGrilla(criterios, aplicar) {
  const c = { ...GRILLA_POR_DEFECTO };
  for (const [clave, valor] of Object.entries(criterios || {})) {
    if (valor !== null && valor !== undefined && valor !== "") c[clave] = valor;
  }
  if (!c.desde) c.desde = ahoraISO().slice(0, 10);

  if (!(c.dias > 0)) return fallar("La grilla tiene que cubrir al menos un día");
  if (!(c.cuantasPeliculas > 0)) return fallar("Hay que programar al menos una película");
  if (!(c.precio > 0)) return fallar("El precio debe ser mayor a cero");
  if (minutosDe(c.apertura) >= cierreEfectivo(c.cierre)) {
    return fallar("El cine tiene que cerrar después de abrir");
  }
  if (!datos.salas.length) return fallar("No hay salas cargadas para programar");

  const elenco = elegirElenco(Number(c.cuantasPeliculas));
  if (!elenco.length) {
    return fallar("No hay películas confirmadas para armar la grilla: revisá el buzón de importadas");
  }

  const pases = repartir(elenco, { ...c, dias: Number(c.dias) });
  if (aplicar) {
    for (const pase of pases) {
      datos.funciones.push({
        id: siguienteId(datos.funciones),
        peliculaId: pase.peliculaId, salaId: pase.salaId, inicio: pase.inicio,
        idioma: c.idioma, proyeccion: c.proyeccion, precio: Number(c.precio),
      });
    }
  }

  const pasesPorPelicula = new Map();
  pases.forEach((p) => pasesPorPelicula.set(p.peliculaId, (pasesPorPelicula.get(p.peliculaId) || 0) + 1));

  return responder({
    elenco: elenco.map((p) => ({
      id: p.id, titulo: p.titulo, puntaje: puntajeDe(p),
      duracionMinutos: p.duracionMinutos, generos: p.generos,
      pases: pasesPorPelicula.get(p.id) || 0,
    })),
    pases,
    indicadores: medirGrilla(elenco, pases, { ...c, dias: Number(c.dias) }),
    funcionesCreadas: aplicar ? pases.length : 0,
  });
}

export const proponerGrilla = (criterios) => planificarGrilla(criterios, false);
export const armarGrilla = (criterios) => planificarGrilla(criterios, true);

function conDatosDeLaReserva(r) {
  const funcion = datos.funciones.find((f) => f.id === r.funcionId);
  return {
    ...r,
    funcion,
    pelicula: funcion ? peliculaDe(funcion.peliculaId) : null,
    sala: funcion ? salaDe(funcion.salaId) : null,
    cliente: datos.clientes.find((c) => c.id === r.clienteId),
    total: r.entradas.reduce((suma, e) => suma + e.precio, 0),
    pago: datos.pagos.find((p) => p.reservaId === r.id) || null,
  };
}

export function obtenerReservas() {
  return responder(datos.reservas.map(conDatosDeLaReserva));
}

/**
 * Las reservas de un cliente, buscadas por email: el cliente no inicia sesión, así que
 * el email es lo único con lo que puede recuperarlas (CU-09).
 */
export function obtenerReservasDe(email) {
  const buscado = String(email || "").trim().toLowerCase();
  if (!buscado.includes("@")) return fallar("El email no es válido");
  const cliente = datos.clientes.find((c) => c.email.toLowerCase() === buscado);
  if (!cliente) return responder([]);
  return responder(datos.reservas
    .filter((r) => r.clienteId === cliente.id)
    .map(conDatosDeLaReserva));
}

/**
 * R6: cancelar libera las butacas de esa función.
 * R13: solo se cancela una reserva sin cobrar; una pagada necesita una devolución.
 */
export function cancelarReserva(id) {
  const reserva = datos.reservas.find((r) => r.id === Number(id));
  if (!reserva) return fallar(`No existe la reserva ${id}`);
  if (reserva.estado !== "RESERVADA") {
    return fallar(`La reserva está ${reserva.estado}, solo se puede cancelar una reserva sin cobrar`);
  }
  reserva.estado = "CANCELADA";
  return responder(reserva);
}

/* --------------------------------------------------------------------- pagos */

export function obtenerMediosPago() {
  return responder(Object.entries(datos.MEDIOS_PAGO)
    .map(([nombre, medio]) => ({ nombre, ...medio })));
}

/**
 * Registra el cobro y deja la reserva PAGADA. El monto no se recibe: sale del total
 * de la reserva, así es imposible cobrar un importe distinto al de las butacas.
 */
export function cobrar(reservaId, medio, codigoAutorizacion) {
  const reserva = datos.reservas.find((r) => r.id === Number(reservaId));
  if (!reserva) return fallar(`No existe la reserva ${reservaId}`);
  // R5
  if (reserva.estado !== "RESERVADA") {
    return fallar(`La reserva está ${reserva.estado}, no se puede cobrar`);
  }
  const definicion = datos.MEDIOS_PAGO[medio];
  if (!definicion) return fallar("Falta el medio de pago");
  // R11
  if (definicion.requiereAutorizacion && !String(codigoAutorizacion || "").trim()) {
    return fallar(`El pago con ${medio} necesita código de autorización`);
  }
  if (datos.pagos.some((p) => p.reservaId === reserva.id)) {
    return fallar(`La reserva ${reserva.id} ya tiene un pago registrado`);
  }

  // El descuento se resuelve acá y no al reservar: recién ahora se sabe el medio de
  // pago, y hay promociones que dependen de él.
  const subtotal = reserva.entradas.reduce((suma, e) => suma + e.precio, 0);
  const ganadora = mejorPromocion(reserva, funcionDe(reserva), medio);
  const descuento = ganadora ? descuentoDe(ganadora, reserva.entradas) : 0;
  const pago = {
    id: siguienteId(datos.pagos),
    reservaId: reserva.id,
    subtotal: redondear(subtotal),
    promocionId: ganadora ? ganadora.id : null,
    descuento: redondear(descuento),
    monto: redondear(subtotal - descuento),
    medio,
    fecha: ahoraISO(),
    codigoAutorizacion: String(codigoAutorizacion || "").trim(),
  };
  datos.pagos.push(pago);
  reserva.estado = "PAGADA";
  return responder(pago);
}

export function obtenerPagoDeReserva(reservaId) {
  const pago = datos.pagos.find((p) => p.reservaId === Number(reservaId));
  return responder(pago || null);
}

/*
 * La pasarela emulada. Los checkouts abiertos viven en un Map y no en `datos` porque lo
 * único que tienen que sobrevivir es el rato entre que el cliente ve el QR y aprueba:
 * cerrada la pestaña, ese checkout no le sirve a nadie.
 */
const checkouts = new Map();

/**
 * Abre el checkout de un medio electrónico. Valida lo mismo que el cobro y no al
 * confirmar: mandar a pagar una reserva que no se puede cobrar termina en plata que hay
 * que devolver, y la devolución es justo lo que no existe (R13).
 */
export function abrirCheckout(reservaId, medio) {
  const reserva = datos.reservas.find((r) => r.id === Number(reservaId));
  if (!reserva) return fallar(`No existe la reserva ${reservaId}`);
  const definicion = datos.MEDIOS_PAGO[medio];
  if (!definicion) return fallar("Falta el medio de pago");
  if (!definicion.requiereAutorizacion) {
    return fallar(`El pago con ${medio} se cobra en la caja del cine, no por checkout`);
  }
  if (reserva.estado !== "RESERVADA") {
    return fallar(`La reserva está ${reserva.estado}, no se puede cobrar`);
  }
  if (datos.pagos.some((p) => p.reservaId === reserva.id)) {
    return fallar(`La reserva ${reserva.id} ya tiene un pago registrado`);
  }

  // El monto ya viaja con el descuento aplicado: es el importe que el cliente aprueba en
  // la pantalla del procesador, y si después se cobrara otro no coincidirían.
  const subtotal = reserva.entradas.reduce((suma, e) => suma + e.precio, 0);
  const ganadora = mejorPromocion(reserva, funcionDe(reserva), medio);
  const id = `MP-${Math.floor(1e9 + Math.random() * 9e9)}`;
  const checkout = {
    id,
    reservaId: reserva.id,
    medio,
    monto: redondear(subtotal - (ganadora ? descuentoDe(ganadora, reserva.entradas) : 0)),
    urlPago: `https://checkout.emulado.local/mp/${id}`,
    // El contenido del QR, no una imagen: dibujarlo es del navegador.
    codigoQr: `MP-QR|${id}`,
  };
  checkouts.set(id, checkout);
  return responder(checkout);
}

/**
 * «El cliente pagó»: la pasarela devuelve el código de autorización y con él se registra
 * el cobro. Qué se está pagando sale del checkout y no de quien confirma, así no se puede
 * autorizar un checkout y aplicarlo a otra reserva.
 */
export function confirmarCheckout(checkoutId) {
  const checkout = checkouts.get(String(checkoutId));
  if (!checkout) return fallar(`No existe el checkout ${checkoutId}`);
  return cobrar(checkout.reservaId, checkout.medio,
    `MP-AUT-${Math.floor(10000000 + Math.random() * 89999999)}`);
}

/** Arqueo: qué se cobró en un día, cuánto, y abierto por medio de pago. */
export function obtenerArqueo(fecha) {
  const delDia = datos.pagos.filter((p) => p.fecha.slice(0, 10) === fecha);
  const porMedio = {};
  for (const pago of delDia) {
    if (!porMedio[pago.medio]) porMedio[pago.medio] = { cantidad: 0, total: 0 };
    porMedio[pago.medio].cantidad += 1;
    porMedio[pago.medio].total += pago.monto;
  }

  const detalle = delDia.map((pago) => {
    const reserva = datos.reservas.find((r) => r.id === pago.reservaId);
    const funcion = reserva && datos.funciones.find((f) => f.id === reserva.funcionId);
    return {
      ...pago,
      reserva,
      pelicula: funcion ? peliculaDe(funcion.peliculaId) : null,
      cliente: reserva && datos.clientes.find((c) => c.id === reserva.clienteId),
      entradas: reserva ? reserva.entradas.length : 0,
    };
  });

  return responder({
    fecha,
    pagos: detalle,
    total: delDia.reduce((suma, p) => suma + p.monto, 0),
    entradas: detalle.reduce((suma, p) => suma + p.entradas, 0),
    porMedio,
  });
}

/* ------------------------------------------------------ informes por función */

/**
 * Qué se vendió para esa función y a qué valor. Lo que declara es lo **cobrado**, y por
 * eso la fuente es el pago: una reserva sin pagar retiene butacas pero no vendió ninguna
 * entrada, y contarla infla la declaración con plata que nunca entró.
 *
 * El desglose por tarifa se arma con el precio de lista de cada butaca y los totales con
 * el pago: es la misma plata mirada por dos lados, y el pago es el único que sabe cuánto
 * sacó la promoción, que es sobre el total y no sobre una butaca.
 */
function borderoDe(funcionId) {
  const funcion = datos.funciones.find((f) => f.id === Number(funcionId));
  if (!funcion) return null;

  const porTarifa = {};
  let espectadores = 0;
  let bruta = 0;
  let descuentos = 0;
  let neta = 0;

  for (const reserva of datos.reservas.filter((r) => r.funcionId === funcion.id)) {
    const pago = datos.pagos.find((p) => p.reservaId === reserva.id);
    if (!pago) continue;
    for (const entrada of reserva.entradas) {
      const tarifa = entrada.tarifa || "GENERAL";
      if (!porTarifa[tarifa]) porTarifa[tarifa] = { cantidad: 0, total: 0 };
      porTarifa[tarifa].cantidad += 1;
      porTarifa[tarifa].total = redondear(porTarifa[tarifa].total + entrada.precio);
      espectadores += 1;
    }
    bruta += pago.subtotal;
    descuentos += pago.descuento;
    neta += pago.monto;
  }

  return {
    funcionId: funcion.id,
    pelicula: peliculaDe(funcion.peliculaId).titulo,
    sala: salaDe(funcion.salaId).nombre,
    funcion: funcion.inicio,
    generadoEn: ahoraISO(),
    espectadores,
    recaudacionBruta: redondear(bruta),
    descuentos: redondear(descuentos),
    recaudacionNeta: redondear(neta),
    porTarifa,
  };
}

export function obtenerBordero(funcionId) {
  const bordero = borderoDe(funcionId);
  return bordero ? responder(bordero) : fallar(`No existe la función ${funcionId}`);
}

/**
 * Emitir es declarar: el backend escribe el archivo que se sube al INCAA y responde el
 * mismo informe. Acá no hay dónde escribirlo, así que lo único que se ve del acto de
 * declarar es el `generadoEn` nuevo — que es, justamente, lo que fecha la declaración.
 */
export const emitirBordero = (funcionId) => obtenerBordero(funcionId);

/**
 * Cuánto dejó la función entre las dos cajas. El candy de mostrador queda afuera a
 * propósito: solo la compra con `reservaId` dice de qué función se trata, y repartir la
 * del mostrador entre las funciones del día sería inventar el dato.
 */
export function obtenerInformeDeFuncion(funcionId) {
  const bordero = borderoDe(funcionId);
  if (!bordero) return fallar(`No existe la función ${funcionId}`);

  // Sin mirar el estado de la reserva: una compra del candy nace cobrada, así que esa
  // plata entró aunque después la reserva se cancelara.
  const deLaFuncion = datos.reservas
    .filter((r) => r.funcionId === Number(funcionId))
    .map((r) => r.id);
  const compras = datos.comprasCandy.filter((c) => deLaFuncion.includes(c.reservaId));
  const candy = compras.reduce((suma, c) => suma + c.total, 0);

  return responder({
    boleteria: bordero,
    comprasCandy: compras.length,
    candy: redondear(candy),
    total: redondear(bordero.recaudacionNeta + candy),
  });
}

/* -------------------------------------------------------------- promociones */

// R15: no se acumulan, gana la que más descuenta. R16: las entradas de tarifa reducida
// no participan del cálculo. Es el mismo criterio que GestorPromociones del backend.

function funcionDe(reserva) {
  return datos.funciones.find((f) => f.id === reserva.funcionId);
}

function aplicaA(promocion, funcion, medio) {
  if (!promocion.activa || !funcion) return false;
  const inicio = new Date(funcion.inicio);
  const dia = inicio.toISOString().slice(0, 10);
  if (dia < promocion.vigenciaDesde || dia > promocion.vigenciaHasta) return false;

  const DIAS = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
  if (promocion.diasSemana?.length && !promocion.diasSemana.includes(DIAS[inicio.getDay()])) {
    return false;
  }
  if (promocion.mediosPago?.length && !promocion.mediosPago.includes(medio)) return false;

  const hora = funcion.inicio.slice(11, 16);
  if (promocion.horaDesde && hora < promocion.horaDesde.slice(0, 5)) return false;
  if (promocion.horaHasta && hora > promocion.horaHasta.slice(0, 5)) return false;
  return true;
}

/** Solo sobre las entradas de tarifa general (R16). */
function descuentoDe(promocion, entradas) {
  const alcanzadas = entradas.filter((e) => (e.tarifa || "GENERAL") === "GENERAL");
  if (!alcanzadas.length) return 0;
  const subtotal = alcanzadas.reduce((suma, e) => suma + e.precio, 0);

  let bruto = 0;
  if (promocion.tipo === "PORCENTAJE") bruto = subtotal * promocion.porcentaje / 100;
  else if (promocion.tipo === "MONTO_FIJO") bruto = promocion.monto;
  else if (promocion.tipo === "NXM") {
    const gratis = Math.floor(alcanzadas.length / promocion.lleva) * (promocion.lleva - promocion.paga);
    // Regala las más baratas, igual que el backend.
    bruto = alcanzadas.map((e) => e.precio).sort((a, b) => a - b).slice(0, gratis)
      .reduce((suma, precio) => suma + precio, 0);
  }
  return Math.min(Math.max(redondear(bruto), 0), subtotal);
}

function mejorPromocion(reserva, funcion, medio) {
  return (datos.promociones || [])
    .filter((p) => aplicaA(p, funcion, medio))
    .filter((p) => descuentoDe(p, reserva.entradas) > 0)
    // Empate: la de menor id, igual que el backend.
    .sort((a, b) => descuentoDe(b, reserva.entradas) - descuentoDe(a, reserva.entradas) || a.id - b.id)[0];
}

export function obtenerPromociones() {
  return responder((datos.promociones || []).map((p) => ({ ...p })));
}

export function crearPromocion(promocion) {
  if (!promocion.nombre?.trim()) return fallar("La promoción necesita un nombre");
  if (!promocion.vigenciaDesde || !promocion.vigenciaHasta
      || promocion.vigenciaHasta < promocion.vigenciaDesde) {
    return fallar("La vigencia tiene que empezar antes de terminar");
  }
  if ((datos.promociones || []).some(
        (p) => p.nombre.toLowerCase() === promocion.nombre.trim().toLowerCase())) {
    return fallar(`Ya hay una promoción llamada ${promocion.nombre}`);
  }
  if (promocion.tipo === "PORCENTAJE" && !(promocion.porcentaje > 0 && promocion.porcentaje < 100)) {
    return fallar("El porcentaje tiene que estar entre 1 y 99");
  }
  if (promocion.tipo === "MONTO_FIJO" && !(promocion.monto > 0)) {
    return fallar("El monto del descuento debe ser mayor a cero");
  }
  if (promocion.tipo === "NXM" && !(promocion.lleva > promocion.paga && promocion.paga > 0)) {
    return fallar("En un NxM hay que llevar más de lo que se paga");
  }

  datos.promociones = datos.promociones || [];
  const nueva = { ...promocion, nombre: promocion.nombre.trim(),
                  id: siguienteId(datos.promociones), activa: true };
  datos.promociones.push(nueva);
  return responder({ ...nueva });
}

const cambiarEstadoPromocion = (id, activa) => {
  const promocion = (datos.promociones || []).find((p) => p.id === Number(id));
  if (!promocion) return fallar(`No existe la promoción ${id}`);
  promocion.activa = activa;
  return responder({ ...promocion });
};

export const darDeBajaPromocion = (id) => cambiarEstadoPromocion(id, false);
export const darDeAltaPromocion = (id) => cambiarEstadoPromocion(id, true);

/* ----------------------------------------------------------- control de acceso */

/** R18: solo pasa una reserva pagada, y una sola vez. */
export function validarEntrada(codigo) {
  const buscado = String(codigo || "").trim().toUpperCase();
  const reserva = datos.reservas.find((r) => r.codigo === buscado);
  if (!reserva) return fallar("No existe ninguna reserva con ese código");
  if (reserva.estado !== "PAGADA") {
    return fallar(`La reserva está ${reserva.estado}: solo se ingresa con una reserva pagada`);
  }
  if (reserva.ingresadaEn) return fallar(`Esa entrada ya se usó el ${reserva.ingresadaEn}`);

  reserva.ingresadaEn = ahoraISO();
  return obtenerReserva(reserva.id);
}

export const obtenerTarifas = () => responder(
  Object.entries(TARIFAS).map(([nombre, t]) => ({ nombre, ...t })));
