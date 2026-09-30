import { Alert, Center, Group, Loader, Text } from "@mantine/core";

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

export function EsperaOError({ carga }) {
  return carga.error ? <ErrorCaja>{carga.error}</ErrorCaja> : <Cargando />;
}

export function Vacio({ children }) {
  return (
    <Text c="dimmed" ta="center" py="xl">
      {children}
    </Text>
  );
}
