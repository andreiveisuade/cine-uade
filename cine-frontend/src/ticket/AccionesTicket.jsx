import { Anchor, Box, Button, Group, Text } from "@mantine/core";
import { IconQrcode } from "@tabler/icons-react";
import { Link, useLocation } from "react-router";
import { useSesion } from "@/cuenta/SesionDelCliente.jsx";
import { entradaValida } from "./estadoReserva.js";
import { DescargarEntrada } from "./pdf/DescargarEntrada.jsx";

// Lo que se puede hacer con el ticket según su estado y si hay sesión.
export function AccionesTicket({ reserva, alMostrarEnPuerta }) {
  const { usuario } = useSesion();
  const { pathname } = useLocation();

  return (
    <Group gap="sm" justify="space-between" align="flex-start">
      <Box>
        {/* Sin cancelar acá: el ticket es público y se muestra en la puerta; cancelar es del dueño, en Mis reservas. */}
        {entradaValida(reserva) && (
          <Button size="md" leftSection={<IconQrcode size={20} />} onClick={alMostrarEnPuerta}>Mostrar en la puerta</Button>
        )}
        {/* El pago vive en Tu compra, la misma pantalla que después del mapa: un solo lugar para pagar. */}
        {usuario && reserva.cobrable && <Button size="md" component={Link} to={`/compra/${reserva.codigo}`}>Terminar la compra</Button>}
        {usuario === null && reserva.cobrable && (
          <Text size="sm" c="dimmed" maw="52ch">
            Para pagarla online,{" "}
            <Anchor component={Link} to="/ingresar" state={{ volver: pathname }} size="sm">ingresá con tu cuenta de Google</Anchor>.
          </Text>
        )}
      </Box>
      <Group gap="xs">
        {entradaValida(reserva) && <DescargarEntrada reserva={reserva} />}
        <Button variant="subtle" color="gray" c="var(--mantine-color-text)" component={Link} to="/">Volver a la cartelera</Button>
      </Group>
    </Group>
  );
}
