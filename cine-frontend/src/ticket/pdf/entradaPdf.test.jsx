import { render } from "@testing-library/react";
import { jsPDF } from "jspdf";
import { QRCodeSVG } from "qrcode.react";
import { describe, expect, it, vi } from "vitest";
import { dibujarEntrada, leerQr, nombreDelPdf } from "./entradaPdf.js";

const reserva = {
  id: 7, codigo: "ABCD1234", estado: "PAGADA", total: 1700,
  pelicula: { titulo: "Matrix" }, sala: { nombre: "Sala 1", tipo: "IMAX" },
  funcion: { inicio: "2026-10-21T20:30:00", proyeccion: "TRES_D", idioma: "SUBTITULADA" },
  entradas: [
    { codigo: "A1", precio: 1000, tarifa: "GENERAL", requiereAcreditacion: false },
    { codigo: "A2", precio: 700, tarifa: "ESTUDIANTE", requiereAcreditacion: true },
  ],
};

function armar(laReserva = reserva) {
  const { container } = render(<QRCodeSVG value={laReserva.codigo} level="M" />);
  const qr = leerQr(container.querySelector("svg"));
  const doc = new jsPDF({ unit: "mm", format: "a4" });
  const text = vi.spyOn(doc, "text");
  dibujarEntrada(doc, laReserva, { qr, poster: null, enlace: "https://cine.test/#/ticket/ABCD1234" });
  // Los renglones de un texto partido se juntan con un espacio, como se leen.
  const textos = text.mock.calls.flatMap(([t]) => t).join(" ");
  return { doc, qr, textos };
}

describe("la entrada en PDF", () => {
  it("lleva la marca, la función, las butacas con tarifa y precio, el total, el estado y el código legible", () => {
    const { textos } = armar();

    for (const texto of ["CINE UADE", "Matrix", "Miércoles 21 oct", "20:30", "Sala 1 · IMAX", "3D · Subtitulada",
      "A1", "General", "$ 1.000", "A2", "Estudiante", "· traé el carnet", "$ 700", "Total · 2 entradas", "$ 1.700",
      "PAGADA", "ABCD 1234", "Mostralo en la puerta. Sirve una sola vez.", "https://cine.test/#/ticket/ABCD1234"]) {
      expect(textos).toContain(texto);
    }
  });

  it("una reserva sin pagar no lleva el sello y avisa que el código sirve cuando se pague", () => {
    const { textos } = armar({ ...reserva, estado: "RESERVADA" });

    expect(textos).toContain("RESERVADA");
    expect(textos).not.toContain("PAGADA");
    expect(textos).toContain("Todavía no está paga: el QR aparece cuando se pague.");
  });

  it("entra en una sola página aunque sean diez butacas, cinco combos y el título sea largo", () => {
    const diez = Array.from({ length: 10 }, (_, i) => ({ codigo: "F" + i, precio: 1000, tarifa: "GENERAL" }));
    const combos = Array.from({ length: 5 }, (_, i) => ({ productoId: i, nombre: "Combo " + i, cantidad: 2, subtotal: 9000 }));
    const { doc } = armar({ ...reserva, entradas: diez, pelicula: { titulo: "El señor de los anillos: la comunidad del anillo" },
      candy: [{ id: 1, estado: "COBRADA", items: combos }] });

    expect(doc.getNumberOfPages()).toBe(1);
  });

  // R30: lo cobrado se retira con el mismo QR; lo pedido sin pagar no se entrega y no va impreso.
  it("el candy cobrado va en un bloque a retirar, suma al total y lo sin pagar queda afuera", () => {
    const { textos } = armar({ ...reserva, candy: [
      { id: 1, estado: "COBRADA", retiradaEn: "2026-10-21T19:55:00", items: [{ productoId: 4, nombre: "Combo clásico", cantidad: 1, subtotal: 5500 }] },
      { id: 2, estado: "COBRADA", items: [{ productoId: 1, nombre: "Pochoclos", cantidad: 2, subtotal: 7000 }] },
      { id: 3, estado: "PENDIENTE", items: [{ productoId: 2, nombre: "Gaseosa", cantidad: 1, subtotal: 2200 }] },
    ] });

    for (const texto of ["CANDY A RETIRAR", "1 × Combo clásico", "RETIRADO", "2 × Pochoclos", "A RETIRAR", "$ 7.000",
      "Se retira en el candy con el mismo QR de la entrada.", "Total · 2 entradas y candy", "$ 14.200"]) {
      expect(textos).toContain(texto);
    }
    expect(textos).not.toContain("Gaseosa");
    expect(textos).not.toContain("SIN PAGAR");
  });

  it("sin candy cobrado no hay bloque de candy", () => {
    const { textos } = armar({ ...reserva, candy: [
      { id: 3, estado: "PENDIENTE", items: [{ productoId: 2, nombre: "Gaseosa", cantidad: 1, subtotal: 2200 }] },
    ] });

    expect(textos).not.toContain("CANDY A RETIRAR");
    expect(textos).toContain("Total · 2 entradas $");
  });

  it("el QR sale de las celdas que dibujó qrcode.react", () => {
    const { qr } = armar();

    expect(qr.celdas).toBe(21);
    expect(qr.tramos.length).toBeGreaterThan(50);
  });

  it("el archivo se llama con el código de la reserva", () => {
    expect(nombreDelPdf(reserva)).toBe("entrada-cine-uade-ABCD1234.pdf");
  });
});
