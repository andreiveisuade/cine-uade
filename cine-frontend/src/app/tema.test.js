import { describe, expect, it } from "vitest";
import { DEFAULT_THEME, mergeMantineTheme } from "@mantine/core";
import { tema, variables } from "./tema.js";

const oscuro = variables(mergeMantineTheme(DEFAULT_THEME, tema)).dark;

describe("el tema oscuro", () => {
  it("el rojo light se ve rojo: el primaryShade del ámbar no lo lava", () => {
    expect(oscuro["--mantine-color-red-light"]).toBe("rgba(250, 82, 82, 0.15)");
    expect(oscuro["--mantine-color-red-light-color"]).toBe(DEFAULT_THEME.colors.red[3]);
  });

  it("el ámbar sigue con su primaryShade", () => {
    expect(oscuro["--mantine-color-ambar-light-color"]).toBe("var(--mantine-color-ambar-0)");
  });
});
