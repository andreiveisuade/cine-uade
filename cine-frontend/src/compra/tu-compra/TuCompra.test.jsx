import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "@/api/api-http.js";
import { renderConApp } from "@/test/renderConApp.jsx";
import { TuCompra } from "./TuCompra.jsx";

vi.mock("@/api/api-http.js");

const ANA = { id: 1, nombre: "Ana", email: "ana@mail.com", rol: "CLIENTE" };

const RESERVA = {
  id: 7, codigo: "ABCD1234", estado: "RESERVADA", cobrable: true, cancelable: true, funcionId: 5, total: 16000,
  venceEn: "2026-10-21T19:30:00", pelicula: { id: 2, titulo: "Matrix" }, sala: { nombre: "Sala 2", tipo: "IMAX" },
  funcion: { inicio: "2026-10-21T20:30:00", proyeccion: "DOS_D", idioma: "SUBTITULADA" },
  entradas: [
    { codigo: "C5", tarifa: "GENERAL", precio: 8000, requiereAcreditacion: false },
    { codigo: "C6", tarifa: "JUBILADO", precio: 8000, requiereAcreditacion: true },
  ],
  candy: [],
};

const COMBO = { id: 4, nombre: "Combo clásico", tipo: "COMBO", esCombo: true, precio: 5500, precioSuelto: 7000, ahorro: 1500,
  componentes: [{ productoId: 1, nombre: "Pochoclos", cantidad: 1 }] };
const POCHOCLOS = { id: 1, nombre: "Pochoclos", tipo: "POCHOCLOS", esCombo: false, precio: 3500 };

// La promo es del débito y corre solo sobre las entradas; el candy suma igual con cualquier medio.
const cotizacion = (candy) => [
  { medio: "DEBITO", entradas: 16000, descuento: 4800, promocionId: 3, promocion: "Miércoles 30 % OFF", candy, total: 11200 + candy },
  { medio: "CREDITO", entradas: 16000, descuento: 0, candy, total: 16000 + candy },
];

const conCombo = { ...RESERVA, candy: [{ id: 12, estado: "PENDIENTE", items: [{ productoId: 4, nombre: "Combo clásico", cantidad: 1, subtotal: 5500 }] }] };

const abrir = () => renderConApp(<TuCompra />, { ruta: "/compra/:codigo", en: "/compra/ABCD1234" });
const pagar = (texto) => screen.findByRole("button", { name: texto });

beforeEach(() => {
  vi.stubGlobal("location", { ...window.location, assign: vi.fn() });
  api.sesionActual.mockResolvedValue(ANA);
  api.obtenerReservaPorCodigo.mockResolvedValue(RESERVA);
  api.obtenerProductosCandy.mockResolvedValue([COMBO, POCHOCLOS]);
  api.obtenerCotizacion.mockResolvedValue(cotizacion(0));
});

describe("Tu compra: lo elegido", () => {
  it("muestra cada butaca con su tarifa, el carnet que se pide y hasta qué hora esperan las butacas", async () => {
    abrir();

    expect(await screen.findByText("C6")).toBeInTheDocument();
    expect(screen.getByText("Jubilado")).toBeInTheDocument();
    expect(screen.getByText("Acordate del carnet")).toBeInTheDocument();
    expect(screen.getByText("Tus butacas te esperan hasta las 19:30")).toBeInTheDocument();
  });

  it("sin sesión manda a ingresar", async () => {
    api.sesionActual.mockRejectedValue(new Error("401"));

    abrir();

    expect(await screen.findByText("en /ingresar")).toBeInTheDocument();
  });

  it("ya usada en la puerta, lleva al ticket", async () => {
    api.obtenerReservaPorCodigo.mockResolvedValue({ ...RESERVA, estado: "PAGADA", cobrable: false, ingresadaEn: "2026-10-21T20:20:00" });

    abrir();

    expect(await screen.findByText("en /ticket/ABCD1234")).toBeInTheDocument();
  });

  it("vencida dice por qué y deja elegir otra función", async () => {
    api.obtenerReservaPorCodigo.mockResolvedValue({ ...RESERVA, estado: "EXPIRADA", cobrable: false });

    abrir();

    expect(await screen.findByText("Esta reserva venció. Las butacas ya se liberaron.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Elegir otra función" })).toHaveAttribute("href", "/pelicula/2");
    expect(api.obtenerCotizacion).not.toHaveBeenCalled();
  });
});

