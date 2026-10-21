import { Box, Modal, Stack, Text } from "@mantine/core";
import { QRCodeSVG } from "qrcode.react";
import { codigoLegible } from "./estadoReserva.js";

// Para la puerta: el código y el QR a pantalla llena, con brillo máximo de contraste.
export function EnLaPuerta({ reserva, abierto, alCerrar }) {
  return (
    <Modal opened={abierto} onClose={alCerrar} centered title="Mostralo en la puerta" radius="lg">
      <Stack align="center" gap="md" pb="md">
        <Box p="md" bg="white" style={{ borderRadius: "var(--mantine-radius-md)", lineHeight: 0 }}>
          <QRCodeSVG value={reserva.codigo} size={240} level="M" />
        </Box>
        <Text ff="monospace" fz={40} fw={700} style={{ letterSpacing: "0.18em" }}>
          {codigoLegible(reserva.codigo)}
        </Text>
      </Stack>
    </Modal>
  );
}
