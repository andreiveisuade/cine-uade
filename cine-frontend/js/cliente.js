import { iniciarRouter } from "./router.js";
import { wireToggle } from "./theme.js";
import { vistaCartelera } from "./cliente/cartelera.js";
import { vistaPelicula } from "./cliente/pelicula.js";
import { vistaFuncion } from "./cliente/funcion.js";
import { vistaConfirmar } from "./cliente/confirmar.js";
import { vistaTicket } from "./cliente/ticket.js";
import { vistaMisReservas } from "./cliente/mis-reservas.js";
import { vistaRegistro } from "./cliente/registro.js";

iniciarRouter({
  contenedor: document.getElementById("app"),
  inicial: "cartelera",
  rutas: {
    cartelera: vistaCartelera,
    pelicula: vistaPelicula,
    funcion: vistaFuncion,
    confirmar: vistaConfirmar,
    ticket: vistaTicket,
    "mis-reservas": vistaMisReservas,
    registro: vistaRegistro,
  },
});

wireToggle();
