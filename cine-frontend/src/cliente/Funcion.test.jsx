import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "../api/api-http.js";
import { renderConApp } from "../test/renderConApp.jsx";
import { Funcion } from "./Funcion.jsx";

vi.mock("../api/api-http.js");

const asiento = (codigo, extra = {}) =>
  ({ codigo, fila: 1, numero: Number(codigo.slice(1)), tipo: "ESTANDAR", precio: 1000, ocupado: false, estado: "ACTIVA", ...extra });

const FUNCION = {
  id: 5, peliculaId: 2, pelicula: { titulo: "Matrix" }, inicio: "2026-10-21T20:00:00",
  sala: { nombre: "Sala 1", tipo: "DOS_D", filas: 1, capacidadSala: 3 },
  proyeccion: "DOS_D", idioma: "SUBTITULADA", libres: 2, precio: 1000,
  asientos: [asiento("A1"), asiento("A2"), asiento("A3", { ocupado: true })],
};

const butaca = (codigo) => screen.findByTitle(new RegExp(`^${codigo} ·`));

beforeEach(() => {
  api.sesionActual.mockRejectedValue(new Error("401"));
  api.obtenerTarifas.mockResolvedValue([{ nombre: "GENERAL", multiplicador: 1 }]);
  api.obtenerFuncion.mockResolvedValue(FUNCION);
  renderConApp(<Funcion />, { ruta: "/funcion/:id", en: "/funcion/5" });
});

describe("Funcion: elegir butacas", () => {
  it("elegir una libre la suma a la selección y la bloquea en el backend", async () => {
    api.bloquearButacas.mockResolvedValue({ rechazadas: [] });

    await userEvent.click(await butaca("A1"));

    expect(api.bloquearButacas).toHaveBeenCalledWith(expect.objectContaining({ funcionId: 5, butacas: ["A1"] }));
    expect(screen.getByText("1 butaca")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Continuar" })).toBeEnabled();
  });

  it("una ocupada no se puede elegir", async () => {
    const ocupada = await butaca("A3");

    expect(ocupada).toBeDisabled();
    await userEvent.click(ocupada);
    expect(api.bloquearButacas).not.toHaveBeenCalled();
  });

  it("volver a tocar una elegida la suelta", async () => {
    api.bloquearButacas.mockResolvedValue({ rechazadas: [] });
    await userEvent.click(await butaca("A1"));

    await userEvent.click(await butaca("A1"));

    expect(api.bloquearButacas).toHaveBeenLastCalledWith(expect.objectContaining({ butacas: [] }));
    expect(screen.getByText("Elegí una o más butacas")).toBeInTheDocument();
  });

  // El backend contesta 200 con `rechazadas` cuando otro ganó la butaca, y 400 cuando no aparta la selección entera
  // (tope de la compra, butaca fuera de servicio). En los dos la butaca recién pintada vuelve atrás.
  it.each([
    ["otro la ganó (rechazadas)", () => Promise.resolve({ rechazadas: ["A1"] }), "A1: alguien las está comprando"],
    ["el backend la rechaza (400)", () => Promise.reject(Object.assign(new Error("Pasaste el tope de 10 butacas"), { status: 400 })),
      "Pasaste el tope de 10 butacas"],
  ])("si %s vuelve atrás y muestra el error tal cual", async (_caso, respuesta, mensaje) => {
    api.bloquearButacas.mockImplementation(respuesta);

    await userEvent.click(await butaca("A1"));

    expect(await screen.findByText(mensaje)).toBeInTheDocument();
    expect(screen.getByText("Elegí una o más butacas")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Continuar" })).toBeDisabled();
    // El mapa se vuelve a pedir para mostrar lo que cambió.
    await waitFor(() => expect(api.obtenerFuncion).toHaveBeenCalledTimes(2));
  });
});
