import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "../api/api-http.js";
import { catalogoTarifas, CompraEnCurso, resumenCompra, useCompra, useRenovarBloqueo } from "./compra.jsx";

vi.mock("../api/api-http.js");

const TARIFAS = [
  { nombre: "GENERAL", multiplicador: 1 },
  { nombre: "JUBILADO", multiplicador: 0.5 },
  { nombre: "ESTUDIANTE", multiplicador: 0.75 },
];

const FUNCION = {
  asientos: [
    { codigo: "A1", precio: 1000 },
    { codigo: "A2", precio: 1000 },
    { codigo: "B1", precio: 1500 },
  ],
};

describe("resumenCompra", () => {
  beforeAll(async () => {
    api.obtenerTarifas.mockResolvedValue(TARIFAS);
    await catalogoTarifas();
  });

  it.each([
    ["nada elegido", {}, [], 0],
    ["una general", { A1: "GENERAL" }, ["A1"], 1000],
    ["general más jubilado", { A1: "GENERAL", A2: "JUBILADO" }, ["A1", "A2"], 1500],
    ["estudiante en butaca cara", { B1: "ESTUDIANTE" }, ["B1"], 1125],
    ["tarifa que no está en el catálogo cuenta como general", { B1: "INVENTADA" }, ["B1"], 1500],
  ])("%s", (_caso, butacas, codigos, total) => {
    const resumen = resumenCompra(FUNCION, butacas);

    expect(resumen.elegidas.map((a) => a.codigo)).toEqual(codigos);
    expect(resumen.total).toBe(total);
  });
});

describe("useRenovarBloqueo", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  // La selección y la renovación en el mismo hook: el test elige butacas como lo haría el mapa.
  function montar(funcionId) {
    return renderHook(() => {
      const compra = useCompra();
      useRenovarBloqueo(funcionId);
      return compra;
    }, { wrapper: CompraEnCurso });
  }

  it("renueva el bloqueo de lo elegido cada minuto", async () => {
    api.bloquearButacas.mockResolvedValue({ rechazadas: [] });
    const { result } = montar(5);
    act(() => result.current.setSeleccion({ funcionId: 5, butacas: { A1: "GENERAL" } }));

    await act(() => vi.advanceTimersByTimeAsync(60_000));
    expect(api.bloquearButacas).toHaveBeenCalledWith(expect.objectContaining({ funcionId: 5, butacas: ["A1"] }));

    await act(() => vi.advanceTimersByTimeAsync(60_000));
    expect(api.bloquearButacas).toHaveBeenCalledTimes(2);
  });

  it("suelta de la selección las butacas que el backend ya no le sostiene", async () => {
    api.bloquearButacas.mockResolvedValue({ rechazadas: ["A2"] });
    const { result } = montar(5);
    act(() => result.current.setSeleccion({ funcionId: 5, butacas: { A1: "GENERAL", A2: "GENERAL" } }));

    await act(() => vi.advanceTimersByTimeAsync(60_000));

    expect(result.current.seleccion.butacas).toEqual({ A1: "GENERAL" });
  });

  it("no renueva una selección de otra función", async () => {
    api.bloquearButacas.mockResolvedValue({ rechazadas: [] });
    const { result } = montar(5);
    act(() => result.current.setSeleccion({ funcionId: 9, butacas: { A1: "GENERAL" } }));

    await act(() => vi.advanceTimersByTimeAsync(60_000));

    expect(api.bloquearButacas).not.toHaveBeenCalled();
  });

  it("deja de renovar al desmontar", async () => {
    api.bloquearButacas.mockResolvedValue({ rechazadas: [] });
    const { result, unmount } = montar(5);
    act(() => result.current.setSeleccion({ funcionId: 5, butacas: { A1: "GENERAL" } }));

    unmount();
    await vi.advanceTimersByTimeAsync(180_000);

    expect(api.bloquearButacas).not.toHaveBeenCalled();
    expect(vi.getTimerCount()).toBe(0);
  });
});
