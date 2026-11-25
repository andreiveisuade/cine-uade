import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "@/api/api-http.js";
import { renderConApp } from "@/test/renderConApp.jsx";
import { Comentarios } from "./Comentarios.jsx";

vi.mock("@/api/api-http.js");

const DE_LUCIA = { id: 1, autor: "Lucía", puntaje: 4, texto: "Muy buena sala", fecha: "2026-11-22T19:40:00" };
const DE_ANA = { id: 2, autor: "Ana", puntaje: 5, texto: "Me encantó", fecha: "2026-11-25T10:00:00" };

// Mantine elige con Espacio sobre la estrella enfocada; en jsdom no hay coordenadas para el mouse.
async function puntuar(estrellas) {
  (await screen.findByRole("radio", { name: `${estrellas} de 5` })).focus();
  await userEvent.keyboard(" ");
}

beforeEach(() => {
  vi.resetAllMocks();
  api.sesionActual.mockResolvedValue({ id: 1, nombre: "Ana", rol: "CLIENTE" });
  api.obtenerComentarios.mockResolvedValue([DE_LUCIA]);
});

describe("Comentarios", () => {
  it("sin sesión muestra los comentarios y, en vez del formulario, el ingreso", async () => {
    api.sesionActual.mockRejectedValue(new Error("401"));
    renderConApp(<Comentarios />);

    expect(await screen.findByText("Muy buena sala")).toBeInTheDocument();
    expect(await screen.findByRole("link", { name: "Ingresar con Google" })).toHaveAttribute("href", "/ingresar");
    expect(screen.queryByRole("button", { name: "Publicar" })).not.toBeInTheDocument();
  });

  it("resume el promedio y la cantidad", async () => {
    api.obtenerComentarios.mockResolvedValue([DE_ANA, DE_LUCIA]);
    renderConApp(<Comentarios />);

    expect(await screen.findByText("4,5")).toBeInTheDocument();
    expect(screen.getByText("2 comentarios")).toBeInTheDocument();
  });

  it("sin comentarios invita a dejar el primero", async () => {
    api.obtenerComentarios.mockResolvedValue([]);
    renderConApp(<Comentarios />);

    expect(await screen.findByText(/dejá el primero/)).toBeInTheDocument();
  });

  it("publica el puntaje y la reseña, recarga la lista y vacía el formulario", async () => {
    api.publicarComentario.mockResolvedValue(DE_ANA);
    renderConApp(<Comentarios />);
    await screen.findByText("Muy buena sala");
    api.obtenerComentarios.mockResolvedValue([DE_ANA, DE_LUCIA]);

    await puntuar(5);
    await userEvent.type(screen.getByLabelText(/^Reseña/), " Me encantó ");
    await userEvent.click(screen.getByRole("button", { name: "Publicar" }));

    expect(api.publicarComentario).toHaveBeenCalledWith({ puntaje: 5, texto: "Me encantó" });
    expect(await within(screen.getByRole("list")).findByText("Me encantó")).toBeInTheDocument();
    expect(screen.getByLabelText(/^Reseña/)).toHaveValue("");
  });

  it("sin puntaje ni reseña marca los dos campos y no manda nada", async () => {
    renderConApp(<Comentarios />);

    await userEvent.click(await screen.findByRole("button", { name: "Publicar" }));

    expect(screen.getByText("Elegí un puntaje")).toBeInTheDocument();
    expect(screen.getByText("Completá tu reseña")).toBeInTheDocument();
    expect(api.publicarComentario).not.toHaveBeenCalled();
  });

  it("muestra el error del backend y conserva lo escrito", async () => {
    api.publicarComentario.mockRejectedValue(new Error("La reseña no puede tener más de 1000 caracteres"));
    renderConApp(<Comentarios />);

    await puntuar(3);
    await userEvent.type(screen.getByLabelText(/^Reseña/), "Hola");
    await userEvent.click(screen.getByRole("button", { name: "Publicar" }));

    expect(await screen.findByText(/no puede tener más de 1000/)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Reseña/)).toHaveValue("Hola");
  });

  it("el HTML de una reseña se ve como texto", async () => {
    api.obtenerComentarios.mockResolvedValue([{ ...DE_LUCIA, texto: "<img src=x onerror=alert(1)>" }]);
    renderConApp(<Comentarios />);

    expect(await screen.findByText("<img src=x onerror=alert(1)>")).toBeInTheDocument();
  });
});
