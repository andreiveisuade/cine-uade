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

    expect(postMessage).toHaveBeenCalledWith(JSON.stringify({ event: "command", func: "unMute", args: [] }), "*");
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
});
