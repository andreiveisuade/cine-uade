import { describe, expect, it } from "vitest";
import { totalDeLaEntrada } from "./estadoReserva.js";

const candy = [
  { id: 1, estado: "COBRADA", items: [{ productoId: 1, subtotal: 7000 }] },
  { id: 2, estado: "PENDIENTE", items: [{ productoId: 2, subtotal: 2200 }] },
];
const reserva = (estado, conCandy = candy) => ({ estado, total: 2000, entradas: [{}, {}], candy: conCandy });

describe("totalDeLaEntrada: la pantalla y el PDF lo cuentan igual", () => {
  it.each([
    ["sin pagar suma el candy pendiente, que se cobra con la reserva", reserva("RESERVADA"), "Total · 2 entradas y candy", 11200],
    ["paga suma solo lo cobrado", reserva("PAGADA"), "Total · 2 entradas y candy", 9000],
    ["paga y con todo el candy pendiente no lo nombra", reserva("PAGADA", [candy[1]]), "Total · 2 entradas", 2000],
    ["sin candy son las entradas", { ...reserva("PAGADA"), candy: undefined, entradas: [{}] }, "Total · 1 entrada", 2000],
  ])("%s", (_caso, laReserva, texto, importe) => {
    expect(totalDeLaEntrada(laReserva)).toEqual({ texto, importe });
  });
});
