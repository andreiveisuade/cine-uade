import { useState } from "react";
import * as api from "@/api/api-http.js";
import { useCargar } from "@/compartido/hooks/useCargar.js";
import { pedidoPendiente } from "@/compartido/estadoReserva.js";
import { useCandyDeLaCompra } from "./useCandyDeLaCompra.js";

// Nombre y precio de cada producto del pedido: de la carta, o del pedido guardado si ya no está a la venta.
function catalogoDe(productos, reserva) {
  const guardados = (pedidoPendiente(reserva)?.items ?? [])
    .map((i) => [i.productoId, { nombre: i.nombre, precio: i.precioUnitario, ahorro: 0 }]);
  return new Map([...guardados, ...productos.map((p) => [p.id, p])]);
}

// Lo que lleva la compra y cuánto se paga: el candy elegido, la cotización del backend y el total a mostrar.
export function usePedidoDeLaCompra(reserva, productos, sumar, alFallar) {
  const [guardados, setGuardados] = useState(0);
  const candy = useCandyDeLaCompra(reserva, () => setGuardados((n) => n + 1), alFallar);
  // Paga y sin pedido no hay nada que cotizar: el backend daría 409.
  const cotiza = !sumar || candy.hayPedido;
  const cotizacion = useCargar(() => (cotiza ? api.obtenerCotizacion(reserva.codigo) : Promise.resolve([])),
    [reserva.codigo, cotiza, guardados]);

  const catalogo = catalogoDe(productos, reserva);
  const items = Object.entries(candy.cantidades).map(([id, cantidad]) => ({ id: Number(id), cantidad, ...catalogo.get(Number(id)) }));
  const pedido = {
    items,
    total: items.reduce((t, i) => t + i.precio * i.cantidad, 0),
    ahorro: items.reduce((t, i) => t + (i.ahorro ?? 0) * i.cantidad, 0),
  };

  // La web cobra con un solo medio, Mercado Pago, y el backend cotiza solo ese. Mientras el pedido viaja y hasta que
  // llega la cotización nueva, muestra ya el candy nuevo (la promo no lo toca): es la única cuenta de plata del front.
  const actualizando = candy.actualizando || cotizacion.cargando;
  const cotizada = (cotizacion.datos ?? cotizacion.previos ?? [])[0];
  const aPagar = cotizada && actualizando
    ? { ...cotizada, candy: pedido.total, total: cotizada.entradas - cotizada.descuento + pedido.total }
    : cotizada;

  return { candy, pedido, cotiza, cotizacion, aPagar, actualizando, total: aPagar?.total ?? pedido.total };
}
