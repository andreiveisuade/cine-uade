import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route, Routes } from "react-router";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "@/api/api-http.js";
import { renderConApp } from "@/test/renderConApp.jsx";
import { Ingresar } from "./Ingresar.jsx";

vi.mock("@/api/api-http.js");

const ANA = { nombre: "Ana", rol: "CLIENTE" };

// La compra en curso y Mis reservas, para ver a dónde lleva al volver de Google.
const CON_DESTINOS = (
  <Routes>
    <Route path="/ingresar" element={<Ingresar />} />
    <Route path="/confirmar/5" element={<p>confirmar</p>} />
    <Route path="/mis-reservas" element={<p>mis reservas</p>} />
  </Routes>
);

beforeEach(() => {
  api.sesionActual.mockRejectedValue(new Error("401"));
  api.ingresoConGoogle.mockResolvedValue({ disponible: true });
});

describe("Ingresar", () => {
  it("el botón lleva a Google y recuerda a qué compra volver", async () => {
    renderConApp(<Ingresar />, { en: { pathname: "/", state: { volver: "/confirmar/5" } } });

    await userEvent.click(await screen.findByRole("button", { name: "Ingresar con Google" }));

    expect(api.irAGoogle).toHaveBeenCalledOnce();
    expect(sessionStorage.getItem("cine.vuelta")).toBe("/confirmar/5");
  });

  it("antes de ir a Google, la política de privacidad está a un click", async () => {
    renderConApp(<Ingresar />);

    expect(await screen.findByRole("link", { name: "política de privacidad" })).toHaveAttribute("href", "/privacidad.html");
  });

  it("sin credenciales de Google en el servidor avisa y no deja ir", async () => {
    api.ingresoConGoogle.mockResolvedValue({ disponible: false });
    renderConApp(<Ingresar />);

    expect(await screen.findByText(/El ingreso con Google no está disponible/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Ingresar con Google" })).toBeDisabled();
  });

  it("si Google o el cine rechazaron el ingreso, muestra el motivo tal cual", async () => {
    renderConApp(<Ingresar />,
      { en: "/?error=Ese+email+es+del+personal+del+cine%3A+entr%C3%A1+desde+la+aplicaci%C3%B3n+de+escritorio" });

    expect(await screen.findByText("Ese email es del personal del cine: entrá desde la aplicación de escritorio"))
      .toBeInTheDocument();
  });

  it("al volver de Google con sesión sigue la compra que había dejado", async () => {
    sessionStorage.setItem("cine.vuelta", "/confirmar/5");
    api.sesionActual.mockResolvedValue(ANA);
    renderConApp(CON_DESTINOS, { ruta: "/*", en: "/ingresar" });

    expect(await screen.findByText("confirmar")).toBeInTheDocument();
    expect(sessionStorage.getItem("cine.vuelta")).toBeNull();
  });

  it("al volver de Google sin compra pendiente lleva a Mis reservas", async () => {
    api.sesionActual.mockResolvedValue(ANA);
    renderConApp(CON_DESTINOS, { ruta: "/*", en: "/ingresar" });

    expect(await screen.findByText("mis reservas")).toBeInTheDocument();
  });
});
