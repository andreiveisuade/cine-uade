import { Anchor, Box, Button, Group, Text } from "@mantine/core";
import { IconQrcode } from "@tabler/icons-react";
import { Link, useLocation } from "react-router";
import { useSesion } from "@/cuenta/SesionDelCliente.jsx";
import { vigente } from "./estadoReserva.js";
import { DescargarEntrada } from "./pdf/DescargarEntrada.jsx";
import { PagarOnline } from "./pago/PagarOnline.jsx";

// Lo que se puede hacer con el ticket según su estado y si hay sesión.
export function AccionesTicket({ reserva, recien, alMostrarEnPuerta, alVolverDelPago }) {
  const { usuario } = useSesion();
  const { pathname } = useLocation();

  return (
    <Group gap="sm" justify="space-between" align="flex-start">
      <Box id="pagar">
        {/* Sin cancelar acá: el ticket es público y se muestra en la puerta; cancelar es del dueño, en Mis reservas. */}
        {reserva.estado === "PAGADA" && !reserva.ingresadaEn && (
          <Button size="md" leftSection={<IconQrcode size={20} />} onClick={alMostrarEnPuerta}>Mostrar en la puerta</Button>
        )}
        {usuario && reserva.cobrable && <PagarOnline codigo={reserva.codigo} alVolver={alVolverDelPago} />}
        {usuario === null && recien && reserva.cobrable && (
          <Text size="sm" c="dimmed" maw="52ch">
            Para pagarla online,{" "}
            <Anchor component={Link} to="/ingresar" state={{ volver: pathname }} size="sm">ingresá con Google</Anchor>{" "}
            con {reserva.cliente.email}. Si no, se paga en la boletería.
          </Text>
        )}
      </Box>
      <Group gap="xs">
        {vigente(reserva) && <DescargarEntrada reserva={reserva} />}
        <Button variant="subtle" color="gray" c="var(--mantine-color-text)" component={Link} to="/">Volver a la cartelera</Button>
      </Group>
    </Group>
  );
}
