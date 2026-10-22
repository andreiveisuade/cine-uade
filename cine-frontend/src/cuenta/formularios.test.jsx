import { fireEvent, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route, Routes } from "react-router";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "@/api/api-http.js";
import { renderConApp } from "@/test/renderConApp.jsx";
import { Ingresar } from "./Ingresar.jsx";
import { Registro } from "./Registro.jsx";

vi.mock("@/api/api-http.js");

const ANA = { nombre: "Ana", rol: "CLIENTE" };

// Los dos formularios se comportan igual; cambian los campos y a qué endpoint va el envío.
const FORMULARIOS = [
  { nombre: "Ingresar", pantalla: <Ingresar />, boton: "Ingresar", pedido: "ingresar",
    campos: { Email: "ana@mail.com", Contraseña: "secreta" } },
  { nombre: "Registro", pantalla: <Registro />, boton: "Registrarme", pedido: "registrarCliente",
    campos: { Nombre: "Ana", Email: "ana@mail.com", Contraseña: "secreta" } },
];

async function completar(campos) {
  for (const [etiqueta, valor] of Object.entries(campos)) {
    await userEvent.type(screen.getByLabelText(etiqueta, { exact: false }), valor);
  }
}

beforeEach(() => {
  api.sesionActual.mockRejectedValue(new Error("401"));
});

describe.each(FORMULARIOS)("$nombre", ({ pantalla, boton, pedido, campos }) => {
  it("con campos vacíos o solo de espacios marca lo que falta y no le pide nada al backend", async () => {
    renderConApp(pantalla);
    await completar(Object.fromEntries(Object.keys(campos).map((c) => [c, "   "])));

    // Se envía el form directo: `required` del navegador frena el vacío, lo que se prueba es la validación propia.
    fireEvent.submit(screen.getByRole("button", { name: boton }).closest("form"));

    expect(screen.getAllByText(/^Completá /)).toHaveLength(Object.keys(campos).length);
    expect(api[pedido]).not.toHaveBeenCalled();
  });

  it("muestra el error del backend tal cual", async () => {
    api[pedido].mockRejectedValue(new Error("Email o contraseña incorrectos"));
    renderConApp(pantalla);
    await completar(campos);

    await userEvent.click(screen.getByRole("button", { name: boton }));

    expect(await screen.findByText("Email o contraseña incorrectos")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: boton })).toBeEnabled();
  });

  it("deja el botón cargando mientras envía", async () => {
    api[pedido].mockReturnValue(new Promise(() => {}));
    renderConApp(pantalla);
    await completar(campos);

    await userEvent.click(screen.getByRole("button", { name: boton }));

    expect(screen.getByRole("button", { name: boton })).toHaveAttribute("data-loading", "true");
    expect(screen.getByRole("button", { name: boton })).toBeDisabled();
  });

  it("al entrar lleva a Mis reservas", async () => {
    api.registrarCliente.mockResolvedValue(null);
    api.ingresar.mockResolvedValue(ANA);
    renderConApp(pantalla);
    await completar(campos);

    await userEvent.click(screen.getByRole("button", { name: boton }));

    expect(await screen.findByText("otra pantalla")).toBeInTheDocument();
    expect(api.ingresar).toHaveBeenCalledWith("ana@mail.com", "secreta");
  });

  it("si vino desde la compra, vuelve a donde estaba", async () => {
    api.registrarCliente.mockResolvedValue(null);
    api.ingresar.mockResolvedValue(ANA);
    renderConApp(
      <Routes>
        <Route path="/" element={pantalla} />
        <Route path="/confirmar/5" element={<p>confirmar</p>} />
      </Routes>, { ruta: "/*", en: { pathname: "/", state: { volver: "/confirmar/5" } } });
    await completar(campos);

    await userEvent.click(screen.getByRole("button", { name: boton }));

    expect(await screen.findByText("confirmar")).toBeInTheDocument();
  });
});

describe("Ingresar", () => {
  it("una cuenta del personal no entra a la web", async () => {
    api.ingresar.mockResolvedValue({ nombre: "Pepe", rol: "ENCARGADO" });
    renderConApp(<Ingresar />);
    await completar(FORMULARIOS[0].campos);

    await userEvent.click(screen.getByRole("button", { name: "Ingresar" }));

    expect(await screen.findByText(/Esa cuenta es del personal del cine/)).toBeInTheDocument();
  });
});

describe("Ingresar con clave temporal", () => {
  it("lleva a Mi cuenta en vez de Mis reservas", async () => {
    api.ingresar.mockResolvedValue({ ...ANA, debeCambiarClave: true });
    renderConApp(
      <Routes>
        <Route path="/" element={<Ingresar />} />
        <Route path="/mi-cuenta" element={<p>mi cuenta</p>} />
      </Routes>, { ruta: "/*" });
    await completar(FORMULARIOS[0].campos);

    await userEvent.click(screen.getByRole("button", { name: "Ingresar" }));

    expect(await screen.findByText("mi cuenta")).toBeInTheDocument();
  });
});

describe("Registro", () => {
  it("si el email ya compró, pide el código de una entrada y lo manda", async () => {
    const pideCodigo = Object.assign(new Error("Para abrir esta cuenta hace falta el código"), { status: 403 });
    api.registrarCliente.mockRejectedValueOnce(pideCodigo).mockResolvedValueOnce(null);
    api.ingresar.mockResolvedValue(ANA);
    renderConApp(<Registro />);
    await completar({ Nombre: "Ana", Email: "ana@mail.com", Contraseña: "secreta" });
    expect(screen.queryByLabelText(/Código de una de tus entradas/)).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Registrarme" }));
    await userEvent.type(await screen.findByLabelText(/Código de una de tus entradas/), " k7m2qx9p ");
    expect(screen.getByText("Está en el ticket que recibiste al comprar")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Registrarme" }));

    expect(await screen.findByText("otra pantalla")).toBeInTheDocument();
    expect(api.registrarCliente).toHaveBeenLastCalledWith(
      { nombre: "Ana", email: "ana@mail.com", password: "secreta", codigoReserva: "k7m2qx9p" });
  });
});
