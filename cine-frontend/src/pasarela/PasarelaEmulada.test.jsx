import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "@/api/api-http.js";
import { renderConApp } from "@/test/renderConApp.jsx";
import { PasarelaEmulada } from "./PasarelaEmulada.jsx";

vi.mock("@/api/api-http.js");

const RUTA = { ruta: "/pasarela-emulada/:id", en: "/pasarela-emulada/MP-123?monto=4500.5&medio=DEBITO" };

beforeEach(() => {
  api.sesionActual.mockRejectedValue(new Error("401"));
});

describe("PasarelaEmulada", () => {
  it("dice que es un pago de prueba y muestra el monto y el medio", () => {
    renderConApp(<PasarelaEmulada />, RUTA);

    expect(screen.getByRole("heading", { name: "Pago de prueba" })).toBeInTheDocument();
    expect(screen.getByText(/No se cobra nada/)).toBeInTheDocument();
    expect(screen.getByText("$ 4500.50")).toBeInTheDocument();
    expect(screen.getByText(/con débito/)).toBeInTheDocument();
  });

  it.each([
    ["Aprobar", "approved"],
    ["Rechazar", "rejected"],
  ])("%s vuelve por el retorno con el status de Mercado Pago", async (boton, status) => {
    renderConApp(<PasarelaEmulada />, RUTA);

    await userEvent.click(screen.getByRole("button", { name: boton }));

    expect(api.volverDeLaPasarelaEmulada).toHaveBeenCalledWith("MP-123", status);
    expect(screen.getByRole("button", { name: "Aprobar" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Rechazar" })).toBeDisabled();
  });
});
