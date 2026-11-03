import { avisar, get, navegar, post, put } from "./http/clienteHttp.js";
import { ruta } from "./http/rutas.js";

const reserva = (codigo) => ruta`/reservas/codigo/${codigo}`;

// Sin nombre ni email: la reserva es del cliente de la sesión (R28).
export const crearReserva = ({ funcionId, butacas, sesion }) =>
  post("/reservas", { funcionId: Number(funcionId), butacas, sesion });

export const obtenerMisReservas = () => get("/reservas/mias");

export const obtenerReservaPorCodigo = (codigo) => get(reserva(codigo));

export const cancelarReservaPorCodigo = (codigo) => post(reserva(codigo) + "/cancelacion");

// R30: va el pedido entero y {} lo saca; vuelve la reserva con el candy pendiente.
export const pedirCandy = (codigo, cantidades) => put(reserva(codigo) + "/candy", { cantidades });

// Lo que costaría con cada medio, sin abrir el pago: la promo depende del medio (R15, R16).
export const obtenerCotizacion = (codigo) => get(reserva(codigo) + "/cotizacion");

export const abrirCheckout = (codigo, medio) => post(reserva(codigo) + "/checkout", { medio });

// La vuelta del comprador sin salir del ticket: el backend consulta MP y cobra si aprobó. El 302 al ticket no se
// sigue (ya estamos ahí) y una falla no importa: la conciliación cobra igual.
export const avisarVueltaDelPago = (checkoutId) => avisar(ruta`/checkouts/${checkoutId}/retorno`);

// La pasarela emulada vuelve como MP: el navegador va al retorno con el status y este lo deja en el ticket. Un
// approved no cobra nada que la pasarela no autorice, así que con MP de verdad no sirve.
export const volverDeLaPasarelaEmulada = (checkoutId, status) =>
  navegar(ruta`/checkouts/${checkoutId}/retorno?status=${status}`);
