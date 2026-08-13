// El panel del encargado: qué vista atiende cada ruta y quién puede entrar.
//
// Cada área vive en su propio módulo bajo js/admin/ y expone una función que recibe el
// contenedor. Sumar una pantalla es escribir su módulo y agregar una línea acá, que es el
// mismo criterio con el que la consola arma su menú.

import { iniciarRouter } from "./router.js";
import { wireToggle } from "./theme.js";
import { cerrarSesion, esAdministrador, pintarEncabezado, sesionActual } from "./admin/sesion.js";
import { vistaLogin } from "./admin/login.js";
import { vistaPeliculas } from "./admin/peliculas.js";
import { vistaSalas } from "./admin/salas.js";
import { vistaFunciones } from "./admin/funciones.js";
import { vistaProgramaciones } from "./admin/programaciones.js";
import { vistaAgenda } from "./admin/agenda.js";
import { vistaReservas, vistaCobrar } from "./admin/reservas.js";
import { vistaCaja } from "./admin/caja.js";
import { vistaPromociones } from "./admin/promociones.js";
import { vistaPuerta } from "./admin/puerta.js";

document.getElementById("salir").addEventListener("click", cerrarSesion);
pintarEncabezado();
wireToggle();

iniciarRouter({
  contenedor: document.getElementById("app"),
  inicial: sesionActual() ? (esAdministrador() ? "peliculas" : "puerta") : "login",
  guardia: (ruta) => {
    if (!sesionActual() && ruta !== "login") return "#/login";
    if (sesionActual() && ruta === "login") return esAdministrador() ? "#/peliculas" : "#/puerta";
    // El rol no es solo cosmético: escondemos el menú y además cerramos la ruta.
    if (sesionActual() && !esAdministrador() && ruta !== "puerta") return "#/puerta";
    return null;
  },
  rutas: {
    login: vistaLogin,
    peliculas: vistaPeliculas,
    salas: vistaSalas,
    funciones: vistaFunciones,
    programaciones: vistaProgramaciones,
    agenda: vistaAgenda,
    reservas: vistaReservas,
    cobrar: vistaCobrar,
    caja: vistaCaja,
    promociones: vistaPromociones,
    puerta: vistaPuerta,
  },
});
