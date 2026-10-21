import { Alert, Center, Group, Loader, Stack, Text } from "@mantine/core";

export function Cargando({ mensaje = "Cargando…" }) {
  return (
    <Center py="xl">
      <Group gap="sm">
        <Loader size="sm" />
        <Text c="dimmed">{mensaje}</Text>
      </Group>
    </Center>
  );
}

// Tal cual: el mensaje lo escribió el gestor para que lo lea una persona.
export function ErrorCaja({ children }) {
  return (
    <Alert color="red" variant="light">
      {children}
    </Alert>
  );
}

// `esqueleto` es la forma de la pantalla que viene: se ve dónde va a quedar cada cosa antes de que llegue.
export function EsperaOError({ carga, esqueleto = <Cargando /> }) {
  return carga.error ? <ErrorCaja>{carga.error}</ErrorCaja> : esqueleto;
}

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
