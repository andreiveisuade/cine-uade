import { Button, Group, Text } from "@mantine/core";
import { Link } from "react-router";

// Sin pagar, el candy se suma a la compra y se paga con la entrada; ya paga, se suma y se paga aparte (R30).
export function LlamadoAlCandy({ reserva }) {
  if (reserva.estado === "PAGADA") {
    return (
      <Group gap="sm">
        <Text fw={650}>Sumalo ahora y retiralo con el QR de tu entrada.</Text>
        <Button size="compact-md" variant="light" component={Link} to={`/compra/${reserva.codigo}`}>Sumar candy</Button>
      </Group>
    );
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
