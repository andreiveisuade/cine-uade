import { act, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import confetti from "canvas-confetti";
import * as api from "../api/api-http.js";
import { renderConApp } from "../test/renderConApp.jsx";
import { Ticket } from "./Ticket.jsx";

vi.mock("../api/api-http.js");
vi.mock("canvas-confetti", () => ({ default: Object.assign(vi.fn(), { reset: vi.fn() }) }));

const reserva = (estado, cobrable = false) => ({
  id: 1, codigo: "ABCD1234", estado, cobrable, total: 2000,
  pelicula: { titulo: "Matrix" }, sala: { nombre: "Sala 1", tipo: "DOS_D" },
  funcion: { inicio: "2026-10-21T20:00:00", proyeccion: "DOS_D", idioma: "SUBTITULADA" },
  cliente: { nombre: "Ana", email: "ana@mail.com" },
  entradas: [{ codigo: "A1", precio: 1000, tarifa: "GENERAL" }, { codigo: "A2", precio: 1000, tarifa: "GENERAL" }],
});

const abrir = () => renderConApp(<Ticket />, { ruta: "/ticket/:codigo", en: "/ticket/ABCD1234" });

// Solo el intervalo es falso: los findBy de Testing Library siguen esperando con timeouts reales.
const pasar = (ms) => act(() => vi.advanceTimersByTimeAsync(ms));

beforeEach(() => {
  vi.useFakeTimers({ toFake: ["setInterval", "clearInterval"] });
  api.sesionActual.mockRejectedValue(new Error("401"));
  api.obtenerProductosCandy.mockResolvedValue([]);
});

afterEach(() => vi.useRealTimers());

describe("Ticket", () => {
  it.each([
    ["RESERVADA", "Reserva confirmada. Pagala online o en boletería antes de que venza."],
    ["PAGADA", "Entrada paga. Mostrá el código en la puerta."],
    ["EXPIRADA", "Esta reserva venció. Las butacas ya se liberaron."],
    ["CANCELADA", "Esta reserva está cancelada."],
  ])("con estado %s le dice al comprador lo que corresponde", async (estado, texto) => {
    api.obtenerReservaPorCodigo.mockResolvedValue(reserva(estado));

    abrir();

    expect(await screen.findByText(texto)).toBeInTheDocument();
  });

  it("mientras espera el pago vuelve a pedir cada 10 s y deja de pedir al pasar a PAGADA", async () => {
    api.obtenerReservaPorCodigo
      .mockResolvedValueOnce(reserva("RESERVADA", true))
      .mockResolvedValueOnce(reserva("RESERVADA", true))
      .mockResolvedValue(reserva("PAGADA"));
    abrir();
    await screen.findByText(/Reserva confirmada/);

    await pasar(10_000);
    expect(api.obtenerReservaPorCodigo).toHaveBeenCalledTimes(2);
    await pasar(10_000);
    expect(await screen.findByText("Entrada paga. Mostrá el código en la puerta.")).toBeInTheDocument();

    await pasar(60_000);
    expect(api.obtenerReservaPorCodigo).toHaveBeenCalledTimes(3);
  });

  it("deja de pedir al salir de la pantalla", async () => {
    api.obtenerReservaPorCodigo.mockResolvedValue(reserva("RESERVADA", true));
    const { unmount } = abrir();
    await screen.findByText(/Reserva confirmada/);

    unmount();
    await pasar(60_000);

    expect(api.obtenerReservaPorCodigo).toHaveBeenCalledOnce();
  });

  it("una reserva que no se puede cobrar online no se vuelve a pedir", async () => {
    api.obtenerReservaPorCodigo.mockResolvedValue(reserva("RESERVADA", false));
    abrir();
    await screen.findByText(/Reserva confirmada/);

    await pasar(60_000);

    expect(api.obtenerReservaPorCodigo).toHaveBeenCalledOnce();
  });

  it("tira confetti una sola vez cuando el pago llega con la pantalla abierta", async () => {
    api.obtenerReservaPorCodigo
      .mockResolvedValueOnce(reserva("RESERVADA", true))
      .mockResolvedValue(reserva("PAGADA"));
    abrir();
    await screen.findByText(/Reserva confirmada/);

    await pasar(10_000);
    await screen.findByText(/Entrada paga/);
    await vi.waitFor(() => expect(confetti).toHaveBeenCalledOnce());
    await pasar(60_000);

    expect(confetti).toHaveBeenCalledOnce();
  });

  it("no tira confetti si el ticket se abre ya pagado", async () => {
    api.obtenerReservaPorCodigo.mockResolvedValue(reserva("PAGADA"));
    abrir();
    await screen.findByText(/Entrada paga/);

    await pasar(30_000);

    expect(confetti).not.toHaveBeenCalled();
  });
});
