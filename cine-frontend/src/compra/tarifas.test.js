import { describe, expect, it } from "vitest";
import { resumenCompra } from "./tarifas.js";

// Los precios por tarifa vienen del backend; a propósito no son butaca × multiplicador, para que se note si se recalcula.
const FUNCION = {
  asientos: [
    { codigo: "A1", precio: 1000, precios: { GENERAL: 1000, JUBILADO: 499.99 } },
    { codigo: "A2", precio: 1000, precios: { GENERAL: 1000, JUBILADO: 499.99 } },
    { codigo: "B1", precio: 1500, precios: { GENERAL: 1500, ESTUDIANTE: 1111.11 } },
  ],
};

describe("resumenCompra", () => {
  it.each([
    ["nada elegido", {}, [], 0],
    ["una general", { A1: "GENERAL" }, ["A1"], 1000],
    ["general más jubilado", { A1: "GENERAL", A2: "JUBILADO" }, ["A1", "A2"], 1499.99],
    ["estudiante en butaca cara", { B1: "ESTUDIANTE" }, ["B1"], 1111.11],
  ])("%s", (_caso, butacas, codigos, total) => {
    const resumen = resumenCompra(FUNCION, butacas);

    expect(resumen.elegidas.map((a) => a.codigo)).toEqual(codigos);
    expect(resumen.total).toBe(total);
  });
});
