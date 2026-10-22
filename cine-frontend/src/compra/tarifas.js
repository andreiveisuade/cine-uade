// Los precios por tarifa los calcula el backend y vienen en cada butaca de la función: acá solo se leen y se suman.
export const precioConTarifa = (asiento, nombreTarifa) => asiento.precios[nombreTarifa];

/** Las butacas elegidas de esa función y lo que suman con sus tarifas: el mapa y la confirmación dicen lo mismo. */
export function resumenCompra(funcion, butacas) {
  const elegidas = funcion.asientos.filter((a) => butacas[a.codigo]);
  const total = elegidas.reduce((suma, a) => suma + precioConTarifa(a, butacas[a.codigo]), 0);
  return { elegidas, total };
}
