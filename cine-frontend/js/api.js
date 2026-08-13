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
function precioDeAsiento(funcion, sala, asiento) {
  return funcion.precio
    * datos.TIPOS_SALA[sala.tipo].multiplicador
    * datos.TIPOS_ASIENTO[asiento.tipo].multiplicador;
}

// Un asiento no está ocupado en sí mismo: lo está en una función, si alguna
// reserva no cancelada de esa función lo tomó.
function asientosOcupados(funcionId) {
  return new Set(
    datos.reservas
      .filter((r) => r.funcionId === funcionId && r.estado !== "CANCELADA")
      .flatMap((r) => r.entradas.map((e) => e.asientoId)),
  );
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
export function obtenerFuncion(id) {
  const funcion = datos.funciones.find((f) => f.id === Number(id));
  if (!funcion) return fallar(`No existe la función ${id}`);

  const sala = salaDe(funcion.salaId);
  const ocupados = asientosOcupados(funcion.id);
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

/**
 * Crea la reserva con las butacas elegidas. Si el email no existe, da de alta el cliente.
 * Replica las validaciones de GestorReservas.
 */
export function crearReserva({ funcionId, nombre, email, codigos }) {
  const funcion = datos.funciones.find((f) => f.id === Number(funcionId));
  if (!funcion) return fallar(`No existe la función ${funcionId}`);
  if (!nombre || !nombre.trim()) return fallar("Falta el nombre del cliente");
  if (!email || !email.includes("@")) return fallar("El email no es válido");
  if (!codigos || codigos.length === 0) return fallar("Hay que elegir al menos una butaca");

  const sala = salaDe(funcion.salaId);
  const deLaSala = datos.asientos.filter((a) => a.salaId === sala.id);
  const ocupados = asientosOcupados(funcion.id);

  const entradas = [];
  const yaElegidos = [];
  for (const codigo of codigos) {
    const buscado = String(codigo).trim().toUpperCase();
    if (yaElegidos.includes(buscado)) return fallar(`La butaca ${buscado} está repetida`);
    const asiento = deLaSala.find((a) => a.codigo === buscado);
    if (!asiento) return fallar(`La butaca ${buscado} no existe en esa sala`);
    if (asiento.estado === "FUERA_DE_SERVICIO") return fallar(`La butaca ${buscado} está fuera de servicio`);
    if (ocupados.has(asiento.id)) return fallar(`La butaca ${buscado} ya está ocupada`);
    yaElegidos.push(buscado);
    entradas.push({
      asientoId: asiento.id,
      codigoAsiento: asiento.codigo,
      precio: precioDeAsiento(funcion, sala, asiento),
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
    entradas,
  };
  datos.reservas.push(reserva);
  return responder(reserva);
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
  };
  datos.peliculas.push(pelicula);
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

export function crearSala({ nombre, tipo, butacasPorFila, codigosVip, codigosAccesibles }) {
  const nombreSala = (nombre || "").trim();
  if (!nombreSala) return fallar("El nombre no puede estar vacío");
  if (!datos.TIPOS_SALA[tipo]) return fallar("Falta el tipo de sala");
  if (!butacasPorFila || butacasPorFila.length === 0) return fallar("La sala necesita al menos una fila");
  if (butacasPorFila.length > 26) return fallar("Máximo 26 filas: se identifican con una letra");
  if (butacasPorFila.some((b) => !(b > 0))) return fallar("Cada fila debe tener al menos una butaca");
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
  };
  datos.salas.push(sala);

  const primerId = datos.asientos.reduce((max, a) => Math.max(max, a.id), 0) + 1;
  datos.asientos.push(...datos.generarAsientos(
    sala, codigosVip || [], codigosAccesibles || [], primerId));
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
  if (estado !== "DISPONIBLE" && estado !== "FUERA_DE_SERVICIO") {
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

export function obtenerReservas() {
  const lista = datos.reservas.map((r) => {
    const funcion = datos.funciones.find((f) => f.id === r.funcionId);
    return {
      ...r,
      funcion,
      pelicula: funcion ? peliculaDe(funcion.peliculaId) : null,
      sala: funcion ? salaDe(funcion.salaId) : null,
      cliente: datos.clientes.find((c) => c.id === r.clienteId),
      total: r.entradas.reduce((suma, e) => suma + e.precio, 0),
    };
  });
  return responder(lista);
}

export function cambiarEstadoReserva(id, estado) {
  const reserva = datos.reservas.find((r) => r.id === Number(id));
  if (!reserva) return fallar(`No existe la reserva ${id}`);
  // R5
  if (estado === "PAGADA" && reserva.estado !== "RESERVADA") {
    return fallar(`La reserva está ${reserva.estado}, no se puede pagar`);
  }
  if (estado === "CANCELADA" && reserva.estado === "CANCELADA") {
    return fallar("La reserva ya está cancelada");
  }
  reserva.estado = estado;
  return responder(reserva);
}
