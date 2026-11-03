import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "@/api/api-http.js";
import { CompraEnCurso, useCompra } from "./CompraEnCurso.jsx";
import { useRenovarBloqueo } from "./useRenovarBloqueo.js";

vi.mock("@/api/api-http.js");

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
