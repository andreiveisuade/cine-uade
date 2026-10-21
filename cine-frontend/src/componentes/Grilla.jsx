import { Box } from "@mantine/core";

// Tantas columnas como entren de `min`: 3 ítems o 30 usan la misma regla y nunca quedan más angostos que eso.
export function Grilla({ min = 220, gap = "var(--mantine-spacing-lg)", children, ...props }) {
  return (
    <Box style={{ display: "grid", gap, gridTemplateColumns: `repeat(auto-fill, minmax(min(100%, ${typeof min === "number" ? `${min}px` : min}), 1fr))` }} {...props}>
      {children}
    </Box>
  );
}
