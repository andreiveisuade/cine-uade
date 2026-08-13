import * as api from "../api.js";
import { etiqueta, precio } from "../ui.js";

// Lo elegido en el mapa de butacas, para que lo lea la confirmación. Es un mapa de
// código a tarifa y no una lista, porque la tarifa es por persona: en una reserva de
// cuatro puede haber dos generales, un menor y un jubilado. De paso, elegir dos veces
// la misma butaca es imposible de expresar.
export const seleccion = { funcionId: null, butacas: {} };

// El catálogo de tarifas sale del backend y no se repite acá: el multiplicador vive en
// el enum del dominio, y tenerlo duplicado serían dos fuentes de verdad para el precio.
let tarifas = null;

export async function catalogoTarifas() {
  if (!tarifas) tarifas = await api.obtenerTarifas();
  return tarifas;
}

export function tarifaPorNombre(nombre) {
  return (tarifas || []).find((t) => t.nombre === nombre) || { multiplicador: 1, requiereAcreditacion: false };
}

/** El precio de esa butaca con esa tarifa. asiento.precio siempre viene en GENERAL. */
export function precioConTarifa(asiento, nombreTarifa) {
  return Math.round(asiento.precio * tarifaPorNombre(nombreTarifa).multiplicador * 100) / 100;
}

/** Un <select> de tarifas para una butaca. */
export function selectorTarifa(codigo, elegida) {
  const opciones = (tarifas || []).map((t) =>
    `<option value="${t.nombre}" ${t.nombre === elegida ? "selected" : ""}>${etiqueta(t.nombre)}</option>`,
  ).join("");
  return `<select data-tarifa-de="${codigo}"
    class="rounded border border-slate-400 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100">${opciones}</select>`;
}

// El cliente no inicia sesión. Recordar sus datos en el navegador es lo que hace que
// registrarse sirva de algo: no vuelve a tipearlos al comprar ni al buscar sus reservas.
const CLAVE_CLIENTE = "cine.cliente";

export function clienteRecordado() {
  try {
    return JSON.parse(localStorage.getItem(CLAVE_CLIENTE)) || null;
  } catch {
    return null;
  }
}

export function recordarCliente(cliente) {
  localStorage.setItem(CLAVE_CLIENTE,
    JSON.stringify({ nombre: cliente.nombre, email: cliente.email }));
}

/** "No soy yo": borra lo recordado para que la pantalla vuelva a pedir los datos. */
export function olvidarCliente() {
  localStorage.removeItem(CLAVE_CLIENTE);
}
