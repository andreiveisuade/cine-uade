// Datos de ejemplo con la misma forma que devolvería el backend.
// Se reemplazan por llamadas reales tocando únicamente js/api.js.

export const GENEROS = [
  "ACCION", "COMEDIA", "DRAMA", "TERROR", "CIENCIA_FICCION",
  "ANIMACION", "DOCUMENTAL", "ROMANCE", "SUSPENSO",
];

// No hay tipo de sala VIP: lo premium lo dice el tipo de butaca. Cuando existían los
// dos, una butaca de pareja en sala VIP pagaba el recargo dos veces.
export const TIPOS_SALA = {
  DOS_D: { multiplicador: 1.0, soportaTresD: false },
  TRES_D: { multiplicador: 1.3, soportaTresD: true },
  IMAX: { multiplicador: 1.6, soportaTresD: true },
  CUATRO_D: { multiplicador: 1.8, soportaTresD: true },
};

export const TIPOS_ASIENTO = {
  ESTANDAR: { multiplicador: 1.0 },
  VIP: { multiplicador: 1.5 },
  PAREJA: { multiplicador: 1.8 },
  ACCESIBLE: { multiplicador: 1.0 },
};

export const IDIOMAS = ["DOBLADA", "SUBTITULADA"];
export const PROYECCIONES = ["DOS_D", "TRES_D"];

// Los medios electrónicos devuelven un código de autorización del procesador; el
// efectivo no. Saberlo acá evita un if con lista de casos en cada pantalla (R11).
export const MEDIOS_PAGO = {
  EFECTIVO: { requiereAutorizacion: false },
  DEBITO: { requiereAutorizacion: true },
  CREDITO: { requiereAutorizacion: true },
  QR: { requiereAutorizacion: true },
  TRANSFERENCIA: { requiereAutorizacion: true },
};

// La clasificación guarda la edad mínima, no solo la etiqueta: cuando se valide la
// edad al vender va a ser una comparación numérica.
export const CLASIFICACIONES = {
  ATP: 0,
  MAS_13: 13,
  MAS_16: 16,
  MAS_18: 18,
};

/**
 * Poster de relleno como SVG embebido: el backend va a mandar una URL de verdad en
 * este mismo campo, y el <img> la consume igual.
 */
function poster(titulo, fondo) {
  const palabras = titulo.split(" ");
  const lineas = [];
  let actual = "";
  for (const palabra of palabras) {
    if ((actual + " " + palabra).trim().length > 13) {
      if (actual) lineas.push(actual.trim());
      actual = palabra;
    } else {
      actual += " " + palabra;
    }
  }
  if (actual.trim()) lineas.push(actual.trim());

  const primeraY = 150 - (lineas.length - 1) * 13;
  const texto = lineas
    .map((linea, i) => `<tspan x="100" y="${primeraY + i * 26}">${linea}</tspan>`)
    .join("");
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 300">`
    + `<rect width="200" height="300" fill="${fondo}"/>`
    + `<text fill="#ffffff" font-family="Helvetica,Arial,sans-serif" font-size="19"`
    + ` font-weight="bold" text-anchor="middle">${texto}</text></svg>`;
  return "data:image/svg+xml," + encodeURIComponent(svg);
}

