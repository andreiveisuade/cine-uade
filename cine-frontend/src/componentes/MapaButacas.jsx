import { Box, Group, Paper, Stack, Text, UnstyledButton } from "@mantine/core";

export const SIMBOLO = { VIP: "*", PAREJA: "&", ACCESIBLE: "+", ESTANDAR: "" };

const COLOR_TIPO = { VIP: "yellow", PAREJA: "pink", ACCESIBLE: "blue", ESTANDAR: "gray" };

export function estiloTipo(tipo) {
  const color = COLOR_TIPO[tipo];
  return {
    border: `1px solid var(--mantine-color-${color}-${tipo === "ESTANDAR" ? 5 : 6})`,
    background: tipo === "ESTANDAR" ? "var(--mantine-color-body)" : `var(--mantine-color-${color}-light)`,
    color: "var(--mantine-color-text)",
  };
}

export const ESTILO = {
  elegida: { background: "var(--mantine-color-green-filled)", color: "white", border: "1px solid transparent" },
  ocupada: { background: "var(--mantine-color-gray-6)", color: "white", border: "1px solid transparent", cursor: "not-allowed" },
  fueraDeServicio: {
    background: "var(--mantine-color-default-hover)", color: "var(--mantine-color-dimmed)",
    textDecoration: "line-through", border: "1px dashed var(--mantine-color-default-border)", cursor: "not-allowed",
  },
};

// pintar: (asiento) => { estilo, deshabilitado, titulo }
export function MapaButacas({ sala, asientos, pintar, alElegir }) {
  const filas = [];
  for (let fila = 1; fila <= sala.filas; fila++) filas.push(asientos.filter((a) => a.fila === fila));

  return (
    <Paper withBorder p="md" style={{ overflowX: "auto" }}>
      <Box mb="md" py={4} bg="dark.6" c="white" ta="center" fz={10} style={{ letterSpacing: "0.3em", borderRadius: 4 }}>
        PANTALLA
      </Box>
      <Stack gap={4} style={{ minWidth: "max-content" }} mx="auto" w="fit-content">
        {filas.map((deLaFila, i) => (
          <Group key={i} gap={4} justify="center" wrap="nowrap">
            <Text w={16} ta="right" fz="xs" fw={600} c="dimmed">{deLaFila[0]?.codigo.charAt(0)}</Text>
            {deLaFila.map((a) => {
              const { estilo, deshabilitado, titulo } = pintar(a);
              return (
                <UnstyledButton key={a.codigo} title={titulo} disabled={deshabilitado}
                  onClick={() => alElegir?.(a)}
                  style={{ height: 28, width: a.tipo === "PAREJA" ? 48 : 28, borderRadius: 4, fontSize: 10,
                           fontWeight: 500, textAlign: "center", flexShrink: 0, ...estilo }}>
                  {a.numero}{SIMBOLO[a.tipo]}
                </UnstyledButton>
              );
            })}
          </Group>
        ))}
      </Stack>
    </Paper>
  );
}

export function Referencia({ items }) {
  return (
    <Group gap="md" mt="sm">
      {items.map(([estilo, texto]) => (
        <Group key={texto} gap={6} wrap="nowrap">
          <Box w={12} h={12} style={{ borderRadius: 3, ...estilo }} />
          <Text fz="xs" c="dimmed">{texto}</Text>
        </Group>
      ))}
    </Group>
  );
}
