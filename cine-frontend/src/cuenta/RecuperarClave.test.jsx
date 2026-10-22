import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "@/api/api-http.js";
import { renderConApp } from "@/test/renderConApp.jsx";
import { RecuperarClave } from "./RecuperarClave.jsx";

vi.mock("@/api/api-http.js");

async function completar() {
  await userEvent.type(screen.getByLabelText(/^Email/), " ana@mail.com ");
  await userEvent.type(screen.getByLabelText(/^Código de una de tus entradas/), " k7m2qx9p ");
  await userEvent.type(screen.getByLabelText(/^Contraseña nueva/), "nueva");
  await userEvent.click(screen.getByRole("button", { name: "Cambiar contraseña" }));
}

beforeEach(() => {
  api.sesionActual.mockRejectedValue(new Error("401"));
});

describe("RecuperarClave", () => {
  it("manda los tres campos y lleva a ingresar", async () => {
    api.recuperarClave.mockResolvedValue(null);
    renderConApp(<RecuperarClave />);
    await completar();

    expect(api.recuperarClave).toHaveBeenCalledWith({ email: "ana@mail.com", codigoReserva: "k7m2qx9p", nueva: "nueva" });
    expect(await screen.findByText("otra pantalla")).toBeInTheDocument();
  });

  it("muestra el error del backend tal cual", async () => {
    api.recuperarClave.mockRejectedValue(new Error("Demasiados intentos, probá más tarde"));
    renderConApp(<RecuperarClave />);
    await completar();

    expect(await screen.findByText("Demasiados intentos, probá más tarde")).toBeInTheDocument();
  });

  it("lleva el foco al primer campo que falta", async () => {
    renderConApp(<RecuperarClave />);
    await userEvent.type(screen.getByLabelText(/^Email/), "ana@mail.com");
    await userEvent.type(screen.getByLabelText(/^Código de una de tus entradas/), "   ");
    await userEvent.type(screen.getByLabelText(/^Contraseña nueva/), "   ");
    await userEvent.click(screen.getByRole("button", { name: "Cambiar contraseña" }));

    expect(screen.getByLabelText(/^Código de una de tus entradas/)).toHaveFocus();
    expect(api.recuperarClave).not.toHaveBeenCalled();
  });
});
