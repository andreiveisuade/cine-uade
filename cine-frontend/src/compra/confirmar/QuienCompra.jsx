import { Button, Paper, Stack, Text, Title } from "@mantine/core";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";

// La reserva es del cliente de la sesión (R28): se ve con qué cuenta se compra, sin datos que tipear.
export function QuienCompra({ usuario, enviando, error, alConfirmar }) {
  return (
    <Paper withBorder p="lg">
      <Title order={2} fz="h4" mb="xs">Comprás con tu cuenta</Title>
      <Text fw={650}>{usuario.nombre}</Text>
      <Text size="sm" c="dimmed" mb="md">{usuario.email}</Text>
      <Stack gap="sm">
        <Button size="md" loading={enviando} onClick={alConfirmar}>Confirmar reserva</Button>
        {error && <ErrorCaja>{error}</ErrorCaja>}
      </Stack>
    </Paper>
  );
}
