import { fireEvent, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "@/api/api-http.js";
import { renderConApp } from "@/test/renderConApp.jsx";
import { Cartelera } from "./Cartelera.jsx";

vi.mock("@/api/api-http.js");

const pelicula = (id, titulo, cambios) => ({
  id, titulo, duracionMinutos: 120, generos: ["DRAMA"], clasificacion: "ATP", sinopsis: `La historia de ${titulo}`,
  posterUrl: `https://image.tmdb.org/${id}.jpg`, trailerYoutube: `trailer${id}`, ...cambios,
});

const PELICULAS = [pelicula(1, "Dune"), pelicula(2, "Matrix", { trailerYoutube: null }), pelicula(3, "Alien")];

const matchMediaOriginal = window.matchMedia;

function conMouse() {
  window.matchMedia = (query) => ({ ...matchMediaOriginal(query), matches: query.includes("hover") });
}

beforeEach(() => {
  api.sesionActual.mockRejectedValue(new Error("401"));
  api.obtenerCartelera.mockResolvedValue(PELICULAS);
  api.obtenerGeneros.mockResolvedValue(["DRAMA", "TERROR"]);
});

afterEach(() => { window.matchMedia = matchMediaOriginal; });

const abrir = (en = "/cartelera") => renderConApp(<Cartelera />, { ruta: "/cartelera/:genero?", en });
const abrirHome = () => renderConApp(<Cartelera conHero />);
const tarjeta = (titulo) => screen.getByText(titulo, { selector: "p" }).closest("a");

describe("Cartelera", () => {
  it("lista las películas en cartel con lo justo para decidir y cada una lleva a su ficha", async () => {
    abrir();

    expect(await screen.findByText("3 películas en cartel")).toBeInTheDocument();
    expect(api.obtenerCartelera).toHaveBeenCalledWith(undefined);
    expect(within(tarjeta("Dune")).getByText("2h 0m · Drama")).toBeInTheDocument();
    expect(tarjeta("Matrix")).toHaveAttribute("href", "/pelicula/2");
  });

  it("filtra por el género de la URL y ofrece volver a todos", async () => {
    api.obtenerCartelera.mockResolvedValue([PELICULAS[0]]);

    abrir("/cartelera/DRAMA");

    expect(await screen.findByText("1 película de drama")).toBeInTheDocument();
    expect(api.obtenerCartelera).toHaveBeenCalledWith("DRAMA");
    const filtro = screen.getByLabelText("Filtrar por género");
    expect(within(filtro).getAllByRole("link").map((a) => [a.textContent, a.getAttribute("href")])).toEqual([
      ["Todos", "/cartelera"], ["Drama", "/cartelera/DRAMA"], ["Terror", "/cartelera/TERROR"],
    ]);
  });

  it("sin películas de ese género lo dice y ofrece ver toda la cartelera", async () => {
    api.obtenerCartelera.mockResolvedValue([]);

    abrir("/cartelera/TERROR");

    expect(await screen.findByText("No hay películas de ese género en cartelera.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Ver toda la cartelera" })).toHaveAttribute("href", "/cartelera");
  });

  it("sin ninguna película en cartel lo dice sin ofrecer una cartelera que tampoco tiene nada", async () => {
    api.obtenerCartelera.mockResolvedValue([]);

    abrir();

    expect(await screen.findByText("Todavía no hay películas en cartelera.")).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Ver toda la cartelera" })).not.toBeInTheDocument();
  });

  it("si no puede traerla muestra el error", async () => {
    api.obtenerCartelera.mockRejectedValue(new Error("No se pudo conectar con el servidor"));

    abrir();

    expect(await screen.findByText("No se pudo conectar con el servidor")).toBeInTheDocument();
  });

  it("con mouse, quedarse sobre una tarjeta con trailer muestra la previa y salir la saca", async () => {
    conMouse();
    abrir();
    await screen.findByText("3 películas en cartel");

    await userEvent.hover(tarjeta("Dune"));
    expect(await screen.findByTitle("Previa del trailer")).toHaveAttribute("src", expect.stringContaining("/embed/trailer1?"));

    await userEvent.unhover(tarjeta("Dune"));
    expect(screen.queryByTitle("Previa del trailer")).not.toBeInTheDocument();
  });

  it.each([
    ["sin mouse (táctil)", false, "Dune"],
    ["con mouse pero sin trailer", true, "Matrix"],
  ])("%s no hay previa", async (_caso, mouse, titulo) => {
    if (mouse) conMouse();
    abrir();
    await screen.findByText("3 películas en cartel");

    await userEvent.hover(tarjeta(titulo));
    await new Promise((r) => setTimeout(r, 700));

    expect(screen.queryByTitle("Previa del trailer")).not.toBeInTheDocument();
  });
});

describe("Cartelera en la home: la portada", () => {
  it("destaca la primera con su sinopsis y lleva a comprar", async () => {
    abrirHome();

    expect(await screen.findByRole("heading", { level: 1, name: "Dune" })).toBeInTheDocument();
    expect(screen.getByText("La historia de Dune")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Comprar entradas" })).toHaveAttribute("href", "/pelicula/1");
  });

  it("elegir una miniatura la destaca; sin trailer no ofrece verlo", async () => {
    abrirHome();
    await screen.findByRole("heading", { level: 1, name: "Dune" });

    await userEvent.click(screen.getByRole("button", { name: "Destacar Matrix" }));

    expect(await screen.findByRole("heading", { level: 1, name: "Matrix" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Destacar Matrix" })).toHaveAttribute("aria-pressed", "true");
    expect(screen.queryByRole("button", { name: "Ver trailer" })).not.toBeInTheDocument();
  });

  it("al llenarse la barra de la destacada pasa a la siguiente, y después de la última vuelve a la primera", async () => {
    abrirHome();
    await screen.findByRole("heading", { level: 1, name: "Dune" });
    const barra = () => screen.getByRole("button", { pressed: true }).lastElementChild;

    for (const siguiente of ["Matrix", "Alien", "Dune"]) {
      // jsdom no tiene `animation` en el style y React escucha la versión con prefijo.
      fireEvent(barra(), new Event("webkitAnimationEnd", { bubbles: true }));
      expect(await screen.findByRole("heading", { level: 1, name: siguiente })).toBeInTheDocument();
    }
  });

  it("ver el trailer lo abre en un modal", async () => {
    abrirHome();

    await userEvent.click(await screen.findByRole("button", { name: "Ver trailer" }));

    expect(await screen.findByTitle("Trailer de Dune")).toHaveAttribute("src", expect.stringContaining("/embed/trailer1?"));
  });

  it("si el cuadro del trailer no existe en máxima calidad usa el de alta", async () => {
    const { container } = abrirHome();
    await screen.findByRole("heading", { level: 1, name: "Dune" });
    const fondo = container.querySelector('img[src*="i.ytimg.com"]');
    expect(fondo).toHaveAttribute("src", "https://i.ytimg.com/vi/trailer1/maxresdefault.jpg");

    fireEvent.error(fondo);

    expect(container.querySelector('img[src*="i.ytimg.com"]')).toHaveAttribute("src", "https://i.ytimg.com/vi/trailer1/hqdefault.jpg");
  });

  it("con una sola película no muestra miniaturas", async () => {
    api.obtenerCartelera.mockResolvedValue([PELICULAS[0]]);

    abrirHome();

    expect(await screen.findByRole("heading", { level: 1, name: "Dune" })).toBeInTheDocument();
    expect(screen.queryByLabelText("Películas destacadas")).not.toBeInTheDocument();
  });
});
