import { describe, expect, it } from "vitest";
import { armarComprobante } from "./armarComprobante.js";

const reserva = {
  id: 7, codigo: "ABCD1234", estado: "PAGADA", total: 2000, cliente: { nombre: "Ana" },
  pelicula: { titulo: "Matrix" }, sala: { nombre: "Sala 1", tipo: "IMAX" },
  funcion: { inicio: "2026-10-21T20:30:00", proyeccion: "DOS_D", idioma: "DOBLADA" },
  entradas: [{ codigo: "A1", precio: 1000, tarifa: "GENERAL" }, { codigo: "A2", precio: 1000, tarifa: "GENERAL" }],
};

describe("el comprobante en texto", () => {
  it("con promo muestra el descuento y lo cobrado", () => {
    const texto = armarComprobante({ ...reserva, pago: { monto: 1000, descuento: 1000 } });
    expect(texto).toMatch(/Descuento\s*: -\$ 1000\.00/);
    expect(texto).toMatch(/Total\s*: \$ 1000\.00/);
  });

  it("sin promo no nombra el descuento", () => {
    const texto = armarComprobante({ ...reserva, pago: { monto: 2000, descuento: 0 } });
    expect(texto).not.toContain("Descuento");
    expect(texto).toMatch(/Total\s*: \$ 2000\.00/);
  });
});
