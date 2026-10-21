import { Stack, Text } from "@mantine/core";

// Día grande y mes chico, como en una agenda: se encuentra la función de un vistazo.
export function FechaEnBloque({ iso }) {
  return (
    <Stack gap={0} align="center" w={56} py={6} bg="var(--cine-superficie-2)" style={{ borderRadius: "var(--mantine-radius-md)", flexShrink: 0 }}>
      <Text className="cine-display cine-cifras" fz={28} fw={850} lh={1}>{iso.slice(8, 10)}</Text>
      <Text size="xs" c="dimmed" className="cine-cifras">/{iso.slice(5, 7)}</Text>
    </Stack>
  );
}
