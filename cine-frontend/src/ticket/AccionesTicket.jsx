import { useState } from "react";
import { Anchor, Button, Code, Stack, Text } from "@mantine/core";
import { IconQrcode, IconReceipt } from "@tabler/icons-react";
import { Link, useLocation } from "react-router";
import { useSesion } from "@/cuenta/SesionDelCliente.jsx";
import { entradaValida } from "@/compartido/estadoReserva.js";
import { armarComprobante } from "./armarComprobante.js";
import { DescargarEntrada } from "./pdf/DescargarEntrada.jsx";

// Lo que se puede hacer con el ticket según su estado y si hay sesión: una columna centrada, del más usado al menos.
export function AccionesTicket({ reserva, alMostrarEnPuerta }) {
  const { usuario } = useSesion();
  const { pathname } = useLocation();
  const [conComprobante, setConComprobante] = useState(false);
  const valida = entradaValida(reserva);

  return (
    <Stack align="center" gap="md">
      <Stack gap="sm" w="100%" maw={320} role="group" aria-label="Acciones del ticket">
        {/* El pago vive en Tu compra, la misma pantalla que después del mapa: un solo lugar para pagar. */}
        {usuario && reserva.cobrable && (
          <Button size="md" fullWidth component={Link} to={`/compra/${reserva.codigo}`}>Terminar la compra</Button>
        )}
        {usuario === null && reserva.cobrable && (
          <Text size="sm" c="dimmed" ta="center">
            Para pagarla online,{" "}
            <Anchor component={Link} to="/ingresar" state={{ volver: pathname }} size="sm">ingresá con tu cuenta de Google</Anchor>.
          </Text>
        )}
        {/* Sin cancelar acá: el ticket es público y se muestra en la puerta; cancelar es del dueño, en Mis reservas. */}
        {valida && (
          <Button size="md" fullWidth leftSection={<IconQrcode size={20} />} onClick={alMostrarEnPuerta}>Mostrar en la puerta</Button>
        )}
        {valida && <DescargarEntrada reserva={reserva} size="md" fullWidth />}
        <Button size="md" fullWidth variant="default" leftSection={<IconReceipt size={18} />}
          aria-expanded={conComprobante} onClick={() => setConComprobante((abierto) => !abierto)}>
          Ver el comprobante en texto
        </Button>
        <Button size="md" fullWidth variant="subtle" color="gray" c="var(--mantine-color-text)" component={Link} to="/">
          Volver a la cartelera
        </Button>
      </Stack>
      {conComprobante && <Code block fz="xs" w="100%" maw={480}>{armarComprobante(reserva)}</Code>}
    </Stack>
  );
}