// Los datos de catálogo (director, sinopsis, año, idioma original, poster) no tienen
// reglas asociadas: son para mostrar la película. enCartelera sí decide qué ve el
// cliente: una película cargada no está necesariamente en exhibición.
export const peliculas = [
  { id: 1, titulo: "Matrix", duracionMinutos: 136, generos: ["ACCION", "CIENCIA_FICCION"],
    clasificacion: "MAS_16", posterUrl: poster("Matrix", "#1f4d3d"),
    director: "Lana y Lilly Wachowski", anio: 1999, idiomaOriginal: "Inglés",
    sinopsis: "Un programador descubre que el mundo que conoce es una simulación y se une a la resistencia que pelea contra las máquinas.",
    enCartelera: true },
  { id: 2, titulo: "El Padrino", duracionMinutos: 175, generos: ["DRAMA", "SUSPENSO"],
    clasificacion: "MAS_16", posterUrl: poster("El Padrino", "#3b2a1a"),
    director: "Francis Ford Coppola", anio: 1972, idiomaOriginal: "Inglés",
    sinopsis: "El hijo menor de una familia mafiosa de Nueva York termina heredando el negocio que había jurado no tocar.",
    enCartelera: true },
  { id: 3, titulo: "Intensa-Mente 2", duracionMinutos: 96, generos: ["ANIMACION", "COMEDIA"],
    clasificacion: "ATP", posterUrl: poster("Intensa-Mente 2", "#b4531f"),
    director: "Kelsey Mann", anio: 2024, idiomaOriginal: "Inglés",
    sinopsis: "Las emociones de una adolescente tienen que hacerle lugar a unas cuantas nuevas que llegan sin avisar.",
    enCartelera: true },
  { id: 4, titulo: "Duna: Parte Dos", duracionMinutos: 166, generos: ["CIENCIA_FICCION", "ACCION"],
    clasificacion: "MAS_13", posterUrl: poster("Duna: Parte Dos", "#8a5a2b"),
    director: "Denis Villeneuve", anio: 2024, idiomaOriginal: "Inglés",
    sinopsis: "Paul Atreides se une a los fremen del desierto para vengar a su familia y frenar un futuro que ya vio.",
    enCartelera: true },
  { id: 5, titulo: "El Resplandor", duracionMinutos: 146, generos: ["TERROR", "SUSPENSO"],
    clasificacion: "MAS_18", posterUrl: poster("El Resplandor", "#5b1c1c"),
    director: "Stanley Kubrick", anio: 1980, idiomaOriginal: "Inglés",
    sinopsis: "Una familia queda a cargo de un hotel vacío durante el invierno y el encierro empieza a hacer lo suyo.",
    enCartelera: true },
  { id: 6, titulo: "Cuando Harry conoció a Sally", duracionMinutos: 96, generos: ["ROMANCE", "COMEDIA"],
    clasificacion: "MAS_13", posterUrl: poster("Cuando Harry conoció a Sally", "#7a3350"),
    director: "Rob Reiner", anio: 1989, idiomaOriginal: "Inglés",
    sinopsis: "Dos conocidos discuten durante años si un hombre y una mujer pueden ser solo amigos.",
    enCartelera: true },
  { id: 7, titulo: "Nuestro planeta", duracionMinutos: 88, generos: ["DOCUMENTAL"],
    clasificacion: "ATP", posterUrl: poster("Nuestro planeta", "#1e4763"),
    director: "Alastair Fothergill", anio: 2019, idiomaOriginal: "Inglés",
    sinopsis: "Un recorrido por los ecosistemas que quedan en pie y por lo que está en juego si desaparecen.",
    enCartelera: true },
  { id: 8, titulo: "Oppenheimer", duracionMinutos: 180, generos: ["DRAMA"],
    clasificacion: "MAS_16", posterUrl: poster("Oppenheimer", "#2d2d33"),
    director: "Christopher Nolan", anio: 2023, idiomaOriginal: "Inglés",
    sinopsis: "El físico que dirigió el proyecto de la primera bomba atómica convive después con lo que ayudó a construir.",
    enCartelera: true },
  // Cargada pero fuera de exhibición: la ve el encargado, no el cliente.
  { id: 9, titulo: "Metrópolis", duracionMinutos: 153, generos: ["CIENCIA_FICCION", "DRAMA"],
    clasificacion: "ATP", posterUrl: poster("Metrópolis", "#334155"),
    director: "Fritz Lang", anio: 1927, idiomaOriginal: "Alemán (muda)",
    sinopsis: "En una ciudad partida entre la superficie y el subsuelo, el hijo del gobernante baja a ver cómo se vive abajo.",
    enCartelera: false },
];

// Las seis salas del complejo. Cada tipo de butaca se declara por código: sin TipoSala
// VIP no hay de dónde inferir que una sala es "la de las butacas de pareja".
const definicionSalas = [
  { id: 1, nombre: "Sala 1", tipo: "IMAX",
    butacasPorFila: [8, 10, 12, 12, 14],
    vip: [], pareja: [], accesibles: ["A1", "A8"] },
  { id: 2, nombre: "Sala 2", tipo: "IMAX",
    butacasPorFila: [14, 14, 14, 14, 14, 14, 14, 14],
    vip: [], pareja: [], accesibles: ["A1", "A14"] },
  { id: 3, nombre: "Sala 3", tipo: "TRES_D",
    butacasPorFila: [12, 14, 16, 18, 18, 20, 20, 20, 16, 16],
    vip: ["I1", "I2", "I3", "I4", "J1", "J2", "J3", "J4"],
    pareja: [], accesibles: ["A1", "A12"] },
  { id: 4, nombre: "Sala 4", tipo: "TRES_D",
    butacasPorFila: [16, 18, 20, 22, 22, 24, 24, 24, 24, 22, 20, 18],
    vip: [], pareja: [], accesibles: ["A1", "A2", "A15", "A16"] },
  // La boutique: chica y con todas las butacas de pareja.
  { id: 5, nombre: "Sala 5", tipo: "DOS_D",
    butacasPorFila: [6, 6, 8, 8],
    vip: [],
    pareja: ["A1", "A2", "A3", "A4", "A5", "A6",
             "B1", "B2", "B3", "B4", "B5", "B6",
             "C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8",
             "D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8"],
    accesibles: [] },
  { id: 6, nombre: "Sala 6", tipo: "CUATRO_D",
    butacasPorFila: [10, 12, 12, 14, 14, 12],
    vip: [], pareja: [], accesibles: ["A1", "A10"] },
];

