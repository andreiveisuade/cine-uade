import { Anchor, Box, Group, Stack, Text } from "@mantine/core";
import { Link } from "react-router";
import { Contenedor } from "../componentes/Contenedor.jsx";
import { Marca } from "./Barra.jsx";

const ENLACES = [["/cartelera", "Cartelera"], ["/promociones", "Promos y candy"], ["/nosotros", "Nosotros"]];

export function Pie() {
  return (
    <Box component="footer" mt="var(--cine-seccion)" py="xl" style={{ borderTop: "1px solid var(--cine-borde)" }}>
      <Contenedor>
        <Group justify="space-between" align="flex-start" gap="lg">
          <Stack gap={6}>
            <Marca />
            <Text size="sm" c="dimmed" maw="34ch">Salas 2D y 3D. Elegís la butaca, reservás y entrás con tu código.</Text>
          </Stack>
          <Group gap="lg" wrap="wrap">
            {ENLACES.map(([a, texto]) => (
              <Anchor key={a} component={Link} to={a} size="sm" c="dimmed">{texto}</Anchor>
            ))}
          </Group>
        </Group>
        <Text size="xs" c="dimmed" mt="lg">Trabajo Práctico Obligatorio de Aplicaciones Interactivas, UADE, 2C 2026.</Text>
      </Contenedor>
    </Box>
  );
}
