import * as api from "@/api/api-http.js";

// Se pide una vez: las pantallas de compra lo cargan antes de dibujar y después se lee sin esperar.
let tarifas = null;

export async function catalogoTarifas() {
  if (!tarifas) tarifas = await api.obtenerTarifas();
  return tarifas;
}

export const tarifasCargadas = () => tarifas || [];

export function tarifaPorNombre(nombre) {
  return tarifasCargadas().find((t) => t.nombre === nombre) || { multiplicador: 1, requiereAcreditacion: false };
}

/** El precio de esa butaca con esa tarifa. asiento.precio siempre viene en GENERAL. */
export function precioConTarifa(asiento, nombreTarifa) {
  return Math.round(asiento.precio * tarifaPorNombre(nombreTarifa).multiplicador * 100) / 100;
}

/** Las butacas elegidas de esa función y lo que suman con sus tarifas: el mapa y la confirmación dicen lo mismo. */
export function resumenCompra(funcion, butacas) {
  const elegidas = funcion.asientos.filter((a) => butacas[a.codigo]);
  const total = elegidas.reduce((suma, a) => suma + precioConTarifa(a, butacas[a.codigo]), 0);
  return { elegidas, total };
}