export function letraFila(fila) {
  return String.fromCharCode("A".charCodeAt(0) + fila - 1);
}

/** Cada butaca es estándar salvo que su código esté en una de las listas. */
export function generarAsientos(sala, vip, pareja, accesibles, primerId) {
  const asientos = [];
  let id = primerId;
  sala.butacasPorFila.forEach((cantidad, indice) => {
    const fila = indice + 1;
    for (let numero = 1; numero <= cantidad; numero++) {
      const codigo = letraFila(fila) + numero;
      let tipo = "ESTANDAR";
      if (vip.includes(codigo)) {
        tipo = "VIP";
      } else if (pareja.includes(codigo)) {
        tipo = "PAREJA";
      } else if (accesibles.includes(codigo)) {
        tipo = "ACCESIBLE";
      }
      asientos.push({
        id: id++, salaId: sala.id, fila, numero, codigo,
        tipo, estado: "HABILITADO",
      });
    }
  });
  return asientos;
}

export const salas = [];
export const asientos = [];

let siguienteAsientoId = 1;
for (const def of definicionSalas) {
  const sala = {
    id: def.id,
    nombre: def.nombre,
    tipo: def.tipo,
    butacasPorFila: def.butacasPorFila,
    filas: def.butacasPorFila.length,
    capacidadSala: def.butacasPorFila.reduce((a, b) => a + b, 0),
  };
  salas.push(sala);
  const generados = generarAsientos(
    sala, def.vip, def.pareja, def.accesibles, siguienteAsientoId);
  siguienteAsientoId += generados.length;
  asientos.push(...generados);
}

// Butacas rotas: no se venden en ninguna función.
for (const [salaId, codigo] of [[1, "B5"], [2, "D10"], [3, "C7"], [3, "C8"], [6, "E14"]]) {
  const roto = asientos.find((a) => a.salaId === salaId && a.codigo === codigo);
  if (roto) roto.estado = "FUERA_DE_SERVICIO";
}

// Fechas relativas al día de hoy para que la cartelera nunca quede vencida.
function fecha(diasDesdeHoy, hora) {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  d.setDate(d.getDate() + diasDesdeHoy);
  const [h, m] = hora.split(":");
  d.setHours(Number(h), Number(m));
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
         `T${pad(d.getHours())}:${pad(d.getMinutes())}:00`;
}

export const funciones = [
  { id: 1, peliculaId: 1, salaId: 1, inicio: fecha(0, "20:30"), idioma: "SUBTITULADA", proyeccion: "DOS_D", precio: 5000 },
  { id: 2, peliculaId: 1, salaId: 3, inicio: fecha(0, "22:45"), idioma: "DOBLADA", proyeccion: "TRES_D", precio: 5500 },
  { id: 3, peliculaId: 1, salaId: 2, inicio: fecha(1, "19:00"), idioma: "SUBTITULADA", proyeccion: "DOS_D", precio: 5000 },
  { id: 4, peliculaId: 2, salaId: 5, inicio: fecha(0, "21:00"), idioma: "SUBTITULADA", proyeccion: "DOS_D", precio: 6000 },
  { id: 5, peliculaId: 2, salaId: 2, inicio: fecha(2, "18:30"), idioma: "SUBTITULADA", proyeccion: "DOS_D", precio: 4800 },
  { id: 6, peliculaId: 3, salaId: 4, inicio: fecha(0, "15:00"), idioma: "DOBLADA", proyeccion: "TRES_D", precio: 4500 },
  { id: 7, peliculaId: 3, salaId: 4, inicio: fecha(0, "17:30"), idioma: "DOBLADA", proyeccion: "DOS_D", precio: 4500 },
  { id: 8, peliculaId: 3, salaId: 6, inicio: fecha(1, "16:00"), idioma: "DOBLADA", proyeccion: "TRES_D", precio: 5200 },
  { id: 9, peliculaId: 4, salaId: 1, inicio: fecha(0, "16:00"), idioma: "SUBTITULADA", proyeccion: "TRES_D", precio: 6500 },
  { id: 10, peliculaId: 4, salaId: 6, inicio: fecha(1, "21:30"), idioma: "SUBTITULADA", proyeccion: "TRES_D", precio: 6800 },
  { id: 11, peliculaId: 5, salaId: 3, inicio: fecha(1, "23:15"), idioma: "SUBTITULADA", proyeccion: "DOS_D", precio: 5000 },
  { id: 12, peliculaId: 6, salaId: 5, inicio: fecha(2, "20:00"), idioma: "SUBTITULADA", proyeccion: "DOS_D", precio: 6000 },
  { id: 13, peliculaId: 7, salaId: 2, inicio: fecha(2, "14:00"), idioma: "DOBLADA", proyeccion: "DOS_D", precio: 3800 },
  { id: 14, peliculaId: 8, salaId: 4, inicio: fecha(2, "21:00"), idioma: "SUBTITULADA", proyeccion: "DOS_D", precio: 5500 },
];

