import { createTheme, v8CssVariablesResolver } from "@mantine/core";

// La luz del proyector: el único color de marca. Con autoContrast el texto sobre él sale oscuro.
const ambar = ["#fff8e1", "#ffefb8", "#ffe28a", "#ffd45c", "#fbc433", "#f5b301", "#d99a00", "#b07c00", "#8a6100", "#5e4200"];

// La sala a oscuras: neutros fríos, apenas azulados. dark.7 es la superficie y el fondo va un paso más abajo.
const dark = ["#e6e8ee", "#c4c8d3", "#9ba1af", "#6e7484", "#3b404d", "#2b2f39", "#1e2129", "#15171d", "#0d0f13", "#08090c"];

export const tema = createTheme({
  colors: { ambar, dark },
  primaryColor: "ambar",
  primaryShade: { light: 5, dark: 4 },
  autoContrast: true,
  luminanceThreshold: 0.4,
  fontFamily: "'Archivo Variable', system-ui, -apple-system, 'Segoe UI', sans-serif",
  fontFamilyMonospace: "ui-monospace, 'SF Mono', Menlo, Consolas, monospace",
  fontSizes: { xs: "0.75rem", sm: "0.875rem", md: "1rem", lg: "1.125rem", xl: "1.25rem" },
  spacing: { xs: "0.5rem", sm: "0.75rem", md: "1rem", lg: "1.5rem", xl: "2rem" },
  radius: { xs: "2px", sm: "4px", md: "8px", lg: "12px", xl: "20px" },
  defaultRadius: "md",
  headings: {
    fontFamily: "'Archivo Variable', system-ui, sans-serif",
    fontWeight: "800",
    sizes: {
      h1: { fontSize: "clamp(2.25rem, 1.6rem + 2.6vw, 3.5rem)", lineHeight: "1" },
      h2: { fontSize: "clamp(1.625rem, 1.35rem + 1.1vw, 2.25rem)", lineHeight: "1.05" },
      h3: { fontSize: "1.375rem", lineHeight: "1.15" },
      h4: { fontSize: "1.125rem", lineHeight: "1.25", fontWeight: "700" },
    },
  },
  components: {
    Button: { defaultProps: { radius: "md" }, styles: { root: { fontWeight: 650 } } },
    Badge: { defaultProps: { radius: "sm" } },
    Paper: { defaultProps: { radius: "lg" }, styles: { root: { background: "var(--cine-superficie)" } } },
    Card: { defaultProps: { radius: "lg" }, styles: { root: { background: "var(--cine-superficie)" } } },
  },
});

// Los fondos light de Mantine 9 son sólidos y en oscuro gritan; los de la 8 son translúcidos. Encima, el ámbar
// como texto en claro baja a un tono que da AA sobre blanco.
export function variables(theme) {
  const base = v8CssVariablesResolver(theme);
  const textoAmbarClaro = {
    "--mantine-color-anchor": ambar[8],
    "--mantine-color-ambar-text": ambar[8],
    "--mantine-color-ambar-light-color": ambar[8],
    "--mantine-color-ambar-outline": ambar[7],
  };
  return {
    ...base,
    light: { ...base.light, ...textoAmbarClaro, "--mantine-color-body": "#f4f3ef", "--mantine-color-dimmed": "#5b606d" },
    dark: { ...base.dark, "--mantine-color-anchor": ambar[4], "--mantine-color-body": dark[8], "--mantine-color-dimmed": dark[2] },
  };
}
