import { renderHook, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import * as api from "../api/api-http.js";
import { renderConApp } from "../test/renderConApp.jsx";
import { AppCliente } from "./AppCliente.jsx";
import { SesionDelCliente, useSesion } from "./sesion.jsx";

vi.mock("../api/api-http.js");

const ANA = { nombre: "Ana", rol: "CLIENTE" };

describe("SesionDelCliente", () => {
  it.each([
    ["con cookie vigente queda el usuario que manda el backend", () => Promise.resolve(ANA), ANA],
    ["sin sesión (401) queda null", () => Promise.reject(Object.assign(new Error("401"), { status: 401 })), null],
  ])("%s", async (_caso, respuesta, esperado) => {
    api.sesionActual.mockImplementation(respuesta);
    const { result } = renderHook(() => useSesion(), { wrapper: SesionDelCliente });

    // undefined mientras se pregunta: ni logueado ni anónimo todavía.
    expect(result.current.usuario).toBeUndefined();
    await waitFor(() => expect(result.current.usuario).toEqual(esperado));
  });
});

describe("barra de la cuenta", () => {
  it.each([
    ["anónimo", () => Promise.reject(new Error("401")), ["Ingresar", "Registrarme"], ["Mis reservas", "Salir"]],
    ["con sesión", () => Promise.resolve(ANA), ["Ana", "Mis reservas", "Salir"], ["Ingresar", "Registrarme"]],
  ])("%s muestra solo sus enlaces", async (_caso, respuesta, visibles, ocultos) => {
    api.sesionActual.mockImplementation(respuesta);
    renderConApp(<AppCliente />, { ruta: "/*", en: "/nosotros" });

    for (const texto of visibles) expect(await screen.findByText(texto)).toBeInTheDocument();
    for (const texto of ocultos) expect(screen.queryByText(texto)).not.toBeInTheDocument();
  });

  it("Salir cierra la sesión en el backend y vuelve a la barra anónima", async () => {
    api.sesionActual.mockResolvedValue(ANA);
    api.salir.mockResolvedValue(null);
    api.obtenerGeneros.mockResolvedValue([]);
    api.obtenerCartelera.mockResolvedValue([]);
    renderConApp(<AppCliente />, { ruta: "/*", en: "/nosotros" });

    await userEvent.click(await screen.findByText("Salir"));

    expect(api.salir).toHaveBeenCalledOnce();
    expect(await screen.findByText("Ingresar")).toBeInTheDocument();
  });
});
