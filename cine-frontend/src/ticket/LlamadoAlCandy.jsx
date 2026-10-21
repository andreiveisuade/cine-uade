import { Button, Group, Text } from "@mantine/core";

// El candy no se reserva: se paga en el mostrador. Lo que cambia según el estado es qué tiene que hacer el cliente.
export function LlamadoAlCandy({ reserva }) {
  if (reserva.estado === "PAGADA") {
    return <Text fw={650}>Pedilo en el candy diciendo tu reserva <Text span c="var(--mantine-color-ambar-text)" fw={800}>#{reserva.id}</Text></Text>;
  }
  return (
    <Group gap="sm">
      <Text fw={650}>Pagá online y sumá tu combo en el candy.</Text>
      {reserva.cobrable && (
        <Button size="compact-md" variant="light" onClick={() => document.getElementById("pagar")?.scrollIntoView({ behavior: "smooth", block: "center" })}>
          Pagar online
        </Button>
      )}
    </Group>
  );
}
