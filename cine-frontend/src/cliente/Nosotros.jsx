import { List, Stack, Text, Title } from "@mantine/core";

const INTEGRANTES = ["Andrei Veis", "Lucas Ezequiel Manrique Simonini", "Valentino Garmendia"];

export function Nosotros() {
  return (
    <Stack gap="md" maw={640}>
      <Title order={1}>Nosotros</Title>
      <Text>
        Cine UADE es un cine de barrio con salas 2D y 3D. Elegís la película y la función, reservás tus
        butacas y pagás online, y sumás candy a la entrada si querés. Después llegás, mostrás el código
        y entrás.
      </Text>
      <Title order={2}>Integrantes</Title>
      <List>
        {INTEGRANTES.map((n) => <List.Item key={n}>{n}</List.Item>)}
      </List>
      <Text c="dimmed" size="sm">
        Trabajo Práctico Obligatorio de Aplicaciones Interactivas, UADE, 2C 2026.
      </Text>
    </Stack>
  );
}
