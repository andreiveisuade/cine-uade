import * as api from "../api.js";
import { etiqueta } from "../etiquetas.js";
import { precio } from "../formato.js";

// Lo elegido en el mapa de butacas, para que lo lea la confirmación. Es un mapa de
// código a tarifa y no una lista, porque la tarifa es por persona: en una reserva de
// cuatro puede haber dos generales, un menor y un jubilado. De paso, elegir dos veces
// la misma butaca es imposible de expresar.
export const seleccion = { funcionId: null, butacas: {} };

/* ------------------------------------------ las butacas de mientras se elige */

/*
 * Mientras alguien elige, sus butacas dejan de ofrecerse al resto. El backend las guarda
 * a nombre de una sesión que genera este navegador y las suelta solo a los tres minutos:
 * cerrar la pestaña no avisa, así que el bloqueo tiene que caducar por su cuenta.
 *
 * Va en `sessionStorage` y no en `localStorage` porque cada pestaña es una compra
 * distinta. Y no es una credencial: lo peor que puede hacer una sesión inventada es
 * soltar el bloqueo de otro, y eso devuelve una butaca a la venta sin poder venderla dos
 * veces —eso lo sigue impidiendo la base—.
 */
const CLAVE_SESION = "cine.sesionDeCompra";

export function sesionDeCompra() {
  let sesion = sessionStorage.getItem(CLAVE_SESION);
  if (!sesion) {
    sesion = crypto.randomUUID();
    sessionStorage.setItem(CLAVE_SESION, sesion);
  }
  return sesion;
}

/**
 * Le avisa al backend qué tiene elegido esta sesión, entero y no de a una butaca: con eso
 * toma lo nuevo, renueva lo que sigue elegido y suelta lo que se deseleccionó.
 *
 * Si alguna se escapó la saca de la selección, porque seguir mostrándola elegida sería
 * prometer una butaca que ya no está. Devuelve las que se perdieron para que la pantalla
 * avise.
 */
export async function sostenerSeleccion(funcionId) {
  const { rechazadas } = await api.bloquearButacas({
    funcionId,
    sesion: sesionDeCompra(),
    butacas: Object.keys(seleccion.butacas),
  });
  rechazadas.forEach((codigo) => delete seleccion.butacas[codigo]);
  return rechazadas;
}

// El bloqueo dura tres minutos y se renueva al tocar el mapa; sin esto, quedarse
// leyendo el resumen o tipeando el mail alcanzaría para perder las butacas.
const RENOVAR_CADA_MS = 60_000;
let renovacion = null;

export function renovarMientrasSigaAca(funcionId) {
  clearInterval(renovacion);
  renovacion = setInterval(() => sostenerSeleccion(funcionId).catch(() => {}), RENOVAR_CADA_MS);
  // Irse de la pantalla corta la renovación, y lo que quede bloqueado vence solo. El
  // router no tiene dónde colgar una limpieza, pero el cambio de hash sí.
  window.addEventListener("hashchange", () => clearInterval(renovacion), { once: true });
}

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
