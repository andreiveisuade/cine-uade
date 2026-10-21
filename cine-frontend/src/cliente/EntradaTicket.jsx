import { createElement } from "react";
import { Badge, Box, Group, SimpleGrid, Stack, Text, Title } from "@mantine/core";
import { m } from "motion/react";
import { QRCodeSVG } from "qrcode.react";
import { etiqueta } from "../api/etiquetas.js";
import { dia, fechaHora, hora, precio } from "../api/formato.js";
import { iconoTarifa } from "../api/iconos.js";
import { Entrada } from "../componentes/Entrada.jsx";
import { Poster } from "../componentes/Poster.jsx";

// Qué le decimos al comprador según el estado que manda el backend; el front no decide vencimientos.
const AVISO_POR_ESTADO = {
  RESERVADA: "Reserva confirmada. Pagala online o en boletería antes de que venza.",
  PAGADA: "Entrada paga. Mostrá el código en la puerta.",
  EXPIRADA: "Esta reserva venció. Las butacas ya se liberaron.",
  CANCELADA: "Esta reserva está cancelada.",
};

const COLOR_ESTADO = { RESERVADA: "ambar", PAGADA: "green", EXPIRADA: "gray", CANCELADA: "gray" };

const CODIGO_POR_ESTADO = {
  RESERVADA: "Todavía no está paga. Sirve una sola vez cuando se pague.",
  PAGADA: "Mostralo en la puerta. Sirve una sola vez.",
  EXPIRADA: "Ya no sirve: la reserva venció.",
  CANCELADA: "Ya no sirve: la reserva se canceló.",
};

// El sello cae solo si el pago se vio llegar en esta pantalla; un ticket que ya abrió pago lo muestra quieto.
const SELLO_CAYENDO = { opacity: 0, scale: 2.2, rotate: -24 };
const SELLO = { opacity: 1, scale: 1, rotate: -8 };

export const conAcreditacion = (entrada) => entrada.tarifa && entrada.tarifa !== "GENERAL";

export const vigente = (reserva) => ["RESERVADA", "PAGADA"].includes(reserva.estado) && !reserva.ingresadaEn;

function Dato({ titulo, children, grande }) {
  return (
    <Stack gap={2}>
      <Text size="xs" c="dimmed" tt="uppercase" fw={700} style={{ letterSpacing: "0.06em" }}>{titulo}</Text>
      <Text className={grande ? "cine-display cine-cifras" : "cine-cifras"} fz={grande ? 30 : "md"} fw={grande ? 850 : 600} lh={1.1}>
        {children}
      </Text>
    </Stack>
  );
}

// La tarifa reducida lleva su ícono y la nota del carnet al lado de la butaca, no en un aviso aparte.
function Butaca({ entrada }) {
  return (
    <Group gap="sm" wrap="nowrap" justify="space-between" py={8} style={{ borderBottom: "1px dashed var(--cine-borde)" }}>
      <Group gap="sm" wrap="nowrap">
        <Badge size="lg" variant="default" radius="sm" className="cine-cifras" miw={52}>{entrada.codigo}</Badge>
        {conAcreditacion(entrada) ? (
          <Group gap={6} wrap="nowrap">
            {createElement(iconoTarifa(entrada.tarifa), { size: 18, stroke: 1.75, "aria-hidden": true, color: "var(--mantine-color-ambar-text)" })}
            <Text size="sm" fw={600}>{etiqueta(entrada.tarifa)}</Text>
            <Text size="xs" c="dimmed">· traé el carnet</Text>
          </Group>
        ) : (
          <Text size="sm" c="dimmed">{etiqueta(entrada.tarifa || "GENERAL")}</Text>
        )}
      </Group>
      <Text size="sm" fw={600} className="cine-cifras">{precio(entrada.precio)}</Text>
    </Group>
  );
}

function Talon({ reserva, pagoEnVivo }) {
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
      <Text ff="monospace" fz={28} fw={700} className="cine-cifras"
        style={{ letterSpacing: "0.18em", textDecoration: usada ? "line-through" : "none" }}>
        {reserva.codigo.slice(0, 4)} {reserva.codigo.slice(4)}
      </Text>
      <Text size="xs" c="dimmed" maw="24ch">
        {usada ? `Ya se usó el ${fechaHora(usada)}` : CODIGO_POR_ESTADO[reserva.estado]}
      </Text>
    </Stack>
  );
}

// El ticket es una entrada de verdad: cuerpo con la función y las butacas, talón con el código para la puerta.
export function EntradaTicket({ reserva, pagoEnVivo }) {
  const atenuada = !vigente(reserva);
  const sala = reserva.sala || reserva.funcion.sala;
  return (
    <Entrada talon={reserva.codigo && <Talon reserva={reserva} pagoEnVivo={pagoEnVivo} />}
      style={atenuada ? { opacity: 0.6, filter: "grayscale(1)" } : undefined}>
      <Stack gap="lg">
        <Group gap="md" wrap="nowrap" align="flex-start">
          <Box w={64} style={{ flexShrink: 0, borderRadius: "var(--mantine-radius-sm)", overflow: "hidden" }}>
            <Poster pelicula={reserva.pelicula} />
          </Box>
          <Stack gap={6}>
            <Group gap="sm">
              <Badge color={COLOR_ESTADO[reserva.estado]} variant="filled" size="md">{etiqueta(reserva.estado)}</Badge>
              <Text size="xs" c="dimmed" className="cine-cifras">Reserva #{reserva.id}</Text>
            </Group>
            <Title order={1} fz="clamp(1.75rem, 1.3rem + 1.6vw, 2.5rem)">{reserva.pelicula.titulo}</Title>
            {AVISO_POR_ESTADO[reserva.estado] && <Text size="sm" c="dimmed">{AVISO_POR_ESTADO[reserva.estado]}</Text>}
          </Stack>
        </Group>

        <SimpleGrid cols={{ base: 2, sm: 4 }} spacing="md">
          <Dato titulo="Día" grande>{dia(reserva.funcion.inicio)}</Dato>
          <Dato titulo="Hora" grande>{hora(reserva.funcion.inicio)}</Dato>
          <Dato titulo="Sala">{sala.nombre} · {etiqueta(sala.tipo)}</Dato>
          <Dato titulo="Formato">{etiqueta(reserva.funcion.proyeccion)} · {etiqueta(reserva.funcion.idioma)}</Dato>
        </SimpleGrid>

        <Box>
          {reserva.entradas.map((e) => <Butaca key={e.codigo} entrada={e} />)}
          <Group justify="space-between" align="baseline" pt="sm">
            <Text fw={650}>Total · {reserva.entradas.length} entrada{reserva.entradas.length === 1 ? "" : "s"}</Text>
            <Text className="cine-display cine-cifras" fz={36} fw={850} lh={1}>{precio(reserva.total)}</Text>
          </Group>
        </Box>
      </Stack>
    </Entrada>
  );
}
