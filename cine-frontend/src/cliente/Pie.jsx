import { Anchor, Badge, Box, Group, SimpleGrid, Stack, Text } from "@mantine/core";
import { Link } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { Contenedor } from "../componentes/Contenedor.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { Marca } from "./Barra.jsx";

const ENLACES = [["/cartelera", "Cartelera"], ["/promociones", "Promos"], ["/mis-reservas", "Mis reservas"], ["/nosotros", "Nosotros"]];

function Columna({ titulo, children }) {
  return (
    <Stack gap="sm">
      <Text size="sm" fw={700}>{titulo}</Text>
      {children}
    </Stack>
  );
}

// Solo lo que el sistema sabe: los medios salen del catálogo del backend; si no llegan, la columna no aparece.
export function Pie() {
  const medios = useCargar(() => api.obtenerMediosDePago(), []);
  return (
    <Box component="footer" mt="var(--cine-seccion)" bg="var(--cine-superficie)" style={{ borderTop: "1px solid var(--cine-borde)" }}>
      <Contenedor py="xl">
        <SimpleGrid cols={{ base: 1, xs: 2, md: 3 }} spacing="xl">
          <Group gap="sm" wrap="nowrap" align="center" h="fit-content">
            <img src="/favicon.svg" alt="" width={32} height={32} />
            <Marca />
          </Group>
          <Columna titulo="El cine">
            <Stack gap={8} component="nav" aria-label="Pie">
              {ENLACES.map(([a, texto]) => (
                <Anchor key={a} component={Link} to={a} size="sm" c="dimmed" w="fit-content">{texto}</Anchor>
              ))}
            </Stack>
          </Columna>
          {medios.datos?.length > 0 && (
            <Columna titulo="Medios de pago">
              <Group gap={6}>
                {medios.datos.map((m) => (
                  <Badge key={m.nombre} variant="default" size="lg" tt="none" fw={500}>{etiqueta(m.nombre)}</Badge>
                ))}
              </Group>
              <Text size="xs" c="dimmed">El efectivo, en la boletería.</Text>
            </Columna>
          )}
        </SimpleGrid>
      </Contenedor>
      <Box style={{ borderTop: "1px solid var(--cine-borde)" }}>
        <Contenedor py="md">
          <Text size="xs" c="dimmed">TPO Aplicaciones Interactivas · UADE · 2C 2026</Text>
        </Contenedor>
      </Box>
    </Box>
  );
}
