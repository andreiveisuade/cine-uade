import { Button, Group, Text } from "@mantine/core";
import { Link } from "react-router";

// Sin pagar, el candy se suma a la compra y se paga con la entrada (R30); ya paga, se pide en el mostrador.
export function LlamadoAlCandy({ reserva }) {
  if (reserva.estado === "PAGADA") {
    return <Text fw={650}>Pedilo en el candy diciendo tu reserva <Text span c="var(--mantine-color-ambar-text)" fw={800}>#{reserva.id}</Text></Text>;
  }
  return (
    <Group gap="sm">
      <Text fw={650}>Sumalo a tu compra y pagalo con la entrada.</Text>
      {reserva.cobrable && (
        <Button size="compact-md" variant="light" component={Link} to={`/compra/${reserva.codigo}`}>Sumar a mi compra</Button>
      )}
    </Group>
  );
}
