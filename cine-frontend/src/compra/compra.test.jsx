import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "@/api/api-http.js";
import { resumenCompra } from "./tarifas.js";
import { CompraEnCurso, useCompra } from "./CompraEnCurso.jsx";
import { useRenovarBloqueo } from "./useRenovarBloqueo.js";

vi.mock("@/api/api-http.js");

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

describe("CompraEnCurso", () => {
  // La vuelta de Google es una página nueva: la compra se lee de la pestaña, no de la memoria.
  it("lo elegido sigue después de recargar la página", () => {
    const antes = renderHook(() => useCompra(), { wrapper: CompraEnCurso });
    act(() => antes.result.current.setSeleccion({ funcionId: 5, butacas: { A1: "JUBILADO" } }));
    antes.unmount();

    const despues = renderHook(() => useCompra(), { wrapper: CompraEnCurso });

    expect(despues.result.current.seleccion).toEqual({ funcionId: 5, butacas: { A1: "JUBILADO" } });
  });
});
