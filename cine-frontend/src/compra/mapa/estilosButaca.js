// El ámbar queda para lo elegido: ningún tipo de butaca lo usa.
const COLOR_TIPO = { VIP: "grape", PAREJA: "pink", ACCESIBLE: "blue", ESTANDAR: "gray" };

export function estiloTipo(tipo) {
  const color = COLOR_TIPO[tipo];
  return {
    border: tipo === "ESTANDAR" ? "1px solid var(--cine-borde)" : `1.5px solid var(--mantine-color-${color}-5)`,
    background: tipo === "ESTANDAR" ? "var(--cine-superficie-2)" : `var(--mantine-color-${color}-light)`,
    color: "var(--mantine-color-text)",
  };
}

export const ESTILO = {
  elegida: { background: "var(--mantine-color-ambar-4)", color: "#15171d", fontWeight: 700, border: "1px solid transparent",
             boxShadow: "0 0 0 3px color-mix(in srgb, var(--mantine-color-ambar-4) 30%, transparent)" },
  ocupada: { background: "var(--mantine-color-dark-3)", color: "var(--mantine-color-dark-0)", border: "1px solid transparent", cursor: "not-allowed", opacity: 0.55 },
  fueraDeServicio: {
    background: "var(--mantine-color-default-hover)", color: "var(--mantine-color-dimmed)",
    textDecoration: "line-through", border: "1px dashed var(--mantine-color-default-border)", cursor: "not-allowed",
  },
};
