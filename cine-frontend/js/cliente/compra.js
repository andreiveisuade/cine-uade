import * as api from "../api.js";
import { etiqueta } from "../etiquetas.js";
import { precio } from "../formato.js";

export const seleccion = { funcionId: null, butacas: {} };

// sessionStorage: cada pestaña es una compra. No es credencial: la doble venta la impide la base.
const CLAVE_SESION = "cine.sesionDeCompra";

export function sesionDeCompra() {
  let sesion = sessionStorage.getItem(CLAVE_SESION);
  if (!sesion) {
    sesion = crypto.randomUUID();
    sessionStorage.setItem(CLAVE_SESION, sesion);
  }
  return sesion;
}

export async function sostenerSeleccion(funcionId) {
  const { rechazadas } = await api.bloquearButacas({
    funcionId,
    sesion: sesionDeCompra(),
    butacas: Object.keys(seleccion.butacas),
  });
  rechazadas.forEach((codigo) => delete seleccion.butacas[codigo]);
  return rechazadas;
}

const RENOVAR_CADA_MS = 60_000;
let renovacion = null;

export function renovarMientrasSigaAca(funcionId) {
  clearInterval(renovacion);
  renovacion = setInterval(() => sostenerSeleccion(funcionId).catch(() => {}), RENOVAR_CADA_MS);
  // El router no tiene dónde colgar una limpieza, pero el cambio de hash sí.
  window.addEventListener("hashchange", () => clearInterval(renovacion), { once: true });
}

let tarifas = null;

export async function catalogoTarifas() {
  if (!tarifas) tarifas = await api.obtenerTarifas();
  return tarifas;
}

export function tarifaPorNombre(nombre) {
  return (tarifas || []).find((t) => t.nombre === nombre) || { multiplicador: 1, requiereAcreditacion: false };
}

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

export function olvidarCliente() {
  localStorage.removeItem(CLAVE_CLIENTE);
}

// El listado por email no trae el código: el ticket y la cancelación usan los que se compraron acá.
const CLAVE_CODIGOS = "cine.codigos";

function codigosRecordados() {
  try {
    return JSON.parse(localStorage.getItem(CLAVE_CODIGOS)) || {};
  } catch {
    return {};
  }
}

export function codigoRecordado(reservaId) {
  return codigosRecordados()[reservaId] || null;
}

export function recordarCodigo(reservaId, codigo) {
  localStorage.setItem(CLAVE_CODIGOS,
    JSON.stringify({ ...codigosRecordados(), [reservaId]: codigo }));
}
