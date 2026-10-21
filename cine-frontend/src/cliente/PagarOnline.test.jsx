import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "../api/api-http.js";
import { renderConApp } from "../test/renderConApp.jsx";
import { PagarOnline } from "./PagarOnline.jsx";

vi.mock("../api/api-http.js");

const MEDIOS = [
  { nombre: "EFECTIVO", requiereAutorizacion: false },
  { nombre: "MERCADO_PAGO", requiereAutorizacion: true },
  { nombre: "TARJETA_CREDITO", requiereAutorizacion: true },
];

beforeEach(() => {
  api.sesionActual.mockResolvedValue({ nombre: "Ana" });
  api.obtenerMediosDePago.mockResolvedValue(MEDIOS);
});

afterEach(() => vi.unstubAllGlobals());

describe("PagarOnline", () => {
  it("ofrece solo los medios que pasan por la pasarela", async () => {
    renderConApp(<PagarOnline codigo="ABCD1234" />);

    await userEvent.click(screen.getByRole("button", { name: "Pagar online" }));

    const opciones = (await screen.findAllByRole("option")).map((o) => o.value);
    expect(opciones).toEqual(["MERCADO_PAGO", "TARJETA_CREDITO"]);
  });

  it("mientras llegan los medios el botón queda cargando y no pide dos veces", async () => {
    let entregar;
    api.obtenerMediosDePago.mockReturnValue(new Promise((ok) => (entregar = ok)));
    renderConApp(<PagarOnline codigo="ABCD1234" />);
    const boton = screen.getByRole("button", { name: "Pagar online" });

    await userEvent.click(boton);
    await userEvent.click(boton);

    expect(boton).toHaveAttribute("data-loading", "true");
    expect(api.obtenerMediosDePago).toHaveBeenCalledOnce();
    entregar(MEDIOS);
    expect(await screen.findByRole("button", { name: "Continuar" })).toBeInTheDocument();
  });

  it("muestra el monto que calcula el backend e Ir a pagar lleva a la pasarela", async () => {
    const ubicacion = {};
    vi.stubGlobal("location", ubicacion);
    api.abrirCheckout.mockResolvedValue({ monto: 4500, medio: "MERCADO_PAGO", urlPago: "https://pasarela/pagar/1" });
    renderConApp(<PagarOnline codigo="ABCD1234" />);

    await userEvent.click(screen.getByRole("button", { name: "Pagar online" }));
    await userEvent.click(await screen.findByRole("button", { name: "Continuar" }));

    expect(api.abrirCheckout).toHaveBeenCalledWith("ABCD1234", "MERCADO_PAGO");
    expect(await screen.findByText(/4\.500/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Ir a pagar" }));
    expect(window.location).toBe("https://pasarela/pagar/1");
  });

  it("si el checkout falla muestra el error del backend y deja elegir otro medio", async () => {
    api.abrirCheckout.mockRejectedValue(new Error("La reserva ya venció"));
    renderConApp(<PagarOnline codigo="ABCD1234" />);

    await userEvent.click(screen.getByRole("button", { name: "Pagar online" }));
    await userEvent.click(await screen.findByRole("button", { name: "Continuar" }));

    expect(await screen.findByText("La reserva ya venció")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Continuar" })).toBeEnabled();
  });
});
