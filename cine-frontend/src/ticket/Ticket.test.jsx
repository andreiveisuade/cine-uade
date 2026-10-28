import { act, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import confetti from "canvas-confetti";
import * as api from "@/api/api-http.js";
import { renderConApp } from "@/test/renderConApp.jsx";
import { Ticket } from "./Ticket.jsx";

vi.mock("@/api/api-http.js");
vi.mock("canvas-confetti", () => ({ default: Object.assign(vi.fn(), { reset: vi.fn() }) }));
// Un documento que acepta cualquier dibujo y anota lo que se escribe y con qué nombre se guarda.
const pdf = vi.hoisted(() => ({ text: vi.fn(), save: vi.fn(), getTextWidth: () => 10, splitTextToSize: (t) => [t] }));
vi.mock("jspdf", () => ({
  jsPDF: class {
    constructor() {
      return new Proxy({}, { get: (_, metodo) => pdf[metodo] ?? (() => {}) });
    }
  },
}));

const reserva = (estado, cobrable = false) => ({
  id: 1, codigo: "ABCD1234", estado, cobrable, total: 2000, venceEn: "2026-10-21T19:30:00",
  pelicula: { titulo: "Matrix" }, sala: { nombre: "Sala 1", tipo: "DOS_D" },
  funcion: { inicio: "2026-10-21T20:00:00", proyeccion: "DOS_D", idioma: "SUBTITULADA" },
  cliente: { nombre: "Ana", email: "ana@mail.com" },
  entradas: [{ codigo: "A1", precio: 1000, tarifa: "GENERAL" }, { codigo: "A2", precio: 1000, tarifa: "GENERAL" }],
});

const abrir = () => renderConApp(<Ticket />, { ruta: "/ticket/:codigo", en: "/ticket/ABCD1234" });

// Solo el intervalo es falso: los findBy de Testing Library siguen esperando con timeouts reales.
const pasar = (ms) => act(() => vi.advanceTimersByTimeAsync(ms));

beforeEach(() => {
  vi.useFakeTimers({ toFake: ["setInterval", "clearInterval"] });
  api.sesionActual.mockRejectedValue(new Error("401"));
  api.obtenerProductosCandy.mockResolvedValue([]);
  api.obtenerPromocionesVigentes.mockResolvedValue([]);
});

afterEach(() => vi.useRealTimers());

describe("Ticket", () => {
  it.each([
    ["RESERVADA", "Reserva confirmada. Pagala online o en boletería antes de las 19:30: después se liberan las butacas."],
    ["PAGADA", "Entrada paga. Mostrá el código en la puerta."],
    ["EXPIRADA", "Esta reserva venció. Las butacas ya se liberaron."],
    ["CANCELADA", "Esta reserva está cancelada."],
  ])("con estado %s le dice al comprador lo que corresponde", async (estado, texto) => {
    api.obtenerReservaPorCodigo.mockResolvedValue(reserva(estado));

    abrir();

    expect(await screen.findByText(texto)).toBeInTheDocument();
  });

  it("al recargar no muestra la reserva vieja que quedó en el historial: la pide al backend", async () => {
    api.obtenerReservaPorCodigo.mockResolvedValue(reserva("PAGADA"));

    renderConApp(<Ticket />, { ruta: "/ticket/:codigo",
      en: { pathname: "/ticket/ABCD1234", state: { reserva: reserva("RESERVADA", true) } } });

    expect(await screen.findByText("Entrada paga. Mostrá el código en la puerta.")).toBeInTheDocument();
  });

  it("mientras espera el pago vuelve a pedir cada 10 s y deja de pedir al pasar a PAGADA", async () => {
    api.obtenerReservaPorCodigo
      .mockResolvedValueOnce(reserva("RESERVADA", true))
      .mockResolvedValueOnce(reserva("RESERVADA", true))
      .mockResolvedValue(reserva("PAGADA"));
    abrir();
    await screen.findByText(/Reserva confirmada/);

    await pasar(10_000);
    expect(api.obtenerReservaPorCodigo).toHaveBeenCalledTimes(2);
    await pasar(10_000);
    expect(await screen.findByText("Entrada paga. Mostrá el código en la puerta.")).toBeInTheDocument();

    await pasar(60_000);
    expect(api.obtenerReservaPorCodigo).toHaveBeenCalledTimes(3);
  });

  it("deja de pedir al salir de la pantalla", async () => {
    api.obtenerReservaPorCodigo.mockResolvedValue(reserva("RESERVADA", true));
    const { unmount } = abrir();
    await screen.findByText(/Reserva confirmada/);

    unmount();
    await pasar(60_000);

    expect(api.obtenerReservaPorCodigo).toHaveBeenCalledOnce();
  });

  it("una reserva que no se puede cobrar online no se vuelve a pedir", async () => {
    api.obtenerReservaPorCodigo.mockResolvedValue(reserva("RESERVADA", false));
    abrir();
    await screen.findByText(/Reserva confirmada/);

    await pasar(60_000);

    expect(api.obtenerReservaPorCodigo).toHaveBeenCalledOnce();
  });

  it("tira confetti una sola vez cuando el pago llega con la pantalla abierta", async () => {
    api.obtenerReservaPorCodigo
      .mockResolvedValueOnce(reserva("RESERVADA", true))
      .mockResolvedValue(reserva("PAGADA"));
    abrir();
    await screen.findByText(/Reserva confirmada/);

    await pasar(10_000);
    await screen.findByText(/Entrada paga/);
    await vi.waitFor(() => expect(confetti).toHaveBeenCalledOnce());
    await pasar(60_000);

    expect(confetti).toHaveBeenCalledOnce();
  });

  it("no tira confetti si el ticket se abre ya pagado", async () => {
    api.obtenerReservaPorCodigo.mockResolvedValue(reserva("PAGADA"));
    abrir();
    await screen.findByText(/Entrada paga/);

    await pasar(30_000);

    expect(confetti).not.toHaveBeenCalled();
  });
});

describe("Ticket como entrada", () => {
  it.each([
    ["RESERVADA", false],
    ["PAGADA", true],
    ["EXPIRADA", false],
    ["CANCELADA", false],
  ])("con estado %s el QR del código aparece: %s", async (estado, conQr) => {
    api.obtenerReservaPorCodigo.mockResolvedValue(reserva(estado));
    abrir();

    expect(await screen.findByText("ABCD 1234")).toBeInTheDocument();
    expect(screen.queryByTitle("QR del código ABCD1234") !== null).toBe(conQr);
  });

  it("la nota del carnet la decide la entrada que manda el backend, no el nombre de la tarifa", async () => {
    api.obtenerReservaPorCodigo.mockResolvedValue({
      ...reserva("RESERVADA"),
      entradas: [
        { codigo: "A1", precio: 1000, tarifa: "GENERAL", requiereAcreditacion: false },
        { codigo: "A2", precio: 700, tarifa: "ESTUDIANTE", requiereAcreditacion: true },
        { codigo: "A3", precio: 600, tarifa: "MENOR", requiereAcreditacion: false },
      ],
    });
    abrir();

    expect(await screen.findByText("Estudiante")).toBeInTheDocument();
    expect(screen.getAllByText(/traé el carnet/)).toHaveLength(1);
  });

  it("el comprobante del backend no se muestra suelto: se despliega", async () => {
    api.obtenerReservaPorCodigo.mockResolvedValue(reserva("RESERVADA"));
    abrir();

    expect(await screen.findByText("Ver el comprobante en texto")).toBeInTheDocument();
    expect(screen.getByText("Ver el comprobante en texto").closest("details")).not.toHaveAttribute("open");
    expect(screen.queryByRole("button", { name: "Copiar comprobante" })).not.toBeInTheDocument();
  });

  it("el código se copia desde el talón, al lado del código legible", async () => {
    const usuario = userEvent.setup();
    api.obtenerReservaPorCodigo.mockResolvedValue(reserva("PAGADA"));
    abrir();

    await usuario.click(await screen.findByRole("button", { name: "Copiar código" }));

    expect(await navigator.clipboard.readText()).toBe("ABCD1234");
    expect(await screen.findByText("Código copiado")).toBeInTheDocument();
  });
});

describe("descargar la entrada en PDF", () => {
  it.each([
    ["RESERVADA", false],
    ["PAGADA", true],
    ["EXPIRADA", false],
    ["CANCELADA", false],
  ])("con estado %s el botón aparece: %s", async (estado, aparece) => {
    api.obtenerReservaPorCodigo.mockResolvedValue(reserva(estado));
    abrir();
    await screen.findByText("ABCD 1234");

    expect(screen.queryByRole("button", { name: "Descargar entrada" }) !== null).toBe(aparece);
  });

  it("una entrada ya usada en la puerta no se descarga", async () => {
    api.obtenerReservaPorCodigo.mockResolvedValue({ ...reserva("PAGADA"), ingresadaEn: "2026-10-21T19:50:00" });
    abrir();
    await screen.findByText("ABCD 1234");

    expect(screen.queryByRole("button", { name: "Descargar entrada" })).not.toBeInTheDocument();
  });

  it("al tocarlo guarda el PDF con el código en el nombre y en la entrada", async () => {
    api.obtenerReservaPorCodigo.mockResolvedValue(reserva("PAGADA"));
    abrir();

    await userEvent.click(await screen.findByRole("button", { name: "Descargar entrada" }));

    await vi.waitFor(() => expect(pdf.save).toHaveBeenCalledWith("entrada-cine-uade-ABCD1234.pdf"));
    expect(pdf.text).toHaveBeenCalledWith("ABCD 1234", expect.any(Number), expect.any(Number), expect.any(Object));
  });
});

describe("venta cruzada en el ticket", () => {
  const combo = { id: 9, nombre: "Combo clásico", esCombo: true, precio: 5500, precioSuelto: 6500, ahorro: 1000,
    componentes: [{ productoId: 1, cantidad: 1, nombre: "Pochoclos grandes" }], imagenUrl: "/api/imagenes/c.webp" };
  const pochoclos = { id: 1, nombre: "Pochoclos grandes", tipo: "POCHOCLOS", esCombo: false, precio: 4000, imagenUrl: "/api/imagenes/p.webp" };
  const sinFoto = { id: 2, nombre: "Agua", tipo: "BEBIDA", esCombo: false, precio: 1800 };
  const promo = { nombre: "Miércoles 2x1", beneficio: "2x1", diasSemana: [], mediosPago: [], tarifasExcluidas: [],
    vigenciaDesde: "2026-01-01", vigenciaHasta: "2026-12-31", imagenUrl: "/api/imagenes/a.webp" };

  beforeEach(() => {
    api.obtenerProductosCandy.mockResolvedValue([pochoclos, sinFoto, combo]);
    api.obtenerPromocionesVigentes.mockResolvedValue([promo]);
  });

  it("muestra combos primero, después los productos con foto y al final las promos", async () => {
    api.obtenerReservaPorCodigo.mockResolvedValue(reserva("RESERVADA", true));
    abrir();

    const seccion = await screen.findByRole("region", { name: "Para sumar a la función" });
    // Texto exacto: el combo también nombra a los pochoclos en su detalle.
    const nombres = within(seccion).getAllByText(/^(Combo clásico|Pochoclos grandes|2x1)$/).map((n) => n.textContent);
    expect(nombres).toEqual(["Combo clásico", "Pochoclos grandes", "2x1"]);
    expect(seccion).not.toHaveTextContent("Agua");
  });

  it.each([
    ["sin pagar invita a sumarlo a la compra", reserva("RESERVADA", true), "Sumalo a tu compra y pagalo con la entrada."],
    ["paga, a sumarlo y retirarlo con el QR", reserva("PAGADA"), "Sumalo ahora y retiralo con el QR de tu entrada."],
  ])("%s", async (_caso, laReserva, llamado) => {
    api.obtenerReservaPorCodigo.mockResolvedValue(laReserva);
    abrir();

    expect(await screen.findByText(llamado)).toBeInTheDocument();
  });

  it("paga, el llamado lleva a sumar candy a la entrada", async () => {
    api.obtenerReservaPorCodigo.mockResolvedValue(reserva("PAGADA"));
    abrir();

    expect(await screen.findByRole("link", { name: "Sumar candy" })).toHaveAttribute("href", "/compra/ABCD1234");
  });

  // El candy pedido no se entrega hasta cobrarse, y lo cobrado se retira una vez con el QR.
  it("cada renglón del candy dice si falta pagarlo, si se retira o si ya se retiró", async () => {
    api.obtenerReservaPorCodigo.mockResolvedValue({ ...reserva("PAGADA"), candy: [
      { id: 1, estado: "COBRADA", retiradaEn: "2026-10-21T19:55:00", items: [{ productoId: 4, nombre: "Combo clásico", cantidad: 1, subtotal: 5500 }] },
      { id: 2, estado: "COBRADA", items: [{ productoId: 1, nombre: "Pochoclos", cantidad: 2, subtotal: 7000 }] },
      { id: 3, estado: "PENDIENTE", items: [{ productoId: 2, nombre: "Gaseosa", cantidad: 1, subtotal: 2200 }] },
    ] });
    abrir();

    expect(await screen.findByText("1 × Combo clásico")).toBeInTheDocument();
    expect(screen.getByText("retirado")).toBeInTheDocument();
    expect(screen.getByText("a retirar")).toBeInTheDocument();
    expect(screen.getByText("sin pagar")).toBeInTheDocument();
  });

  it("una reserva vencida no ofrece nada para sumar", async () => {
    api.obtenerReservaPorCodigo.mockResolvedValue(reserva("EXPIRADA"));
    abrir();
    await screen.findByText("Esta reserva venció. Las butacas ya se liberaron.");

    expect(screen.queryByLabelText("Para sumar a la función")).not.toBeInTheDocument();
  });
});
