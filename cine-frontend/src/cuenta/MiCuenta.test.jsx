import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "@/api/api-http.js";
import { renderConApp } from "@/test/renderConApp.jsx";
import { MiCuenta } from "./MiCuenta.jsx";

vi.mock("@/api/api-http.js");

const ANA = { id: 1, nombre: "Ana", email: "ana@mail.com", rol: "CLIENTE", debeCambiarClave: false };

beforeEach(() => {
  api.sesionActual.mockResolvedValue(ANA);
});

describe("MiCuenta", () => {
  it("sin sesión manda a ingresar", async () => {
    api.sesionActual.mockRejectedValue(new Error("401"));
    renderConApp(<MiCuenta />);
    expect(await screen.findByText("otra pantalla")).toBeInTheDocument();
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

  it("el cambio de clave muestra el error del backend tal cual", async () => {
    api.cambiarClave.mockRejectedValue(new Error("La contraseña actual no es correcta"));
    renderConApp(<MiCuenta />);
    await userEvent.type(await screen.findByLabelText(/^Contraseña actual/), "mala");
    await userEvent.type(screen.getByLabelText(/^Contraseña nueva/), "otra");

    await userEvent.click(screen.getByRole("button", { name: "Cambiar contraseña" }));

    expect(await screen.findByText("La contraseña actual no es correcta")).toBeInTheDocument();
    expect(api.cambiarClave).toHaveBeenCalledWith({ actual: "mala", nueva: "otra" });
  });

  it("con clave temporal avisa y, al cambiarla, refresca la sesión y el aviso se va", async () => {
    api.sesionActual.mockResolvedValueOnce({ ...ANA, debeCambiarClave: true }).mockResolvedValue(ANA);
    api.cambiarClave.mockResolvedValue(null);
    renderConApp(<MiCuenta />);
    expect(await screen.findByText(/Entraste con una clave temporal/)).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText(/^Contraseña actual/), "temporal");
    await userEvent.type(screen.getByLabelText(/^Contraseña nueva/), "nueva");

    await userEvent.click(screen.getByRole("button", { name: "Cambiar contraseña" }));

    await screen.findByText("Cambiaste tu contraseña");
    expect(screen.queryByText(/Entraste con una clave temporal/)).not.toBeInTheDocument();
  });

  it("la baja pide confirmación antes de llamar al backend", async () => {
    api.darDeBaja.mockResolvedValue(null);
    renderConApp(<MiCuenta />);
    const seccion = (await screen.findByRole("heading", { name: "Dar de baja la cuenta" })).closest("form");
    await userEvent.type(within(seccion).getByLabelText(/^Contraseña/), "secreta");

    await userEvent.click(within(seccion).getByRole("button", { name: "Dar de baja" }));
    expect(api.darDeBaja).not.toHaveBeenCalled();
    await userEvent.click(await screen.findByRole("button", { name: "Sí, dar de baja" }));

    expect(api.darDeBaja).toHaveBeenCalledWith({ actual: "secreta" });
    expect(await screen.findByText("otra pantalla")).toBeInTheDocument();
  });
});
