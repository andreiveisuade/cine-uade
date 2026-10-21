import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route, Routes } from "react-router";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "@/api/api-http.js";
import { renderConApp } from "@/test/renderConApp.jsx";
import { ErrorDePantalla } from "./ErrorDePantalla.jsx";

vi.mock("@/api/api-http.js");

function Rota() {
  throw new Error("se rompió el render");
}

// Como en AppCliente: el aviso envuelve las rutas, así que volver al inicio tiene que dejar ver la de destino.
const PANTALLAS = (
  <Routes>
    <Route index element={<p>inicio</p>} />
    <Route path="funcion/:id" element={<Rota />} />
  </Routes>
);

beforeEach(() => {
  api.sesionActual.mockResolvedValue(null);
  vi.spyOn(console, "error").mockImplementation(() => {});
});

describe("ErrorDePantalla", () => {
  it("si una pantalla tira, muestra el aviso en vez de la página en blanco", () => {
    renderConApp(<ErrorDePantalla><Rota /></ErrorDePantalla>, { ruta: "/funcion/1", en: "/funcion/1" });

    expect(screen.getByText("Algo salió mal")).toBeInTheDocument();
  });

  it("Volver al inicio limpia el error y deja ver la pantalla de destino", async () => {
    // Al limpiar el error la pantalla rota se vuelve a intentar una vez antes de que llegue la navegación; React
    // se recupera solo y lo reporta como recuperable, que acá no es un fallo.
    const recuperable = vi.fn();
    renderConApp(<ErrorDePantalla>{PANTALLAS}</ErrorDePantalla>,
      { ruta: "/*", en: "/funcion/1", onRecoverableError: recuperable });

    await userEvent.click(screen.getByRole("link", { name: "Volver al inicio" }));

    expect(screen.queryByText("Algo salió mal")).not.toBeInTheDocument();
    expect(screen.getByText("inicio")).toBeInTheDocument();
  });
});
