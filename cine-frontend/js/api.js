// Único punto de acceso a datos. Las vistas importan siempre desde acá y no saben si
// atrás hay un mock o la API real.
//
// Para conectar el backend, cambiar FUENTE a "http". El contrato de los endpoints está
// en API.md y las dos implementaciones devuelven exactamente la misma forma, así que no
// hay que tocar ninguna vista.

import * as http from "./api-http.js";
import * as mock from "./api-mock.js";

export const FUENTE = "http"; // "mock" | "http"

const impl = FUENTE === "http" ? http : mock;

export const {
  // catálogos
  obtenerGeneros,
  obtenerClasificaciones,
  obtenerTiposSala,
  obtenerIdiomas,
  obtenerProyecciones,
  obtenerMediosPago,
  obtenerTarifas,
  // cliente
  obtenerCartelera,
  obtenerPelicula,
  obtenerFuncionesDePelicula,
  obtenerFuncion,
  registrarCliente,
  buscarClientePorEmail,
  crearReserva,
  obtenerReserva,
  obtenerReservasDe,
  // encargado
  login,
  obtenerPeliculas,
  crearPelicula,
  actualizarPelicula,
  eliminarPelicula,
  obtenerSalas,
  obtenerSala,
  crearSala,
  eliminarSala,
  cambiarEstadoAsiento,
  obtenerFunciones,
  programarFuncion,
  eliminarFuncion,
  // programaciones
  obtenerProgramaciones,
  obtenerProgramacion,
  previsualizarProgramacion,
  crearProgramacion,
  darDeBajaProgramacion,
  darDeAltaProgramacion,
  obtenerReservas,
  cancelarReserva,
  cobrar,
  obtenerPagoDeReserva,
  obtenerArqueo,
  // promociones
  obtenerPromociones,
  crearPromocion,
  darDeBajaPromocion,
  darDeAltaPromocion,
  // control de acceso
  validarEntrada,
} = impl;