describe("Tu compra: el candy", () => {
  it("sumar un combo manda el pedido entero y vuelve a cotizar, sin dejar pagar mientras tanto", async () => {
    api.pedirCandy.mockResolvedValue(conCombo);
    abrir();

    await userEvent.click(await screen.findByRole("button", { name: "Sumar Combo clásico" }));
    api.obtenerCotizacion.mockResolvedValue(cotizacion(5500));

    expect(screen.getByRole("button", { name: /^Pagar/ })).toBeDisabled();
    await waitFor(() => expect(api.pedirCandy).toHaveBeenCalledWith("ABCD1234", { 4: 1 }));
    expect(await pagar("Pagar $ 16.700 con Mercado Pago")).toBeEnabled();
    expect(screen.getByLabelText("1 Combo clásico")).toBeInTheDocument();
  });

  it("dos clics seguidos viajan en un solo pedido", async () => {
    api.pedirCandy.mockResolvedValue({ ...conCombo, candy: [{ ...conCombo.candy[0], items: [{ ...conCombo.candy[0].items[0], cantidad: 2 }] }] });
    abrir();

    await userEvent.click(await screen.findByRole("button", { name: "Sumar Combo clásico" }));
    await userEvent.click(screen.getByRole("button", { name: "Sumar otro Combo clásico" }));

    await waitFor(() => expect(api.pedirCandy).toHaveBeenCalledWith("ABCD1234", { 4: 2 }));
    expect(api.pedirCandy).toHaveBeenCalledOnce();
  });

  it("si el backend lo rechaza, lo dice y vuelve a lo que había", async () => {
    api.pedirCandy.mockRejectedValue(new Error("La reserva 7 venció: sus butacas volvieron a estar disponibles"));
    abrir();

    await userEvent.click(await screen.findByRole("button", { name: "Sumar Combo clásico" }));

    expect(await screen.findByText("La reserva 7 venció: sus butacas volvieron a estar disponibles")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sumar Combo clásico" })).toBeInTheDocument();
  });
});

describe("Tu compra: pagar", () => {
  it("de entrada elige el medio que menos cuesta, y cada uno dice su total", async () => {
    abrir();

    expect(await pagar("Pagar $ 11.200 con Mercado Pago")).toBeEnabled();
    expect(screen.getByText("Miércoles 30 % OFF, en las entradas")).toBeInTheDocument();
  });

  it("cada Pagar abre un checkout nuevo con el medio elegido y va a Mercado Pago", async () => {
    api.abrirCheckout.mockResolvedValue({ id: "7-CREDITO-1", urlPago: "https://pasarela/pagar/1" });
    abrir();

    await userEvent.click(await screen.findByRole("radio", { name: /Crédito/ }));
    await userEvent.click(await pagar("Pagar $ 16.000 con Mercado Pago"));
    await userEvent.click(await pagar("Pagar $ 16.000 con Mercado Pago"));

    expect(api.abrirCheckout).toHaveBeenCalledTimes(2);
    expect(api.abrirCheckout).toHaveBeenCalledWith("ABCD1234", "CREDITO");
    expect(window.location.assign).toHaveBeenCalledWith("https://pasarela/pagar/1");
    expect(JSON.parse(sessionStorage.getItem("cine.checkout"))).toEqual({ codigo: "ABCD1234", id: "7-CREDITO-1" });
  });

  it("al volver de Mercado Pago con el checkout abierto le avisa al backend y, cobrada, lleva al ticket", async () => {
    sessionStorage.setItem("cine.checkout", JSON.stringify({ codigo: "ABCD1234", id: "7-DEBITO-1" }));
    api.avisarVueltaDelPago.mockResolvedValue();
    api.obtenerReservaPorCodigo.mockResolvedValueOnce(RESERVA).mockResolvedValue({ ...RESERVA, estado: "PAGADA", cobrable: false });

    abrir();

    expect(await screen.findByText("en /ticket/ABCD1234")).toBeInTheDocument();
    expect(api.avisarVueltaDelPago).toHaveBeenCalledOnce();
    expect(api.avisarVueltaDelPago).toHaveBeenCalledWith("7-DEBITO-1");
    expect(sessionStorage.getItem("cine.checkout")).toBeNull();
  });

  it("abandonar cancela la reserva y vuelve al mapa", async () => {
    api.cancelarReservaPorCodigo.mockResolvedValue(RESERVA);
    abrir();

    await userEvent.click(await screen.findByRole("button", { name: "Abandonar la compra" }));

    expect(api.cancelarReservaPorCodigo).toHaveBeenCalledWith("ABCD1234");
    expect(await screen.findByText("en /funcion/5")).toBeInTheDocument();
  });
});

describe("Tu compra: sumar candy a una entrada paga", () => {
  const PAGADA = { ...RESERVA, estado: "PAGADA", cobrable: false, cancelable: false, venceEn: undefined };
  const soloCandy = [{ medio: "DEBITO", entradas: 0, descuento: 0, candy: 5500, total: 5500 }];

  beforeEach(() => api.obtenerReservaPorCodigo.mockResolvedValue(PAGADA));

  it("sin pedido no cotiza, no deja abandonar y vuelve a la entrada", async () => {
    abrir();

    expect(await screen.findByText("Sumá candy a tu entrada")).toBeInTheDocument();
    expect(screen.getByText(/Elegí algo del candy y pagalo acá/)).toBeInTheDocument();
    expect(api.obtenerCotizacion).not.toHaveBeenCalled();
    expect(screen.queryByRole("button", { name: "Abandonar la compra" })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Volver a la entrada" })).toHaveAttribute("href", "/ticket/ABCD1234");
  });

  it("al sumar un combo cotiza solo el candy y deja pagarlo", async () => {
    api.pedirCandy.mockResolvedValue({ ...PAGADA, candy: conCombo.candy });
    api.obtenerCotizacion.mockResolvedValue(soloCandy);
    abrir();

    await userEvent.click(await screen.findByRole("button", { name: "Sumar Combo clásico" }));

    expect(await pagar("Pagar $ 5.500 con Mercado Pago")).toBeEnabled();
    // «Entradas» queda solo en la tabla de lo elegido: el desglose del pago no muestra entradas en cero.
    expect(screen.getAllByText("Entradas")).toHaveLength(1);
  });
});
