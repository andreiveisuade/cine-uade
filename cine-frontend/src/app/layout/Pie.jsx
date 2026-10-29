import { Anchor, Box, Group, Text } from "@mantine/core";
import { Link } from "react-router";
import { Contenedor } from "@/compartido/layout/Contenedor.jsx";

const ENLACES = [["/cartelera", "Cartelera"], ["/promociones", "Promos"], ["/mis-reservas", "Mis reservas"], ["/nosotros", "Nosotros"]];

// Una franja baja que no compite con el contenido. En celular la navegación ya está en el menú: no se repite.
export function Pie() {
  return (
    <Box component="footer" mt="var(--cine-seccion)" style={{ borderTop: "1px solid var(--cine-borde)" }}>
      <Contenedor py="md">
        <Group justify="space-between" gap="md" wrap="wrap">
          <Group gap="lg" wrap="nowrap">
            <Group gap={8} wrap="nowrap">
              <img src="/favicon.svg" alt="" width={20} height={20} />
              <Text className="cine-display" fw={800} fz="sm">CINE UADE</Text>
            </Group>
            <Group gap="md" component="nav" aria-label="Pie" visibleFrom="sm">
              {ENLACES.map(([a, texto]) => (
                <Anchor key={a} component={Link} to={a} size="xs" c="dimmed">{texto}</Anchor>
              ))}
            </Group>
          </Group>
          <Group gap="lg" wrap="wrap">
            {/* Fuera del nav, que en celular se oculta: es una página aparte, sin ruteo por hash. */}
            <Anchor href="/privacidad.html" size="xs" c="dimmed">Privacidad</Anchor>
            <Text size="xs" c="dimmed">TPO Aplicaciones Interactivas · UADE · 2C 2026</Text>
          </Group>
        </Group>
      </Contenedor>
    </Box>
  );
}
