import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "./api-http.js";

const respuesta = (status, cuerpo = "") => new Response(status === 204 ? null : cuerpo, { status });
const json = (status, datos) => respuesta(status, JSON.stringify(datos));

let fetch;
beforeEach(() => {
  fetch = vi.fn().mockResolvedValue(json(200, {}));
  vi.stubGlobal("fetch", fetch);
});
afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

const pedido = () => {
  const [url, opciones] = fetch.mock.calls[0];
  return { url, metodo: opciones.method, cabeceras: opciones.headers, cuerpo: opciones.body && JSON.parse(opciones.body) };
};

describe("api-http: el armado de cada pedido", () => {
  it.each([
    ["la cartelera sin género", () => api.obtenerCartelera(), "/api/cartelera"],
    ["la cartelera de un género", () => api.obtenerCartelera("CIENCIA FICCION"), "/api/cartelera?genero=CIENCIA%20FICCION"],
    ["una función sin sesión", () => api.obtenerFuncion(5), "/api/funciones/5"],
    ["una función con la sesión de compra", () => api.obtenerFuncion(5, "a b"), "/api/funciones/5?sesion=a%20b"],
    ["un ticket por código", () => api.obtenerReservaPorCodigo("AB/12"), "/api/reservas/codigo/AB%2F12"],
    ["el candy de la carta", () => api.obtenerProductosCandy(), "/api/candy/productos"],
  ])("GET de %s", async (_caso, llamar, url) => {
    await llamar();

    expect(pedido()).toEqual({ url, metodo: "GET", cabeceras: {}, cuerpo: undefined });
  });

  it.each([
    ["bloquear butacas", () => api.bloquearButacas({ funcionId: "5", sesion: "s", butacas: ["A1"] }),
      "POST", "/api/funciones/5/bloqueos", { sesion: "s", butacas: ["A1"] }],
    ["crear una reserva", () => api.crearReserva({ funcionId: "5", nombre: "Ana", email: "ana@mail.com", butacas: { A1: "GENERAL" }, sesion: "s" }),
      "POST", "/api/reservas", { funcionId: 5, nombre: "Ana", email: "ana@mail.com", butacas: { A1: "GENERAL" }, sesion: "s" }],
    ["modificar la cuenta", () => api.modificarCuenta({ nombre: "Ana" }), "PUT", "/api/cuenta", { nombre: "Ana" }],
    ["dar de baja", () => api.darDeBaja({ actual: "x" }), "DELETE", "/api/cuenta", { actual: "x" }],
    ["abrir el checkout", () => api.abrirCheckout("AB12", "MERCADO_PAGO"), "POST", "/api/reservas/codigo/AB12/checkout", { medio: "MERCADO_PAGO" }],
  ])("%s manda JSON", async (_caso, llamar, metodo, url, cuerpo) => {
    await llamar();

    expect(pedido()).toEqual({ url, metodo, cabeceras: { "Content-Type": "application/json" }, cuerpo });
  });

  it("ingresar manda Basic con la clave en UTF-8, aunque tenga eñes", async () => {
    await api.ingresar("ana@mail.com", "año");

    const { url, metodo, cabeceras } = pedido();
    expect([url, metodo]).toEqual(["/api/sesion", "POST"]);
    const [tipo, credencial] = cabeceras.Authorization.split(" ");
    expect(tipo).toBe("Basic");
    expect(new TextDecoder().decode(Uint8Array.from(atob(credencial), (c) => c.charCodeAt(0)))).toBe("ana@mail.com:año");
  });

  it("la vuelta del pago no sigue el redirect y no falla aunque falle la red", async () => {
    fetch.mockRejectedValue(new TypeError("Failed to fetch"));

    await expect(api.avisarVueltaDelPago("ck 1")).resolves.toBeUndefined();
    expect(fetch).toHaveBeenCalledWith("/api/checkouts/ck%201/retorno", { redirect: "manual" });
  });
});

describe("api-http: las respuestas", () => {
  it("devuelve el JSON de un 2xx y null de un 204", async () => {
    fetch.mockResolvedValueOnce(json(200, [{ id: 1 }])).mockResolvedValueOnce(respuesta(204));

    expect(await api.obtenerGeneros()).toEqual([{ id: 1 }]);
    expect(await api.salir()).toBeNull();
  });

  it("un 2xx con cuerpo vacío devuelve null", async () => {
    fetch.mockResolvedValue(respuesta(200));

    expect(await api.obtenerGeneros()).toBeNull();
  });

  it("un 2xx que no es JSON es un error: no hay con qué seguir", async () => {
    fetch.mockResolvedValue(respuesta(200, "<html>"));

    await expect(api.obtenerGeneros()).rejects.toThrow("El servidor devolvió una respuesta que no se pudo leer");
  });

  it("sin conexión dice que no se pudo conectar", async () => {
    fetch.mockRejectedValue(new TypeError("Failed to fetch"));

    await expect(api.obtenerGeneros()).rejects.toThrow("No se pudo conectar con el servidor");
  });

  it("el {error} del backend llega tal cual, con el código para que la pantalla decida", async () => {
    fetch.mockResolvedValue(json(409, { error: "La butaca A1 ya no está disponible" }));

    await expect(api.crearReserva({ funcionId: 5 })).rejects.toMatchObject({
      message: "La butaca A1 ya no está disponible", status: 409,
    });
  });

  it.each([
    [502, "El servidor no está disponible: probá de nuevo en un momento"],
    [504, "El servidor no está disponible: probá de nuevo en un momento"],
    [500, "Algo salió mal en el servidor: probá de nuevo en un momento"],
    [404, "El servidor respondió con un error (código 404)"],
  ])("un %i sin {error} muestra un mensaje según el código y deja el cuerpo en la consola", async (status, mensaje) => {
    const consola = vi.spyOn(console, "error").mockImplementation(() => {});
    fetch.mockResolvedValue(respuesta(status, "Bad Gateway"));

    await expect(api.obtenerGeneros()).rejects.toMatchObject({ message: mensaje, status });
    expect(consola).toHaveBeenCalledWith(`Respuesta ${status} sin mensaje de error en JSON:`, "Bad Gateway");
  });

  it("un error sin cuerpo no ensucia la consola", async () => {
    const consola = vi.spyOn(console, "error").mockImplementation(() => {});
    fetch.mockResolvedValue(respuesta(401));

    await expect(api.sesionActual()).rejects.toMatchObject({ message: "El servidor respondió con un error (código 401)", status: 401 });
    expect(consola).not.toHaveBeenCalled();
  });
});
