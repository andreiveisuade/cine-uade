import { Stack, Text } from "@mantine/core";

// Un hueco con borde punteado: se lee como "acá iría algo", no como un error.
export function Vacio({ children, accion }) {
  return (
    <Stack align="center" gap="sm" py="xl" px="md" ta="center"
      style={{ border: "1px dashed var(--cine-borde)", borderRadius: "var(--mantine-radius-lg)" }}>
      <Text c="dimmed">{children}</Text>
      {accion}
    </Stack>
  );
}
