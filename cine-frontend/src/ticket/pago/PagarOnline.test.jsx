import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "@/api/api-http.js";
import { renderConApp } from "@/test/renderConApp.jsx";
import { PagarOnline } from "./PagarOnline.jsx";

vi.mock("@/api/api-http.js");

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

const CHECKOUT = { id: "1-MERCADO_PAGO-9", monto: 4500, medio: "MERCADO_PAGO", urlPago: "https://pasarela/pagar/1" };

beforeEach(() => vi.stubGlobal("location", { ...window.location, assign: vi.fn() }));

async function irAPagar() {
  await userEvent.click(screen.getByRole("button", { name: "Pagar online" }));
  await userEvent.click(await screen.findByRole("button", { name: "Continuar" }));
  expect(await screen.findByText(/4\.500/)).toBeInTheDocument();
  await userEvent.click(screen.getByRole("button", { name: "Ir a pagar" }));
}

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

    expect(boton).toBeDisabled();
    expect(api.obtenerMediosDePago).toHaveBeenCalledOnce();
    entregar(MEDIOS);
    expect(await screen.findByRole("button", { name: "Continuar" })).toBeInTheDocument();
  });

  it("muestra el monto que calcula el backend e Ir a pagar navega a la pasarela en la misma pestaña", async () => {
    api.abrirCheckout.mockResolvedValue(CHECKOUT);
    renderConApp(<PagarOnline codigo="ABCD1234" />);

    await userEvent.click(screen.getByRole("button", { name: "Pagar online" }));
    await userEvent.click(await screen.findByRole("button", { name: "Continuar" }));

    expect(api.abrirCheckout).toHaveBeenCalledWith("ABCD1234", "MERCADO_PAGO");
    expect(await screen.findByText(/4\.500/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Ir a pagar" }));
    expect(window.location.assign).toHaveBeenCalledWith("https://pasarela/pagar/1");
  });

  it("Ya pagué le avisa la vuelta al backend y lee la reserva que vuelve a consultar el ticket, sin pedirla otra vez", async () => {
    api.abrirCheckout.mockResolvedValue(CHECKOUT);
    api.avisarVueltaDelPago.mockResolvedValue();
    const alVolver = vi.fn().mockResolvedValue({ codigo: "ABCD1234", estado: "PAGADA", cobrable: false });
    renderConApp(<PagarOnline codigo="ABCD1234" alVolver={alVolver} />);

    await irAPagar();
    await userEvent.click(await screen.findByRole("button", { name: "Ya pagué" }));

    expect(api.avisarVueltaDelPago).toHaveBeenCalledWith("1-MERCADO_PAGO-9");
    expect(alVolver).toHaveBeenCalled();
    expect(api.obtenerReservaPorCodigo).not.toHaveBeenCalled();
  });

  // Si MP rechazó, el motivo viaja en el 302 del retorno, que el fetch no lee: sin esto el botón no decía nada.
  it("Ya pagué con la reserva todavía sin pagar avisa que Mercado Pago no confirmó", async () => {
    api.abrirCheckout.mockResolvedValue(CHECKOUT);
    api.avisarVueltaDelPago.mockResolvedValue();
    const alVolver = vi.fn().mockResolvedValue({ codigo: "ABCD1234", estado: "RESERVADA", cobrable: true });
    renderConApp(<PagarOnline codigo="ABCD1234" alVolver={alVolver} />);

    await irAPagar();
    await userEvent.click(await screen.findByRole("button", { name: "Ya pagué" }));

    expect(await screen.findByText("Mercado Pago todavía no confirmó el pago")).toBeInTheDocument();
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
