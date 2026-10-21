import { List, SimpleGrid, Stack, Text, Title } from "@mantine/core";
import { Encabezado } from "../componentes/Encabezado.jsx";
import { useTitulo } from "../componentes/useTitulo.js";

const INTEGRANTES = ["Andrei Veis", "Lucas Ezequiel Manrique Simonini", "Valentino Garmendia"];

export function Nosotros() {
  useTitulo("Nosotros");
  return (
    <div>
      <Encabezado titulo="Nosotros" />
      <SimpleGrid cols={{ base: 1, md: 2 }} spacing="xl" maw={1040}>
        <Text fz="lg" lh={1.65} maw="60ch">
          Cine UADE es un cine de barrio con salas 2D y 3D. Elegís la película y la función, reservás tus
          butacas y pagás online, y sumás candy a la entrada si querés. Después llegás, mostrás el código
          y entrás.
        </Text>
        <Stack gap="sm">
          <Title order={2} fz="h3">Integrantes</Title>
          <List spacing="xs">
            {INTEGRANTES.map((n) => <List.Item key={n}>{n}</List.Item>)}
          </List>
          <Text c="dimmed" size="sm" mt="sm">Trabajo Práctico Obligatorio de Aplicaciones Interactivas, UADE, 2C 2026.</Text>
        </Stack>
      </SimpleGrid>
    </div>
  );
}
