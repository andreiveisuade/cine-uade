import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "@/api/api-http.js";
import { renderConApp } from "@/test/renderConApp.jsx";
import { Pelicula } from "./Pelicula.jsx";

vi.mock("@/api/api-http.js");

const pelicula = (cambios) => ({
  id: 7, titulo: "Dune", duracionMinutos: 155, generos: ["CIENCIA_FICCION"], clasificacion: "ATP",
  posterUrl: "https://image.tmdb.org/p.jpg", trailerYoutube: "vKQi3bBA1y8", ...cambios,
});

// Una función a las `hh:mm` de dentro de `dias` días, en hora local como la manda el backend.
const funcion = (id, dias, hh) => {
  const d = new Date();
  d.setDate(d.getDate() + dias);
  const fecha = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  return { id, inicio: `${fecha}T${hh}:00`, sala: { nombre: `Sala ${id}`, tipo: "DOS_D" }, proyeccion: "DOS_D", idioma: "SUBTITULADA", precioDesde: 5000 };
};

const matchMediaOriginal = window.matchMedia;

function conMovimientoReducido() {
  window.matchMedia = (query) => ({ ...matchMediaOriginal(query), matches: query.includes("prefers-reduced-motion") });
}

beforeEach(() => {
  api.sesionActual.mockRejectedValue(new Error("401"));
  api.obtenerFuncionesDePelicula.mockResolvedValue([]);
});

afterEach(() => { window.matchMedia = matchMediaOriginal; });

const abrir = () => renderConApp(<Pelicula />, { ruta: "/pelicula/:id", en: "/pelicula/7" });

describe("Pelicula", () => {
  it("arranca el trailer solo y muteado", async () => {
    api.obtenerPelicula.mockResolvedValue(pelicula());
    abrir();

    const video = await screen.findByTitle("Trailer de Dune");
    const url = new URL(video.src);
    expect(url.origin + url.pathname).toBe("https://www.youtube-nocookie.com/embed/vKQi3bBA1y8");
    expect(url.searchParams.get("autoplay")).toBe("1");
    expect(url.searchParams.get("mute")).toBe("1");
    expect(url.searchParams.get("enablejsapi")).toBe("1");
  });

  it("el botón de sonido le pide al video que se desmutee", async () => {
    api.obtenerPelicula.mockResolvedValue(pelicula());
    abrir();
    const video = await screen.findByTitle("Trailer de Dune");
    const postMessage = vi.spyOn(video.contentWindow, "postMessage");

    await userEvent.click(screen.getByRole("button", { name: "Activar sonido" }));

    expect(postMessage).toHaveBeenCalledWith(JSON.stringify({ event: "command", func: "unMute", args: [] }), "https://www.youtube-nocookie.com");
    expect(screen.queryByRole("button", { name: "Activar sonido" })).not.toBeInTheDocument();
  });

  it("sin trailer muestra el póster de fondo", async () => {
    api.obtenerPelicula.mockResolvedValue(pelicula({ trailerYoutube: null }));
    abrir();

    expect(await screen.findByTestId("fondo-poster")).toHaveAttribute("src", "https://image.tmdb.org/p.jpg");
    expect(screen.queryByTitle("Trailer de Dune")).not.toBeInTheDocument();
  });

  it("con movimiento reducido no arranca solo: miniatura y play", async () => {
    conMovimientoReducido();
    api.obtenerPelicula.mockResolvedValue(pelicula());
    abrir();

    const play = await screen.findByRole("button", { name: "Reproducir el trailer de Dune" });
    expect(screen.queryByTitle("Trailer de Dune")).not.toBeInTheDocument();
    await userEvent.click(play);
    expect(screen.getByTitle("Trailer de Dune")).toBeInTheDocument();
  });

  it("muestra el puntaje con sus votos al lado del año y la duración", async () => {
    api.obtenerPelicula.mockResolvedValue(pelicula({ anio: 2024, puntaje: 7.3, votos: 1088 }));
    abrir();

    const puntaje = await screen.findByRole("img", { name: "Puntaje 7,3 de 10, 1.088 votos" });
    expect(puntaje).toHaveTextContent("★ 7,3 · 1.088 votos");
    expect(puntaje.parentElement).toHaveTextContent("2024 · 2h 35m · ★ 7,3 · 1.088 votos");
  });

  it("sin votos o sin puntaje no muestra el puntaje", async () => {
    api.obtenerPelicula.mockResolvedValue(pelicula({ puntaje: 7.3, votos: 0 }));
    const { unmount } = abrir();
    await screen.findByRole("heading", { name: "Dune" });
    expect(screen.queryByRole("img", { name: /Puntaje/ })).not.toBeInTheDocument();
    expect(screen.queryByText(/★/)).not.toBeInTheDocument();
    unmount();

    api.obtenerPelicula.mockResolvedValue(pelicula({ puntaje: 0, votos: 12 }));
    abrir();
    await screen.findByRole("heading", { name: "Dune" });
    expect(screen.queryByText(/★/)).not.toBeInTheDocument();
  });

  it("abre el primer día, pliega el resto y cada encabezado cuenta sus funciones", async () => {
    api.obtenerPelicula.mockResolvedValue(pelicula());
    api.obtenerFuncionesDePelicula.mockResolvedValue([funcion(1, 0, "18:00"), funcion(2, 0, "21:30"), funcion(3, 1, "20:00")]);
    abrir();

    const hoy = await screen.findByRole("button", { name: /Hoy/ });
    const manana = screen.getByRole("button", { name: /Mañana/ });
    expect(hoy).toHaveTextContent("2 funciones");
    expect(manana).toHaveTextContent("1 función");
    expect(hoy).toHaveAttribute("aria-expanded", "true");
    expect(manana).toHaveAttribute("aria-expanded", "false");
    expect(screen.getByRole("link", { name: /18:00/ })).toBeVisible();
    expect(screen.queryByRole("link", { name: /20:00/ })).not.toBeInTheDocument();

    await userEvent.click(manana);
    expect(manana).toHaveAttribute("aria-expanded", "true");
    expect(await screen.findByRole("link", { name: /20:00/ })).toBeVisible();
    expect(hoy).toHaveAttribute("aria-expanded", "true");
  });
});
