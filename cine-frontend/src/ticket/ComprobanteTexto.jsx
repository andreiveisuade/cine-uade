import { Box, Code } from "@mantine/core";
import { armarComprobante } from "./armarComprobante.js";

export function ComprobanteTexto({ reserva }) {
  return (
    <Box component="details" c="dimmed" fz="sm">
      <Box component="summary" style={{ cursor: "pointer" }}>Ver el comprobante en texto</Box>
      <Code block fz="xs" mt="sm">{armarComprobante(reserva)}</Code>
    </Box>
  );
}
