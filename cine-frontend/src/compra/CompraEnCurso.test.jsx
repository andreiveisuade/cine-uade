import { act, renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { CompraEnCurso, useCompra } from "./CompraEnCurso.jsx";

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
