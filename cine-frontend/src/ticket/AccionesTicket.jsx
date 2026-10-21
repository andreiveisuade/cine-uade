import { Anchor, Box, Button, Group, Text } from "@mantine/core";
import { IconCopy, IconFileText, IconQrcode } from "@tabler/icons-react";
import { Link } from "react-router";
import { useSesion } from "@/cliente/sesion.jsx";
import { useAvisar } from "@/compartido/estado/Avisos.jsx";
import { armarComprobante } from "./armarComprobante.js";
import { PagarOnline } from "./PagarOnline.jsx";

// Lo que se puede hacer con el ticket según su estado y si hay sesión.
export function AccionesTicket({ reserva, recien, alMostrarEnPuerta }) {
  const { usuario } = useSesion();
  const avisar = useAvisar();

  async function copiar(texto, aviso) {
    // Sin permiso o fuera de HTTPS el navegador rechaza la escritura.
    try {
      await navigator.clipboard.writeText(texto);
      avisar(aviso);
    } catch {
      avisar("No pudimos copiar. Seleccionalo y copialo a mano.", "error");
    }
  }

  return (
    <Group gap="sm" justify="space-between" align="flex-start">
      <Box id="pagar">
        {/* Sin cancelar acá: el ticket es público y se muestra en la puerta; cancelar es del dueño, en Mis reservas. */}
        {reserva.estado === "PAGADA" && !reserva.ingresadaEn && (
          <Button size="md" leftSection={<IconQrcode size={20} />} onClick={alMostrarEnPuerta}>Mostrar en la puerta</Button>
        )}
        {usuario && reserva.cobrable && <PagarOnline codigo={reserva.codigo} />}
        {usuario === null && recien && reserva.cobrable && (
          <Text size="sm" c="dimmed" maw="52ch">
            Para pagarla online,{" "}
            <Anchor component={Link} to="/registro" state={{ email: reserva.cliente.email }} size="sm">registrate</Anchor>{" "}
            o <Anchor component={Link} to="/ingresar" state={{ email: reserva.cliente.email }} size="sm">ingresá</Anchor>{" "}
            con {reserva.cliente.email}. Si no, se paga en la boletería.
          </Text>
        )}
      </Box>
      <Group gap="xs">
        <Button variant="default" leftSection={<IconCopy size={16} />} onClick={() => copiar(reserva.codigo, "Código copiado")}>Copiar código</Button>
        <Button variant="default" leftSection={<IconFileText size={16} />} onClick={() => copiar(armarComprobante(reserva), "Comprobante copiado")}>
          Copiar comprobante
        </Button>
        <Button variant="subtle" color="gray" c="var(--mantine-color-text)" component={Link} to="/">Volver a la cartelera</Button>
      </Group>
    </Group>
  );
}
