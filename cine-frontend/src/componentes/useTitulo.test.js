import { renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { useTitulo } from "./useTitulo.js";

describe("useTitulo", () => {
  it("pone el texto con el nombre del cine y lo restaura al desmontar", () => {
    document.title = "Cine UADE";
    const { unmount } = renderHook(() => useTitulo("Matrix"));

    expect(document.title).toBe("Matrix · Cine UADE");
    unmount();
    expect(document.title).toBe("Cine UADE");
  });

  it("sin texto deja el título como está (la película todavía está cargando)", () => {
    document.title = "Cine UADE";
    renderHook(() => useTitulo(undefined));

    expect(document.title).toBe("Cine UADE");
  });
});
