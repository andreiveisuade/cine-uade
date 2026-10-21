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

// `esqueleto` es la forma de la pantalla que viene: se ve dónde va a quedar cada cosa antes de que llegue.
export function EsperaOError({ carga, esqueleto = <Cargando /> }) {
  return carga.error ? <ErrorCaja>{carga.error}</ErrorCaja> : esqueleto;
}

export function Vacio({ children }) {
  return (
    <Text c="dimmed" ta="center" py="xl">
      {children}
    </Text>
  );
}
