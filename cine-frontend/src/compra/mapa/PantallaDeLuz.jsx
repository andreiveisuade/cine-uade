import { Box, Text } from "@mantine/core";

// La pantalla es una curva de luz: de ahí sale el ámbar del sitio.
export function PantallaDeLuz() {
  return (
    <Box mx="auto" mb="xl" miw={280} maw={520} aria-hidden>
      <Box h={14} style={{ borderTop: "3px solid var(--mantine-color-ambar-4)", borderRadius: "50% / 100% 100% 0 0",
                           boxShadow: "0 -10px 28px -8px var(--mantine-color-ambar-4)" }} />
      <Text ta="center" fz={10} fw={700} c="dimmed" style={{ letterSpacing: "0.35em" }}>PANTALLA</Text>
    </Box>
  );
}
