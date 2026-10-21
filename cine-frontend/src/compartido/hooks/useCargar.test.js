import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useCargar } from "./useCargar.js";

// Una promesa que el test resuelve cuando quiere: así se ordena a mano qué respuesta llega primero.
function diferida() {
  let resolver, rechazar;
  const promesa = new Promise((ok, mal) => {
    resolver = ok;
    rechazar = mal;
  });
  return { promesa, resolver, rechazar };
}

describe("useCargar", () => {
  it("arranca cargando y después entrega los datos", async () => {
    const { result } = renderHook(() => useCargar(() => Promise.resolve(["Matrix"]), []));

    expect(result.current).toMatchObject({ cargando: true, datos: null, error: null });
    await waitFor(() => expect(result.current.cargando).toBe(false));
    expect(result.current.datos).toEqual(["Matrix"]);
  });

  it("muestra el mensaje del backend cuando el pedido falla", async () => {
    const { result } = renderHook(() =>
      useCargar(() => Promise.reject(new Error("La película no existe")), []));

    await waitFor(() => expect(result.current.error).toBe("La película no existe"));
    expect(result.current).toMatchObject({ cargando: false, datos: null });
  });

  it("muestra la película nueva y no la anterior mientras carga", async () => {
    const pedidos = { 3: diferida(), 4: diferida() };
    const { result, rerender } = renderHook(({ id }) => useCargar(() => pedidos[id].promesa, [id]),
      { initialProps: { id: 3 } });
    await act(() => pedidos[3].resolver("película 3"));
    expect(result.current.datos).toBe("película 3");

    rerender({ id: 4 });

    expect(result.current).toMatchObject({ datos: null, cargando: true, previos: "película 3" });
    await act(() => pedidos[4].resolver("película 4"));
    expect(result.current.datos).toBe("película 4");
  });

  it("no deja que la respuesta vieja que llega tarde pise la nueva", async () => {
    const pedidos = { Matri: diferida(), Matrix: diferida() };
    const { result, rerender } = renderHook(({ q }) => useCargar(() => pedidos[q].promesa, [q]),
      { initialProps: { q: "Matri" } });
    rerender({ q: "Matrix" });

    await act(() => pedidos.Matrix.resolver("de Matrix"));
    await act(() => pedidos.Matri.resolver("de Matri"));

    expect(result.current.datos).toBe("de Matrix");
    expect(result.current.previos).toBe("de Matrix");
  });

  it("recargar vuelve a pedir y deja los datos a la vista mientras tanto", async () => {
    const pedir = vi.fn().mockResolvedValueOnce("primera").mockResolvedValueOnce("segunda");
    const { result } = renderHook(() => useCargar(pedir, [1]));
    await waitFor(() => expect(result.current.datos).toBe("primera"));

    act(() => result.current.recargar());

    expect(result.current).toMatchObject({ datos: "primera", cargando: true });
    await waitFor(() => expect(result.current.datos).toBe("segunda"));
    expect(pedir).toHaveBeenCalledTimes(2);
  });

  it("con las mismas deps en otro render no vuelve a pedir", async () => {
    const pedir = vi.fn().mockResolvedValue("datos");
    const { rerender, result } = renderHook(({ id }) => useCargar(pedir, [id]), { initialProps: { id: 7 } });
    await waitFor(() => expect(result.current.cargando).toBe(false));

    rerender({ id: 7 });

    expect(pedir).toHaveBeenCalledTimes(1);
  });
});
