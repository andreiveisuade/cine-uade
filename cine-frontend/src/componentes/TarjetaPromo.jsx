import { useState } from "react";
import { Badge, Box, Collapse, Group, SimpleGrid, Stack, Text, UnstyledButton } from "@mantine/core";
import { Foto } from "./Foto.jsx";
import { etiqueta } from "@/api/etiquetas.js";

export const DIAS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

// La fecha viaja sin hora: armarla con Date la correría un día por la zona horaria.
export function fecha(iso, conAnio = true) {
  const [anio, mes, dia] = iso.split("-");
  return conAnio ? `${dia}/${mes}/${anio}` : `${dia}/${mes}`;
}

function hoyEnLaSemana() {
  return DIAS[(new Date().getDay() + 6) % 7];
}

function hoyIso() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

export const corre = (promo, dia) => promo.diasSemana.length === 0 || promo.diasSemana.includes(dia);

// Solo presentación (el descuento lo decide el cobro): «hoy» es que alguna función de hoy todavía pueda entrar,
// así que con franja ya terminada a esta hora no se anuncia.
function aplicaHoy(promo) {
  const hoy = hoyIso();
  const ahora = new Date().toTimeString().slice(0, 5);
  const franjaAbierta = !promo.horaHasta || ahora < promo.horaHasta.slice(0, 5);
  return promo.vigenciaDesde <= hoy && hoy <= promo.vigenciaHasta && corre(promo, hoyEnLaSemana()) && franjaAbierta;
}

function festejar(evento) {
  const { left, top, width, height } = evento.currentTarget.getBoundingClientRect();
  // Se baja recién cuando alguien toca: casi nadie la usa.
  import("canvas-confetti").then(({ default: lanzar }) => lanzar({
    particleCount: 80, spread: 70, startVelocity: 35, disableForReducedMotion: true,
    origin: { x: (left + width / 2) / window.innerWidth, y: (top + height / 2) / window.innerHeight },
  }));
}

function DiasDeLaSemana({ promo }) {
  const hoy = hoyEnLaSemana();
  return (
    <Group gap={4} wrap="nowrap" aria-label="Días">
      {DIAS.map((dia) => {
        const prendido = corre(promo, dia);
        return (
          <Badge key={dia} size="md" radius="sm" tt="none" w={34} px={0}
            variant={prendido ? "filled" : "default"} color={dia === hoy && prendido ? "ambar" : "dark"}
            style={{ opacity: prendido ? 1 : 0.45 }} aria-label={`${etiqueta(dia)}${prendido ? "" : " (no aplica)"}`}>
            {etiqueta(dia).slice(0, 2)}
          </Badge>
        );
      })}
    </Group>
  );
}

function Condiciones({ promo }) {
  const [abiertas, setAbiertas] = useState(false);
  return (
    <Box>
      <UnstyledButton onClick={() => setAbiertas((a) => !a)} c="dimmed" fz="sm" fw={600} aria-expanded={abiertas}>
        {abiertas ? "Ocultar condiciones ▲" : "Ver condiciones ▼"}
      </UnstyledButton>
      <Collapse expanded={abiertas}>
        <Stack gap={2} pt={6}>
          <Text size="sm" c="dimmed">Del {fecha(promo.vigenciaDesde)} al {fecha(promo.vigenciaHasta)}</Text>
          {promo.horaDesde && <Text size="sm" c="dimmed">Funciones de {promo.horaDesde.slice(0, 5)} a {promo.horaHasta.slice(0, 5)}</Text>}
          <Text size="sm" c="dimmed">
            {promo.mediosPago.length === 0 ? "Cualquier medio de pago" : `Pagando con ${promo.mediosPago.map(etiqueta).join(", ").toLowerCase()}`}
          </Text>
          {promo.tarifasExcluidas.length > 0 && (
            <Text size="sm" c="dimmed">No suma con las tarifas {promo.tarifasExcluidas.map(etiqueta).join(", ").toLowerCase()}</Text>
          )}
        </Stack>
      </Collapse>
    </Box>
  );
}

// Destacada, la foto va al costado del texto; si no, arriba. El beneficio es lo que se lee primero.
export function TarjetaPromo({ promo, destacada = false, conCondiciones = true }) {
  const hoy = aplicaHoy(promo);
  const empiezaDespues = promo.vigenciaDesde > hoyIso();
  return (
    <Box h="100%" style={{ background: "var(--cine-superficie)", border: "1px solid var(--cine-borde)", borderRadius: "var(--mantine-radius-lg)", overflow: "hidden" }}>
      <SimpleGrid cols={{ base: 1, md: destacada && promo.imagenUrl ? 2 : 1 }} spacing={0} h="100%">
        {promo.imagenUrl && (
          <Foto src={promo.imagenUrl} alt={promo.nombre} />
        )}
        <Stack gap="md" p={destacada ? "xl" : "lg"} justify="center">
          <Group justify="space-between" gap="sm" mih={28}>
            <Text size="sm" fw={700} tt="uppercase" c="dimmed" style={{ letterSpacing: "0.04em" }}>{promo.nombre}</Text>
            {hoy && (
              <UnstyledButton onClick={festejar}>
                <Badge color="ambar" size="lg" variant="filled" style={{ cursor: "pointer" }}>¡Hoy aplica!</Badge>
              </UnstyledButton>
            )}
            {empiezaDespues && <Badge color="gray" variant="light">Desde {fecha(promo.vigenciaDesde, false)}</Badge>}
          </Group>
          <Text className="cine-display" fw={850} lh={0.9} c="var(--mantine-color-ambar-text)"
            fz={destacada ? "clamp(3.5rem, 2.4rem + 4vw, 6rem)" : "clamp(2.75rem, 2.2rem + 2vw, 3.75rem)"}>
            {promo.beneficio}
          </Text>
          <DiasDeLaSemana promo={promo} />
          <Group gap={6}>
            {promo.horaDesde && (
              <Badge variant="default" tt="none">{promo.horaDesde.slice(0, 5)}–{promo.horaHasta.slice(0, 5)}</Badge>
            )}
            {promo.mediosPago.map((medio) => (
              <Badge key={medio} variant="default" tt="none">{etiqueta(medio)}</Badge>
            ))}
            <Badge variant="default" tt="none">Hasta {fecha(promo.vigenciaHasta, false)}</Badge>
          </Group>
          {conCondiciones && <Condiciones promo={promo} />}
        </Stack>
      </SimpleGrid>
    </Box>
  );
}
