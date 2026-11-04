// El ámbar queda para lo elegido: ningún tipo de butaca lo usa.
const COLOR_TIPO = { VIP: "grape", PAREJA: "pink", ACCESIBLE: "blue" };

// Toda butaca libre se ve igual de elegible: el tipo es una franja abajo, no un borde que compita con las demás.
export function estiloTipo(tipo) {
  const color = COLOR_TIPO[tipo];
  return {
    border: "1px solid var(--cine-butaca-borde)",
    background: "var(--cine-butaca-libre)",
    color: "var(--mantine-color-text)",
    ...(color && { boxShadow: `inset 0 -3px 0 var(--mantine-color-${color}-5)` }),
  };
}

export const ESTILO = {
  elegida: { background: "var(--mantine-color-ambar-4)", color: "#15171d", fontWeight: 700, border: "1px solid transparent",
             boxShadow: "0 0 0 3px color-mix(in srgb, var(--mantine-color-ambar-4) 30%, transparent)" },
  ocupada: { background: "var(--cine-butaca-ocupada)", color: "var(--mantine-color-dimmed)", border: "1px solid var(--cine-borde)", cursor: "not-allowed" },
  fueraDeServicio: {
    background: "var(--mantine-color-default-hover)", color: "var(--mantine-color-dimmed)",
    textDecoration: "line-through", border: "1px dashed var(--mantine-color-default-border)", cursor: "not-allowed",
  },
};
