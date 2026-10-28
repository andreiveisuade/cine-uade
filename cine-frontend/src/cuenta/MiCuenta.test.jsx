import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "@/api/api-http.js";
import { renderConApp } from "@/test/renderConApp.jsx";
import { MiCuenta } from "./MiCuenta.jsx";

vi.mock("@/api/api-http.js");

const ANA = { id: 1, nombre: "Ana", email: "ana@mail.com", rol: "CLIENTE" };

beforeEach(() => {
  api.sesionActual.mockResolvedValue(ANA);
});

describe("MiCuenta", () => {
  it("sin sesión manda a ingresar", async () => {
    api.sesionActual.mockRejectedValue(new Error("401"));
    renderConApp(<MiCuenta />, { ruta: "/mi-cuenta", en: "/mi-cuenta" });
    expect(await screen.findByText("en /ingresar")).toBeInTheDocument();
  });

  it("renombra y deja el email solo de lectura", async () => {
    api.modificarCuenta.mockResolvedValue({ ...ANA, nombre: "Ana María" });
    renderConApp(<MiCuenta />);
    const nombre = await screen.findByLabelText(/^Nombre/);
    expect(screen.getByLabelText(/^Email/)).toHaveAttribute("readonly");

    await userEvent.clear(nombre);
    await userEvent.type(nombre, " Ana María ");
    await userEvent.click(screen.getByRole("button", { name: "Guardar" }));

    expect(api.modificarCuenta).toHaveBeenCalledWith({ nombre: "Ana María" });
    expect(await screen.findByText("Guardamos tu nombre")).toBeInTheDocument();
  });

  it("solo hay nombre y baja: sin clave, la cuenta es la de Google", async () => {
    renderConApp(<MiCuenta />);

    expect(await screen.findByRole("heading", { name: "Tus datos" })).toBeInTheDocument();
    expect(screen.queryByLabelText(/Contraseña/)).not.toBeInTheDocument();
  });

  it("la baja pide confirmación antes de llamar al backend, sin clave", async () => {
    api.darDeBaja.mockResolvedValue(null);
    renderConApp(<MiCuenta />);
    const seccion = (await screen.findByRole("heading", { name: "Dar de baja la cuenta" })).closest("form");

    await userEvent.click(within(seccion).getByRole("button", { name: "Dar de baja" }));
    expect(api.darDeBaja).not.toHaveBeenCalled();
    await userEvent.click(await screen.findByRole("button", { name: "Sí, dar de baja" }));

    expect(api.darDeBaja).toHaveBeenCalledOnce();
    expect(await screen.findByText("otra pantalla")).toBeInTheDocument();
  });
});
