// La web del cliente: qué vista atiende cada ruta. Sin login — al cliente se lo
// reconoce por su email recién al confirmar la compra.
//
// Cada pantalla vive en su propio módulo bajo js/cliente/. Lo que comparten —la butaca
// elegida, el catálogo de tarifas y el email recordado— está en cliente/compra.js, que
// es el estado de la compra en curso.

import { iniciarRouter } from "./router.js";
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
