import { act, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "@/api/api-http.js";
import { renderConApp } from "@/test/renderConApp.jsx";
import { Funcion } from "./Funcion.jsx";

vi.mock("@/api/api-http.js");

const asiento = (codigo, extra = {}) =>
  ({ codigo, fila: 1, numero: Number(codigo.slice(1)), tipo: "ESTANDAR", precio: 1000, precios: { GENERAL: 1000, JUBILADO: 433 }, ocupado: false, estado: "ACTIVA", ...extra });

const FUNCION = {
  id: 5, peliculaId: 2, pelicula: { titulo: "Matrix" }, inicio: "2026-10-21T20:00:00",
  sala: { nombre: "Sala 1", tipo: "DOS_D", filas: 1, capacidadSala: 3 },
  proyeccion: "DOS_D", idioma: "SUBTITULADA", libres: 2, precio: 1000,
  asientos: [asiento("A1"), asiento("A2"), asiento("A3", { ocupado: true })],
};

const butaca = (codigo) => screen.findByTitle(new RegExp(`^${codigo} ·`));

const abrir = () => renderConApp(<Funcion />, { ruta: "/funcion/:id", en: "/funcion/5" });

beforeEach(() => {
  api.sesionActual.mockRejectedValue(new Error("401"));
  api.obtenerFuncion.mockResolvedValue(FUNCION);
});

describe("Funcion: elegir butacas", () => {
  it("elegir una libre la suma a la selección y la bloquea en el backend", async () => {
    abrir();
    api.bloquearButacas.mockResolvedValue({ rechazadas: [] });

    await userEvent.click(await butaca("A1"));

    expect(api.bloquearButacas).toHaveBeenCalledWith(expect.objectContaining({ funcionId: 5, butacas: ["A1"] }));
    expect(screen.getByText("1 butaca")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Continuar" })).toBeEnabled();
  });

  it("la tarifa ofrece las que trae la butaca y muestra el precio que manda el backend", async () => {
    abrir();
    api.bloquearButacas.mockResolvedValue({ rechazadas: [] });
    await userEvent.click(await butaca("A1"));
    const tarifa = screen.getByRole("combobox");

    expect([...tarifa.options].map((o) => o.value)).toEqual(["GENERAL", "JUBILADO"]);
    await userEvent.selectOptions(tarifa, "JUBILADO");
    expect(screen.getByText("$ 433")).toBeInTheDocument();
  });

  it("una ocupada no se puede elegir", async () => {
    abrir();
    const ocupada = await butaca("A3");

    expect(ocupada).toBeDisabled();
    await userEvent.click(ocupada);
    expect(api.bloquearButacas).not.toHaveBeenCalled();
  });

  it("volver a tocar una elegida la suelta", async () => {
    abrir();
    api.bloquearButacas.mockResolvedValue({ rechazadas: [] });
    await userEvent.click(await butaca("A1"));

    await userEvent.click(await butaca("A1"));

    expect(api.bloquearButacas).toHaveBeenLastCalledWith(expect.objectContaining({ butacas: [] }));
    expect(screen.getByText("Elegí una o más butacas")).toBeInTheDocument();
  });

  // Dos clics antes de que React vuelva a pintar: el segundo no puede partir de la selección vieja.
  it("dos clics seguidos en butacas distintas suman las dos", async () => {
    abrir();
    api.bloquearButacas.mockResolvedValue({ rechazadas: [] });
    const [a1, a2] = [await butaca("A1"), await butaca("A2")];

    await act(async () => {
      a1.click();
      a2.click();
    });

    expect(api.bloquearButacas).toHaveBeenLastCalledWith(expect.objectContaining({ butacas: ["A1", "A2"] }));
    expect(screen.getByText("2 butacas")).toBeInTheDocument();
  });

  it("no manda un bloqueo mientras el anterior está en vuelo: el backend los tomaría a la vez y rechazaría la primera", async () => {
    abrir();
    let soltar;
    api.bloquearButacas
      .mockImplementationOnce(() => new Promise((r) => { soltar = () => r({ rechazadas: [] }); }))
      .mockResolvedValue({ rechazadas: [] });
    const [a1, a2] = [await butaca("A1"), await butaca("A2")];

    await act(async () => {
      a1.click();
      a2.click();
    });
    expect(api.bloquearButacas).toHaveBeenCalledTimes(1);

    await act(async () => soltar());
    await waitFor(() => expect(api.bloquearButacas).toHaveBeenCalledTimes(2));
    expect(api.bloquearButacas).toHaveBeenLastCalledWith(expect.objectContaining({ butacas: ["A1", "A2"] }));
    expect(screen.getByText("2 butacas")).toBeInTheDocument();
  });

  // El backend contesta 200 con `rechazadas` cuando otro ganó la butaca, y 400 cuando no aparta la selección entera
  // (tope de la compra, butaca fuera de servicio). En los dos la butaca recién pintada vuelve atrás.
  it.each([
    ["otro la ganó (rechazadas)", () => Promise.resolve({ rechazadas: ["A1"] }), "A1: alguien las está comprando"],
    ["el backend la rechaza (400)", () => Promise.reject(Object.assign(new Error("Pasaste el tope de 10 butacas"), { status: 400 })),
      "Pasaste el tope de 10 butacas"],
  ])("si %s vuelve atrás y muestra el error tal cual", async (_caso, respuesta, mensaje) => {
    abrir();
    api.bloquearButacas.mockImplementation(respuesta);

    await userEvent.click(await butaca("A1"));

    expect(await screen.findByText(mensaje)).toBeInTheDocument();
    expect(screen.getByText("Elegí una o más butacas")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Continuar" })).toBeDisabled();
    // El mapa se vuelve a pedir para mostrar lo que cambió.
    await waitFor(() => expect(api.obtenerFuncion).toHaveBeenCalledTimes(2));
  });
});

describe("Funcion: continuar sin sesión", () => {
  // R28: se compra con la cuenta de Google. Ir a Google recarga la página: lo elegido queda en la pestaña.
  it("renueva el bloqueo, guarda lo elegido y lleva a ingresar", async () => {
    abrir();
    api.bloquearButacas.mockResolvedValue({ rechazadas: [] });
    await userEvent.click(await butaca("A1"));
    api.bloquearButacas.mockClear();

    await userEvent.click(screen.getByRole("button", { name: "Continuar" }));

    expect(api.bloquearButacas).toHaveBeenCalledWith(expect.objectContaining({ funcionId: 5, butacas: ["A1"] }));
    expect(await screen.findByText("en /ingresar")).toBeInTheDocument();
    expect(JSON.parse(sessionStorage.getItem("cine.compra"))).toEqual({ funcionId: 5, butacas: { A1: "GENERAL" } });
  });
});

describe("Funcion: continuar con sesión", () => {
  beforeEach(() => api.sesionActual.mockResolvedValue({ id: 1, nombre: "Ana", email: "ana@mail.com", rol: "CLIENTE" }));

  async function elegirYContinuar() {
    api.bloquearButacas.mockResolvedValue({ rechazadas: [] });
    abrir();
    await userEvent.click(await butaca("A1"));
    await userEvent.click(screen.getByRole("button", { name: "Continuar" }));
  }

  it("reserva lo elegido con la sesión que bloqueó y sigue a Tu compra", async () => {
    api.crearReserva.mockResolvedValue({ codigo: "ABCD1234" });
    sessionStorage.setItem("cine.sesionDeCompra", "sesion-del-mapa");

    await elegirYContinuar();

    expect(api.crearReserva).toHaveBeenCalledWith({ funcionId: 5, butacas: { A1: "GENERAL" }, sesion: "sesion-del-mapa" });
    expect(await screen.findByText("en /compra/ABCD1234")).toBeInTheDocument();
  });

  it("si otra compra ganó una butaca (409) avisa, suelta lo elegido y vuelve a pedir el mapa", async () => {
    api.crearReserva.mockRejectedValue(Object.assign(new Error("La butaca A1 ya está ocupada"), { status: 409 }));

    await elegirYContinuar();

    expect(await screen.findByText("La butaca A1 ya está ocupada")).toBeInTheDocument();
    expect(screen.getByText("Elegí una o más butacas")).toBeInTheDocument();
    await waitFor(() => expect(api.obtenerFuncion).toHaveBeenCalledTimes(2));
  });

  it("otro rechazo se muestra al lado del botón y deja reintentar", async () => {
    api.crearReserva.mockRejectedValue(Object.assign(new Error("La función ya empezó: no se pueden reservar butacas"), { status: 400 }));

    await elegirYContinuar();

    expect(await screen.findByText("La función ya empezó: no se pueden reservar butacas")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Continuar" })).toBeEnabled();
  });
});
