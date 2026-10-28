import { Badge, Box, Stack, Text } from "@mantine/core";
import { m } from "motion/react";
import { QRCodeSVG } from "qrcode.react";
import { fechaHora } from "@/compartido/formato.js";
import { CODIGO_POR_ESTADO, codigoLegible, vigente } from "@/ticket/estadoReserva.js";
import { CopiarCodigo } from "./CopiarCodigo.jsx";
import clases from "./Entrada.module.css";

// El sello cae solo si el pago se vio llegar en esta pantalla; un ticket que ya abrió pago lo muestra quieto.
const SELLO_CAYENDO = { opacity: 0, scale: 2.2, rotate: -24 };
const SELLO = { opacity: 1, scale: 1, rotate: -8 };

export function TalonEntrada({ reserva, pagoEnVivo }) {
  const usada = reserva.ingresadaEn;
  return (
    <Stack gap="sm" align="center" ta="center" justify="center" h="100%" pos="relative">
      {reserva.estado === "PAGADA" && (
        <Badge component={m.div} initial={pagoEnVivo ? SELLO_CAYENDO : false} animate={SELLO}
          transition={{ type: "spring", stiffness: 400, damping: 18 }}
          color="green" variant="outline" size="lg" pos="absolute" top={-4} right={-4} style={{ borderWidth: 2 }}>
          Pagada
        </Badge>
      )}
      <Text size="xs" c="dimmed" tt="uppercase" fw={700} style={{ letterSpacing: "0.12em" }}>Código de acceso</Text>
      {vigente(reserva) && (
        <Box p={8} bg="white" style={{ borderRadius: "var(--mantine-radius-sm)", lineHeight: 0 }}>
          <QRCodeSVG value={reserva.codigo} size={132} level="M" title={`QR del código ${reserva.codigo}`} />
        </Box>
      )}
      {/* El ícono cuelga a la derecha para que el código siga centrado bajo el QR. */}
      <Box pos="relative">
        <Text ff="monospace" fw={700} className={`cine-cifras ${clases.codigo}`}
          style={{ letterSpacing: "0.18em", textDecoration: usada ? "line-through" : "none" }}>
          {codigoLegible(reserva.codigo)}
        </Text>
        <Box pos="absolute" left="100%" top="50%" style={{ transform: "translateY(-50%)" }}>
          <CopiarCodigo codigo={reserva.codigo} />
        </Box>
      </Box>
      <Text size="xs" c="dimmed" maw="24ch">
        {usada ? `Ya se usó el ${fechaHora(usada)}` : CODIGO_POR_ESTADO[reserva.estado]}
      </Text>
    </Stack>
  );
}
