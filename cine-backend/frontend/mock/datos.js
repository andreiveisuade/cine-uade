// Datos de ejemplo con la misma forma que devolvería el backend.
// Se reemplazan por llamadas reales tocando únicamente js/api.js.

export const GENEROS = [
  "ACCION", "COMEDIA", "DRAMA", "TERROR", "CIENCIA_FICCION",
  "ANIMACION", "DOCUMENTAL", "ROMANCE", "SUSPENSO",
];

export const TIPOS_SALA = {
  DOS_D: { multiplicador: 1.0, soportaTresD: false },
  TRES_D: { multiplicador: 1.3, soportaTresD: true },
  IMAX: { multiplicador: 1.6, soportaTresD: true },
  CUATRO_D: { multiplicador: 1.8, soportaTresD: true },
  VIP: { multiplicador: 2.0, soportaTresD: true },
};

export const TIPOS_ASIENTO = {
  ESTANDAR: { multiplicador: 1.0 },
  VIP: { multiplicador: 1.5 },
  PAREJA: { multiplicador: 1.8 },
  ACCESIBLE: { multiplicador: 1.0 },
};

export const IDIOMAS = ["DOBLADA", "SUBTITULADA"];
export const PROYECCIONES = ["DOS_D", "TRES_D"];

export const peliculas = [
  { id: 1, titulo: "Matrix", duracionMinutos: 136, generos: ["ACCION", "CIENCIA_FICCION"] },
  { id: 2, titulo: "El Padrino", duracionMinutos: 175, generos: ["DRAMA", "SUSPENSO"] },
  { id: 3, titulo: "Intensa-Mente 2", duracionMinutos: 96, generos: ["ANIMACION", "COMEDIA"] },
  { id: 4, titulo: "Duna: Parte Dos", duracionMinutos: 166, generos: ["CIENCIA_FICCION", "ACCION"] },
  { id: 5, titulo: "El Resplandor", duracionMinutos: 146, generos: ["TERROR", "SUSPENSO"] },
  { id: 6, titulo: "Cuando Harry conoció a Sally", duracionMinutos: 96, generos: ["ROMANCE", "COMEDIA"] },
  { id: 7, titulo: "Nuestro planeta", duracionMinutos: 88, generos: ["DOCUMENTAL"] },
  { id: 8, titulo: "Oppenheimer", duracionMinutos: 180, generos: ["DRAMA"] },
];

// Las seis salas del complejo, igual que SalasDeEjemplo.java.
const definicionSalas = [
  { id: 1, nombre: "Sala 1", tipo: "IMAX",
    butacasPorFila: [8, 10, 12, 12, 14],
    vip: [], accesibles: ["A1", "A8"] },
  { id: 2, nombre: "Sala 2", tipo: "IMAX",
    butacasPorFila: [14, 14, 14, 14, 14, 14, 14, 14],
    vip: [], accesibles: ["A1", "A14"] },
  { id: 3, nombre: "Sala 3", tipo: "TRES_D",
    butacasPorFila: [12, 14, 16, 18, 18, 20, 20, 20, 16, 16],
    vip: ["I1", "I2", "I3", "I4", "J1", "J2", "J3", "J4"], accesibles: ["A1", "A12"] },
  { id: 4, nombre: "Sala 4", tipo: "TRES_D",
    butacasPorFila: [16, 18, 20, 22, 22, 24, 24, 24, 24, 22, 20, 18],
    vip: [], accesibles: ["A1", "A2", "A15", "A16"] },
  { id: 5, nombre: "Sala 5", tipo: "VIP",
    butacasPorFila: [6, 6, 8, 8],
    vip: ["A1", "A2", "A3", "A4", "A5", "A6",
          "B1", "B2", "B3", "B4", "B5", "B6",
          "C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8",
          "D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8"], accesibles: [] },
  { id: 6, nombre: "Sala 6", tipo: "CUATRO_D",
    butacasPorFila: [10, 12, 12, 14, 14, 12],
    vip: [], accesibles: ["A1", "A10"] },
];

export function letraFila(fila) {
  return String.fromCharCode("A".charCodeAt(0) + fila - 1);
}

// Misma lógica que GestorSalas.generarAsientos: en una sala VIP las butacas
// marcadas como premium son de pareja, en el resto son VIP.
export function generarAsientos(sala, vip, accesibles, primerId) {
  const asientos = [];
  let id = primerId;
  sala.butacasPorFila.forEach((cantidad, indice) => {
    const fila = indice + 1;
    for (let numero = 1; numero <= cantidad; numero++) {
      const codigo = letraFila(fila) + numero;
      let tipo = "ESTANDAR";
      if (vip.includes(codigo)) {
        tipo = sala.tipo === "VIP" ? "PAREJA" : "VIP";
      } else if (accesibles.includes(codigo)) {
        tipo = "ACCESIBLE";
      }
      asientos.push({
        id: id++, salaId: sala.id, fila, numero, codigo,
        tipo, estado: "DISPONIBLE",
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
  const generados = generarAsientos(sala, def.vip, def.accesibles, siguienteAsientoId);
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

export const clientes = [
  { id: 1, nombre: "Andrei Veis", email: "andrei@uade.edu.ar" },
  { id: 2, nombre: "Lucía Fernández", email: "lucia@uade.edu.ar" },
  { id: 3, nombre: "Martín Sosa", email: "martin@uade.edu.ar" },
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
