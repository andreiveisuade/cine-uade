import { act, renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { useEnviar } from "./useEnviar.js";

describe("useEnviar", () => {
  it("queda enviando mientras viaja el pedido y lo suelta al terminar", async () => {
    let terminar;
    const { result } = renderHook(() => useEnviar());

    let envio;
    act(() => {
      envio = result.current.enviar(() => new Promise((ok) => (terminar = ok)));
    });
    expect(result.current.enviando).toBe(true);
    await act(async () => {
      terminar();
      await envio;
    });
    expect(result.current).toMatchObject({ enviando: false, error: null });
  });

  it("muestra el mensaje del backend y borra el anterior al reintentar", async () => {
    const { result } = renderHook(() => useEnviar());

    await act(() => result.current.enviar(() => Promise.reject(new Error("Clave incorrecta"))));
    expect(result.current).toMatchObject({ enviando: false, error: "Clave incorrecta" });

    await act(() => result.current.enviar(() => Promise.resolve()));
    expect(result.current.error).toBeNull();
  });
});
