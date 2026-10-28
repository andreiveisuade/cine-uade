import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { Route, Routes, useParams } from "react-router";
import * as api from "@/api/api-http.js";
import { renderConApp } from "@/test/renderConApp.jsx";
import { MisReservas } from "./MisReservas.jsx";

vi.mock("@/api/api-http.js");

const ANA = { id: 1, nombre: "Ana", email: "ana@mail.com", rol: "CLIENTE" };

const reserva = (id, codigo, estado, extra = {}) => ({
  id, codigo, estado, cancelable: false, cobrable: false, cantidadEntradas: 2, total: 2000,
  pelicula: { titulo: "Matrix" }, sala: { nombre: "Sala 1" }, funcion: { inicio: "2026-10-21T20:00:00" }, ...extra,
});

// El ticket como pantalla aparte: así se ve a qué código llevó la búsqueda.
function TicketBuscado() {
  return <p>ticket {useParams().codigo}</p>;
}

const abrir = () => renderConApp(
  <Routes>
    <Route path="mis-reservas" element={<MisReservas />} />
    <Route path="ticket/:codigo" element={<TicketBuscado />} />
  </Routes>,
  { ruta: "/*", en: "/mis-reservas" },
);

const fila = (codigo) => screen.getByRole("link", { name: codigo }).closest(".mantine-Paper-root");

describe("MisReservas sin sesión: buscar por código", () => {
  beforeEach(() => { api.sesionActual.mockRejectedValue(new Error("401")); });

  it("lleva al ticket con el código sin espacios y en mayúsculas", async () => {
    abrir();

    await userEvent.type(await screen.findByLabelText(/^Código de acceso/), "abcd 1234");
    await userEvent.click(screen.getByRole("button", { name: "Buscar" }));

    expect(await screen.findByText("ticket ABCD1234")).toBeInTheDocument();
    expect(api.obtenerMisReservas).not.toHaveBeenCalled();
  });

  it("con el código en blanco no busca y dice qué falta", async () => {
    abrir();

    await userEvent.type(await screen.findByLabelText(/^Código de acceso/), "   ");
    await userEvent.click(screen.getByRole("button", { name: "Buscar" }));

    expect(screen.getByText("Completá el código de tu ticket")).toBeInTheDocument();
    expect(screen.queryByText(/^ticket /)).not.toBeInTheDocument();
  });
});

describe("MisReservas con sesión: las reservas de la cuenta", () => {
  beforeEach(() => { api.sesionActual.mockResolvedValue(ANA); });

  it("sin reservas invita a ver la cartelera", async () => {
    api.obtenerMisReservas.mockResolvedValue([]);

    abrir();

    expect(await screen.findByText("Todavía no tenés reservas.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Ver la cartelera" })).toHaveAttribute("href", "/cartelera");
  });

  it("si no puede traerlas muestra el error", async () => {
    api.obtenerMisReservas.mockRejectedValue(new Error("Algo salió mal en el servidor: probá de nuevo en un momento"));

    abrir();

    expect(await screen.findByText("Algo salió mal en el servidor: probá de nuevo en un momento")).toBeInTheDocument();
  });

  it("cada reserva muestra la función, su estado y lleva a su ticket", async () => {
    api.obtenerMisReservas.mockResolvedValue([
      reserva(1, "AAAA1111", "PAGADA"),
      reserva(2, "BBBB2222", "EXPIRADA", { cantidadEntradas: 1, total: 1000 }),
    ]);

    abrir();

    await screen.findByRole("link", { name: "AAAA1111" });
    expect(within(fila("AAAA1111")).getByText("Pagada")).toBeInTheDocument();
    expect(within(fila("AAAA1111")).getByText(/Sala 1 · 2 entradas · \$ 2\.000/)).toBeInTheDocument();
    expect(within(fila("BBBB2222")).getByText("Vencida")).toBeInTheDocument();
    expect(within(fila("BBBB2222")).getByText(/1 entrada · \$ 1\.000/)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "AAAA1111" })).toHaveAttribute("href", "/ticket/AAAA1111");
  });

  // Qué se puede hacer lo decide el backend con `cancelable` y `cobrable`; la pantalla solo lo obedece.
  it.each([
    ["cancelable y cobrable", { cancelable: true, cobrable: true }, true, true],
    ["solo cancelable", { cancelable: true }, true, false],
    ["ni una ni otra", {}, false, false],
  ])("una reserva %s ofrece lo que corresponde", async (_caso, permisos, cancelar, pagar) => {
    api.obtenerMisReservas.mockResolvedValue([reserva(1, "AAAA1111", "RESERVADA", permisos)]);

    abrir();

    await screen.findByRole("link", { name: "AAAA1111" });
    const acciones = within(fila("AAAA1111"));
    expect(acciones.queryByRole("button", { name: "Cancelar" }) !== null).toBe(cancelar);
    expect(acciones.queryByRole("button", { name: "Pagar online" }) !== null).toBe(pagar);
  });

  it("la que espera el pago dice hasta qué hora; la paga no", async () => {
    api.obtenerMisReservas.mockResolvedValue([
      reserva(1, "AAAA1111", "RESERVADA", { venceEn: "2026-10-21T19:30:00" }),
      reserva(2, "BBBB2222", "PAGADA"),
    ]);
    abrir();

    await screen.findByRole("link", { name: "AAAA1111" });
    expect(within(fila("AAAA1111")).getByText(/pagala antes de las 19:30/)).toBeInTheDocument();
    expect(within(fila("BBBB2222")).queryByText(/pagala antes/)).not.toBeInTheDocument();
  });

  it("cancelar avisa y vuelve a pedir la lista", async () => {
    api.obtenerMisReservas
      .mockResolvedValueOnce([reserva(1, "AAAA1111", "RESERVADA", { cancelable: true })])
      .mockResolvedValue([reserva(1, "AAAA1111", "CANCELADA")]);
    api.cancelarReservaPorCodigo.mockResolvedValue(null);
    abrir();

    await userEvent.click(await screen.findByRole("button", { name: "Cancelar" }));

    expect(api.cancelarReservaPorCodigo).toHaveBeenCalledWith("AAAA1111");
    expect(await screen.findByText("Reserva cancelada, las butacas quedaron libres")).toBeInTheDocument();
    expect(await screen.findByText("Cancelada")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Cancelar" })).not.toBeInTheDocument();
  });

  it("si el backend no la deja cancelar avisa el motivo y la reserva queda como estaba", async () => {
    api.obtenerMisReservas.mockResolvedValue([reserva(1, "AAAA1111", "RESERVADA", { cancelable: true })]);
    api.cancelarReservaPorCodigo.mockRejectedValue(new Error("La función ya empezó"));
    abrir();

    await userEvent.click(await screen.findByRole("button", { name: "Cancelar" }));

    expect(await screen.findByText("La función ya empezó")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Cancelar" })).toBeEnabled();
    expect(api.obtenerMisReservas).toHaveBeenCalledTimes(1);
  });
});
