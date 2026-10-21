import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import confetti from "canvas-confetti";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "../api/api-http.js";
import { renderConApp } from "../test/renderConApp.jsx";
import { Promociones } from "./Promociones.jsx";

vi.mock("../api/api-http.js");
vi.mock("canvas-confetti", () => ({ default: vi.fn() }));

const promo = (cambios) => ({
  nombre: "Miércoles 2x1", tipo: "NXM", beneficio: "2x1",
  vigenciaDesde: "2026-08-01", vigenciaHasta: "2026-12-31", diasSemana: ["WEDNESDAY"], mediosPago: [],
  tarifasExcluidas: ["MENOR", "JUBILADO", "ESTUDIANTE"], ...cambios,
});

const combo = {
  id: 3, nombre: "Combo clásico", tipo: "COMBO", precio: 5500, precioSuelto: 6500, ahorro: 1000,
  disponible: true, esCombo: true,
  componentes: [{ productoId: 1, nombre: "Pochoclos grandes", cantidad: 1 }, { productoId: 2, nombre: "Gaseosa 500ml", cantidad: 1 }],
};
const pochoclos = {
  id: 1, nombre: "Pochoclos grandes", tipo: "POCHOCLOS", precio: 4000, precioSuelto: 4000, ahorro: 0,
  disponible: true, esCombo: false, componentes: [],
};

// Miércoles 21/10/2026. Solo la fecha: los findBy siguen con timeouts reales.
beforeEach(() => {
  vi.useFakeTimers({ toFake: ["Date"] });
  vi.setSystemTime(new Date(2026, 9, 21, 12, 0));
  api.sesionActual.mockRejectedValue(new Error("401"));
  api.obtenerPromocionesVigentes.mockResolvedValue([]);
  api.obtenerProductosCandy.mockResolvedValue([]);
});

afterEach(() => vi.useRealTimers());

const abrir = () => renderConApp(<Promociones />);

describe("Promociones", () => {
  it("muestra el beneficio, los medios, la vigencia y las tarifas que describe el backend", async () => {
    api.obtenerPromocionesVigentes.mockResolvedValue([
      promo({ nombre: "Banco", tipo: "PORCENTAJE", beneficio: "30 % OFF", diasSemana: [], mediosPago: ["DEBITO"],
        horaDesde: "14:00", horaHasta: "18:00" }),
    ]);
    abrir();

    expect(await screen.findByText("30 % OFF")).toBeInTheDocument();
    expect(screen.getByText("Débito")).toBeInTheDocument();
    expect(screen.getByText("14:00–18:00")).toBeInTheDocument();
    await userEvent.click(screen.getByText("Ver condiciones ▼"));
    expect(screen.getByText("Funciones de 14:00 a 18:00")).toBeInTheDocument();
    expect(screen.getByText("Del 01/08/2026 al 31/12/2026")).toBeInTheDocument();
    expect(screen.getByText("No suma con las tarifas menor, jubilado, estudiante")).toBeInTheDocument();
  });

  it.each([
    ["el miércoles es su día", ["WEDNESDAY"], "2026-08-01", true],
    ["corre todos los días", [], "2026-08-01", true],
    ["promo de jueves un miércoles", ["THURSDAY"], "2026-08-01", false],
    ["corre jueves y miércoles", ["THURSDAY", "WEDNESDAY"], "2026-08-01", true],
    ["todavía no empezó", ["WEDNESDAY"], "2026-11-01", false],
    ["su franja todavía no terminó", ["WEDNESDAY"], "2026-08-01", true, "13:00", "17:00"],
    ["su franja ya terminó hoy", ["WEDNESDAY"], "2026-08-01", false, "08:00", "11:00"],
  ])("¡Hoy aplica! cuando %s", async (_, diasSemana, vigenciaDesde, aplica, horaDesde, horaHasta) => {
    api.obtenerPromocionesVigentes.mockResolvedValue([promo({ diasSemana, vigenciaDesde, horaDesde, horaHasta })]);
    abrir();

    await screen.findByText("2x1");
    expect(screen.queryByText("¡Hoy aplica!") !== null).toBe(aplica);
  });

  it("tocar ¡Hoy aplica! festeja con confetti", async () => {
    api.obtenerPromocionesVigentes.mockResolvedValue([promo()]);
    abrir();

    await userEvent.click(await screen.findByText("¡Hoy aplica!"));

    await vi.waitFor(() => expect(confetti).toHaveBeenCalledOnce());
  });

  it("muestra los combos con el ahorro que manda el backend y los sueltos", async () => {
    api.obtenerProductosCandy.mockResolvedValue([combo, pochoclos]);
    abrir();

    const tarjeta = (await screen.findByText("Combo clásico")).closest(".mantine-Card-root");
    expect(await within(tarjeta).findByText("$ 1.000")).toBeInTheDocument();
    expect(within(tarjeta).getByText("$ 6.500")).toBeInTheDocument();
    expect(within(tarjeta).getByText("1 × Gaseosa 500ml")).toBeInTheDocument();
    expect(screen.getByText("$ 4.000")).toBeInTheDocument();
  });

  it("sin promos lo dice", async () => {
    abrir();

    expect(await screen.findByText("Por ahora no hay promos")).toBeInTheDocument();
  });

  it("muestra el error del backend tal cual", async () => {
    api.obtenerPromocionesVigentes.mockRejectedValue(new Error("No se pudo conectar con el servidor"));
    abrir();

    expect(await screen.findByText("No se pudo conectar con el servidor")).toBeInTheDocument();
  });

  it("usa la imagen de la promo y del combo cuando la tienen", async () => {
    api.obtenerPromocionesVigentes.mockResolvedValue([promo({ imagenUrl: "/api/imagenes/a.webp" }), promo({ nombre: "Sin foto" })]);
    api.obtenerProductosCandy.mockResolvedValue([{ ...combo, imagenUrl: "/api/imagenes/c.png" }, pochoclos]);
    abrir();

    expect(await screen.findByAltText("Miércoles 2x1")).toHaveAttribute("src", "/api/imagenes/a.webp");
    expect(screen.queryByAltText("Sin foto")).not.toBeInTheDocument();
    expect(await screen.findByAltText("Combo clásico")).toHaveAttribute("loading", "lazy");
    expect(screen.queryByAltText("Pochoclos grandes")).not.toBeInTheDocument();
  });
});
