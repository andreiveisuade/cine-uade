import { readFileSync } from "node:fs";
import { join } from "node:path";
import { screen } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

// public/ se sirve tal cual: el test lee el mismo archivo que pide Google, sin React de por medio.
const PAGINA = readFileSync(join(import.meta.dirname, "../../public/privacidad.html"), "utf8");

beforeEach(() => {
  document.body.innerHTML = new DOMParser().parseFromString(PAGINA, "text/html").body.innerHTML;
});

describe("política de privacidad", () => {
  it("dice qué es, cuándo se actualizó y responde cada pregunta en su sección", () => {
    expect(screen.getByRole("heading", { level: 1, name: "Política de privacidad" })).toBeInTheDocument();
    expect(screen.getByText(/Última actualización: \d+ de \w+ de \d{4}/)).toBeInTheDocument();
    expect(screen.getAllByRole("heading", { level: 2 }).map((h) => h.textContent)).toEqual([
      "Quiénes somos", "Qué datos guardamos", "Para qué los usamos", "Qué no hacemos",
      "Cookies y lo que queda en tu navegador", "Cómo darte de baja", "Contacto",
    ]);
    expect(screen.getByText(/proyecto académico, sin fines comerciales/)).toBeInTheDocument();
  });

  it("la baja lleva a Mi cuenta y la marca, al inicio del sitio", () => {
    expect(screen.getByRole("link", { name: "Mi cuenta" })).toHaveAttribute("href", "/#/mi-cuenta");
    expect(screen.getByRole("link", { name: "Cine UADE, inicio" })).toHaveAttribute("href", "/");
  });

  it("el repo es público: ningún email de contacto", () => {
    expect(document.body.textContent).not.toContain("@");
    expect(document.querySelector('a[href^="mailto:"]')).toBeNull();
  });
});
