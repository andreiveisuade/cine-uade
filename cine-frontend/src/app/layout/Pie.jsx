import { Anchor, Box, Group, Text } from "@mantine/core";
import { Link } from "react-router";
import * as api from "@/api/api-http.js";
import { Contenedor } from "@/compartido/layout/Contenedor.jsx";
import { MediosDePago } from "@/compartido/ui/MediosDePago.jsx";
import { useCargar } from "@/compartido/hooks/useCargar.js";

const ENLACES = [["/cartelera", "Cartelera"], ["/promociones", "Promos"], ["/mis-reservas", "Mis reservas"], ["/nosotros", "Nosotros"]];

// Una franja baja que no compite con el contenido. En celular la navegación ya está en el menú: no se repite.
// Los medios salen del catálogo del backend; si no llegan, no aparecen.
export function Pie() {
  const medios = useCargar(() => api.obtenerMediosDePago(), []);
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
            {medios.datos?.length > 0 && <MediosDePago medios={medios.datos.map((m) => m.nombre)} tamanio={18} />}
            <Text size="xs" c="dimmed">TPO Aplicaciones Interactivas · UADE · 2C 2026</Text>
          </Group>
        </Group>
      </Contenedor>
    </Box>
  );
}
