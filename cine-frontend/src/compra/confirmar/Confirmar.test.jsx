import { useEffect } from "react";
import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "@/api/api-http.js";
import { useCompra } from "@/compra/CompraEnCurso.jsx";
import { useSesion } from "@/cuenta/SesionDelCliente.jsx";
import { renderConApp } from "@/test/renderConApp.jsx";
import { Confirmar } from "./Confirmar.jsx";

vi.mock("@/api/api-http.js");

const asiento = (codigo) =>
  ({ codigo, fila: 1, numero: Number(codigo.slice(1)), tipo: "ESTANDAR", precio: 1000, precios: { GENERAL: 1000, JUBILADO: 433 } });

const FUNCION = {
  id: 5, peliculaId: 2, pelicula: { titulo: "Matrix" }, inicio: "2026-10-21T20:00:00",
  sala: { nombre: "Sala 1", tipo: "DOS_D" }, proyeccion: "DOS_D", idioma: "SUBTITULADA", precio: 1000,
  asientos: [asiento("A1"), asiento("A2"), asiento("A3")],
};

const TARIFAS = [{ nombre: "GENERAL", requiereAcreditacion: false }, { nombre: "JUBILADO", requiereAcreditacion: true }];

const ANA = { id: 1, nombre: "Ana", email: "ana@mail.com", rol: "CLIENTE" };

// La selección viene del mapa, en memoria: el test la deja puesta como si se hubiera elegido antes de llegar.
// Desde el mapa ya se sabe quién tiene sesión, así que la pantalla se monta recién cuando eso se resolvió.
function ConSeleccion({ butacas, children }) {
  const { seleccion, setSeleccion } = useCompra();
  const { usuario } = useSesion();
  useEffect(() => setSeleccion({ funcionId: 5, butacas }), [butacas, setSeleccion]);
  return seleccion.funcionId && usuario !== undefined ? children : null;
}

const abrir = (butacas = { A1: "GENERAL", A2: "GENERAL" }) =>
  renderConApp(<ConSeleccion butacas={butacas}><Confirmar /></ConSeleccion>, { ruta: "/confirmar/:id", en: "/confirmar/5" });

const campo = (nombre) => screen.findByRole("textbox", { name: nombre });
const confirmar = () => userEvent.click(screen.getByRole("button", { name: "Confirmar reserva" }));

beforeEach(() => {
  api.sesionActual.mockRejectedValue(new Error("401"));
  api.obtenerFuncion.mockResolvedValue(FUNCION);
  api.obtenerTarifas.mockResolvedValue(TARIFAS);
});

describe("Confirmar: el detalle de la compra", () => {
  it("sin butacas elegidas (se recargó la página) vuelve al mapa", async () => {
    renderConApp(<Confirmar />, { ruta: "/confirmar/:id", en: "/confirmar/5" });

    expect(await screen.findByText("otra pantalla")).toBeInTheDocument();
  });

  it("si no puede traer la función muestra el error", async () => {
    api.obtenerFuncion.mockRejectedValue(new Error("No existe la función 5"));

    abrir();

    expect(await screen.findByText("No existe la función 5")).toBeInTheDocument();
  });

  it("lista cada butaca elegida con su precio y el total", async () => {
    abrir();

    const filas = within(await screen.findByRole("table")).getAllByRole("row");
    expect(filas.map((f) => f.textContent)).toEqual(
      ["ButacaTipoTarifaPrecio", expect.stringMatching(/^A1Estándar.*\$ 1\.000$/), expect.stringMatching(/^A2Estándar.*\$ 1\.000$/), "Total$ 2.000"]);
    expect(screen.queryByText("Acordate del carnet")).not.toBeInTheDocument();
  });

  it("cambiar la tarifa de una butaca cambia su precio y el total, y avisa si hay que acreditarla", async () => {
    abrir();
    const [, a1] = within(await screen.findByRole("table")).getAllByRole("row");

    await userEvent.selectOptions(within(a1).getByRole("combobox"), "JUBILADO");

    expect(within(a1).getByText("$ 433")).toBeInTheDocument();
    expect(screen.getByText("$ 1.433")).toBeInTheDocument();
    expect(screen.getByText("Acordate del carnet")).toBeInTheDocument();
    expect(screen.getByText(/A1 \(jubilado\)/)).toBeInTheDocument();
  });
});

describe("Confirmar: los datos de quien reserva", () => {
  it("sin sesión precompleta lo de la compra anterior en este navegador e invita a tener cuenta", async () => {
    localStorage.setItem("cine.cliente", JSON.stringify({ nombre: "Ana", email: "ana@mail.com" }));

    abrir();

    expect(await campo("Nombre")).toHaveValue("Ana");
    expect(await campo("Email")).toHaveValue("ana@mail.com");
    expect(screen.getByText(/Para pagar online hace falta una cuenta/)).toBeInTheDocument();
  });

  it("con sesión precompleta los datos de la cuenta y no pide ingresar", async () => {
    api.sesionActual.mockResolvedValue(ANA);

    abrir();

    expect(await campo("Nombre")).toHaveValue("Ana");
    expect(await campo("Email")).toHaveValue("ana@mail.com");
    expect(screen.queryByText(/Para pagar online hace falta una cuenta/)).not.toBeInTheDocument();
  });

  // Un email de solo espacios ni llega: el navegador lo vacía y el `required` frena el envío.
  it("con el nombre en blanco no reserva y dice qué falta", async () => {
    abrir();
    await userEvent.type(await campo("Nombre"), "   ");
    await userEvent.type(await campo("Email"), "ana@mail.com");

    await confirmar();

    expect(screen.getByText("Completá tu nombre")).toBeInTheDocument();
    expect(api.crearReserva).not.toHaveBeenCalled();
  });
});

describe("Confirmar: reservar", () => {
  async function completarYConfirmar() {
    await userEvent.type(await campo("Nombre"), " Ana ");
    await userEvent.type(await campo("Email"), "ana@mail.com ");
    await confirmar();
  }

  it("reserva con la sesión que bloqueó, recuerda al cliente y lleva al ticket", async () => {
    api.crearReserva.mockResolvedValue({ codigo: "ABCD1234" });
    sessionStorage.setItem("cine.sesionDeCompra", "sesion-del-mapa");
    abrir();

    await completarYConfirmar();

    expect(api.crearReserva).toHaveBeenCalledWith({
      funcionId: 5, nombre: "Ana", email: "ana@mail.com", butacas: { A1: "GENERAL", A2: "GENERAL" }, sesion: "sesion-del-mapa",
    });
    expect(JSON.parse(localStorage.getItem("cine.cliente"))).toEqual({ nombre: "Ana", email: "ana@mail.com" });
    expect(await screen.findByText("otra pantalla")).toBeInTheDocument();
  });

  it("si otra compra ganó una butaca (409) avisa y vuelve al mapa", async () => {
    api.crearReserva.mockRejectedValue(Object.assign(new Error("La butaca A1 ya no está disponible"), { status: 409 }));
    abrir();

    await completarYConfirmar();

    expect(await screen.findByText("La butaca A1 ya no está disponible")).toBeInTheDocument();
    expect(screen.getByText("otra pantalla")).toBeInTheDocument();
  });

  it("otro rechazo del backend se muestra en el formulario y deja reintentar", async () => {
    api.crearReserva.mockRejectedValue(Object.assign(new Error("Ese email es de un empleado"), { status: 400 }));
    abrir();

    await completarYConfirmar();

    expect(await screen.findByText("Ese email es de un empleado")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Confirmar reserva" })).toBeEnabled();
    expect(localStorage.getItem("cine.cliente")).toBeNull();
  });
});
