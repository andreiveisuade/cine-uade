import { Alert, Center, Group, Loader, Text } from "@mantine/core";

/**
 * El círculo que gira es lo único que separa «está tardando» de «se colgó»: quieta, una
 * espera de más de un segundo se lee como una pantalla rota, y lo que hace el usuario es
 * volver a apretar.
 */
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

/** El mensaje del backend, tal cual: lo escribió el gestor para que lo lea una persona. */
export function ErrorCaja({ children }) {
  return (
    <Alert color="red" variant="light">
      {children}
    </Alert>
  );
}

/**
 * Lo que muestra una pantalla mientras su `useCargar` no tiene datos: la espera o el error.
 * Así cada vista arranca con `if (!carga.datos) return <EsperaOError carga={carga} />`.
 */
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
