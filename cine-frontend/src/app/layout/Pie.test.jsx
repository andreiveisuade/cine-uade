import { screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "@/api/api-http.js";
import { renderConApp } from "@/test/renderConApp.jsx";
import { Pie } from "./Pie.jsx";

vi.mock("@/api/api-http.js");

beforeEach(() => {
  api.sesionActual.mockRejectedValue(new Error("401"));
  api.obtenerMediosDePago.mockResolvedValue([]);
});

describe("Pie", () => {
  it("lleva a la política de privacidad, que es una página aparte y no una ruta del hash", async () => {
    renderConApp(<Pie />);

    expect(await screen.findByRole("link", { name: "Privacidad" })).toHaveAttribute("href", "/privacidad.html");
  });
});