// Clientes y administradores son los dos roles de Usuario: comparten nombre y email,
// y solo el administrador tiene credenciales.
export const ROLES = ["CLIENTE", "ADMINISTRADOR"];

export const clientes = [
  { id: 1, nombre: "Andrei Veis", email: "andrei@uade.edu.ar", rol: "CLIENTE" },
  { id: 2, nombre: "Lucía Fernández", email: "lucia@uade.edu.ar", rol: "CLIENTE" },
  { id: 3, nombre: "Martín Sosa", email: "martin@uade.edu.ar", rol: "CLIENTE" },
];

// La clave va en claro solo porque es data de prueba del frontend: contra el backend
// real el login es un POST y la comparación de hashes ocurre allá.
export const administradores = [
  { id: 4, nombre: "Encargado del complejo", email: "encargado@cine.uade.ar", rol: "ADMINISTRADOR", password: "cine2026" },
];

function asientoDe(salaId, codigo) {
  return asientos.find((a) => a.salaId === salaId && a.codigo === codigo);
}

// El precio se congela en la entrada: es lo que se cobró, no lo que vale hoy.
function entrada(funcionId, codigo) {
  const funcion = funciones.find((f) => f.id === funcionId);
  const sala = salas.find((s) => s.id === funcion.salaId);
  const asiento = asientoDe(sala.id, codigo);
  return {
    asientoId: asiento.id,
    codigoAsiento: asiento.codigo,
    precio: funcion.precio
      * TIPOS_SALA[sala.tipo].multiplicador
      * TIPOS_ASIENTO[asiento.tipo].multiplicador,
  };
}

function reserva(id, funcionId, clienteId, estado, codigos) {
  return {
    id, funcionId, clienteId, estado,
    entradas: codigos.map((c) => entrada(funcionId, c)),
  };
}

export const reservas = [
  reserva(1, 1, 1, "PAGADA", ["C5", "C6", "C7"]),
  reserva(2, 1, 2, "RESERVADA", ["E1", "E2"]),
  // Cancelada: sus butacas vuelven a estar libres en la función 1.
  reserva(3, 1, 3, "CANCELADA", ["A3", "A4"]),
  reserva(4, 2, 2, "PAGADA", ["J1", "J2"]),
  reserva(5, 4, 1, "RESERVADA", ["C3", "C4"]),
  reserva(6, 6, 3, "PAGADA", ["F10", "F11", "F12", "F13"]),
  reserva(7, 9, 2, "RESERVADA", ["D6"]),
];

function totalDe(reservaId) {
  return reservas.find((r) => r.id === reservaId)
    .entradas.reduce((suma, e) => suma + e.precio, 0);
}

// Un pago por cada reserva pagada: el monto sale del total de la reserva, nunca
// se carga a mano. Hay uno de ayer para que el arqueo del día no los junte a todos.
export const pagos = [
  { id: 1, reservaId: 1, monto: totalDe(1), medio: "EFECTIVO",
    fecha: fecha(0, "14:22"), codigoAutorizacion: "" },
  { id: 2, reservaId: 4, monto: totalDe(4), medio: "CREDITO",
    fecha: fecha(0, "18:05"), codigoAutorizacion: "AUTH-40219" },
  { id: 3, reservaId: 6, monto: totalDe(6), medio: "QR",
    fecha: fecha(-1, "12:40"), codigoAutorizacion: "QR-88371" },
];
