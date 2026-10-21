import { useState } from "react";
import { Badge, Box, Card, Chip, Group, SimpleGrid, Skeleton, Stack, Text, Title } from "@mantine/core";
import { AnimatePresence, m } from "motion/react";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { precio } from "../api/formato.js";
import { EsperaOError } from "../componentes/Estado.jsx";
import { MontoAnimado } from "../componentes/MontoAnimado.jsx";
import { useCargar } from "../componentes/useCargar.js";

const DIAS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

const SALA = "radial-gradient(ellipse at 75% 0%, var(--mantine-color-indigo-9), var(--mantine-color-dark-8) 55%, var(--mantine-color-dark-9))";

// Mismo ritmo que la cartelera: escalonadas al entrar, y al filtrar las que quedan se corren con `layout`.
const TARJETA = {
  oculta: { opacity: 0, y: 16 },
  visible: (orden) => ({ opacity: 1, y: 0, transition: { duration: 0.3, delay: Math.min(orden, 8) * 0.06 } }),
  saliente: { opacity: 0, scale: 0.95, transition: { duration: 0.15 } },
};
const ELEVAR = { y: -4, transition: { duration: 0.15 } };

// La fecha viaja sin hora: armarla con Date la correría un día por la zona horaria.
function fecha(iso) {
  const [anio, mes, dia] = iso.split("-");
  return `${dia}/${mes}/${anio}`;
}

function hoyEnLaSemana() {
  return DIAS[(new Date().getDay() + 6) % 7];
}

function hoyIso() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

// Solo presentación: si el descuento entra en una compra lo decide el backend al cobrar.
function aplicaHoy(promo) {
  const hoy = hoyIso();
  const enVigencia = promo.vigenciaDesde <= hoy && hoy <= promo.vigenciaHasta;
  return enVigencia && (promo.diasSemana.length === 0 || promo.diasSemana.includes(hoyEnLaSemana()));
}

const corre = (promo, dia) => promo.diasSemana.length === 0 || promo.diasSemana.includes(dia);

function Encabezado() {
  return (
    <Box p={{ base: "lg", sm: "xl" }} c="white" style={{ borderRadius: "var(--mantine-radius-md)", background: SALA }}>
      <Stack gap="xs" maw={560}>
        <Title order={1} fz={{ base: 34, sm: 48 }} lh={1.05}>Promos y candy</Title>
        <Text size="lg" c="gray.4">
          Los descuentos de la semana y los combos de la carta. Las promos no se acumulan: al pagar se aplica la
          que más te conviene.
        </Text>
      </Stack>
    </Box>
  );
}

function DiasDeLaSemana({ promo }) {
  const hoy = hoyEnLaSemana();
  return (
    <Group gap={6} aria-label="Días">
      {DIAS.map((dia) => {
        const prendido = corre(promo, dia);
        return (
          <Badge key={dia} size="lg" radius="sm" tt="none" w={44} px={0}
            variant={prendido ? "filled" : "outline"} color={prendido ? "indigo" : "gray"}
            style={{ opacity: prendido ? 1 : 0.45, outline: dia === hoy ? "2px solid var(--mantine-color-yellow-5)" : undefined }}
            aria-label={`${etiqueta(dia)}${prendido ? "" : " (no aplica)"}`}>
            {etiqueta(dia).slice(0, 3)}
          </Badge>
        );
      })}
    </Group>
  );
}

function TarjetaPromo({ promo, orden }) {
  const hoy = aplicaHoy(promo);
  const empiezaDespues = promo.vigenciaDesde > hoyIso();
  return (
    <m.div layout variants={TARJETA} custom={orden} initial="oculta" animate="visible" exit="saliente" whileHover={ELEVAR}>
      <Card radius="md" padding="lg" h="100%" withBorder
        style={{ background: SALA, color: "white", boxShadow: hoy ? "0 10px 32px -10px var(--mantine-color-yellow-5)" : undefined }}>
        <Stack gap="md" h="100%">
          {/* El badge va arriba y no al costado: en celular el beneficio ocupa todo el ancho. */}
          {(hoy || empiezaDespues) && (
            <Group>
              {hoy && <Badge color="yellow" c="dark.9" size="lg" variant="filled">¡Hoy aplica!</Badge>}
              {empiezaDespues && <Badge color="gray" variant="light">Desde el {fecha(promo.vigenciaDesde)}</Badge>}
            </Group>
          )}
          <Text fw={900} lh={0.95} fz={{ base: 44, sm: 64 }} c="yellow.4" style={{ letterSpacing: "-0.03em", whiteSpace: "nowrap" }}>
            {promo.beneficio}
          </Text>
          <Title order={3} c="white">{promo.nombre}</Title>
          <DiasDeLaSemana promo={promo} />
          <Stack gap={4}>
            {promo.horaDesde && (
              <Text size="sm" c="gray.3">Funciones de {promo.horaDesde.slice(0, 5)} a {promo.horaHasta.slice(0, 5)}</Text>
            )}
            <Group gap={6}>
              <Text size="sm" c="gray.3">
                {promo.mediosPago.length === 0 ? "Con cualquier medio de pago" : "Pagando con"}
              </Text>
              {promo.mediosPago.map((medio) => (
                <Badge key={medio} variant="light" color="indigo" tt="none">{etiqueta(medio)}</Badge>
              ))}
            </Group>
            <Text size="sm" c="gray.3">Del {fecha(promo.vigenciaDesde)} al {fecha(promo.vigenciaHasta)}</Text>
          </Stack>
          {promo.tarifasExcluidas.length > 0 && (
            <Text size="xs" c="gray.5" mt="auto">
              No suma con las tarifas {promo.tarifasExcluidas.map(etiqueta).join(", ").toLowerCase()}
            </Text>
          )}
        </Stack>
      </Card>
    </m.div>
  );
}

function Promos({ promos }) {
  const [dia, setDia] = useState("TODOS");
  const visibles = dia === "TODOS" ? promos : promos.filter((p) => corre(p, dia));

  if (promos.length === 0) {
    return (
      <Card radius="md" padding="xl" withBorder ta="center">
        <Title order={3}>Por ahora no hay promos</Title>
        <Text c="dimmed" mt={4}>Volvé pronto: cuando el cine cargue una, aparece acá. Mientras, mirá los combos.</Text>
      </Card>
    );
  }
  return (
    <Stack gap="md">
      <Chip.Group value={dia} onChange={setDia}>
        <Group gap={6} aria-label="Filtrar por día">
          <Chip value="TODOS" size="sm">Todos</Chip>
          {DIAS.map((d) => <Chip key={d} value={d} size="sm">{etiqueta(d)}</Chip>)}
        </Group>
      </Chip.Group>
      <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="lg">
        <AnimatePresence mode="popLayout">
          {visibles.map((promo, i) => <TarjetaPromo key={promo.nombre} promo={promo} orden={i} />)}
        </AnimatePresence>
      </SimpleGrid>
      {visibles.length === 0 && <Text c="dimmed" ta="center">Ese día no hay promos.</Text>}
    </Stack>
  );
}

function TarjetaCombo({ combo, orden }) {
  return (
    <m.div variants={TARJETA} custom={orden} initial="oculta" animate="visible" whileHover={ELEVAR}>
      <Card radius="md" padding="lg" withBorder h="100%">
        <Stack gap="sm" h="100%">
          <Group justify="space-between" align="flex-start" gap="xs">
            <Title order={3}>{combo.nombre}</Title>
            <Badge color="green" size="lg" variant="filled" tt="none">
              Ahorrás <MontoAnimado valor={combo.ahorro} />
            </Badge>
          </Group>
          <Stack gap={2}>
            {combo.componentes.map((c) => (
              <Text key={c.productoId} size="sm">{c.cantidad} × {c.nombre}</Text>
            ))}
          </Stack>
          <Group gap="xs" align="baseline" mt="auto">
            <Text fw={800} fz={28}>{precio(combo.precio)}</Text>
            <Text c="dimmed" td="line-through" size="sm">{precio(combo.precioSuelto)}</Text>
          </Group>
        </Stack>
      </Card>
    </m.div>
  );
}

function Candy({ productos }) {
  const combos = productos.filter((p) => p.esCombo);
  const sueltos = productos.filter((p) => !p.esCombo);
  if (productos.length === 0) return <Text c="dimmed">La carta del candy está vacía por ahora.</Text>;
  return (
    <Stack gap="lg">
      {combos.length > 0 && (
        <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }} spacing="lg">
          {combos.map((c, i) => <TarjetaCombo key={c.id} combo={c} orden={i} />)}
        </SimpleGrid>
      )}
      {sueltos.length > 0 && (
        <SimpleGrid cols={{ base: 2, sm: 3, md: 4 }} spacing="sm">
          {sueltos.map((p, i) => (
            <m.div key={p.id} variants={TARJETA} custom={i} initial="oculta" animate="visible" whileHover={ELEVAR}>
              <Card radius="md" padding="md" withBorder h="100%">
                <Text size="xs" c="dimmed" tt="uppercase" fw={700}>{etiqueta(p.tipo)}</Text>
                <Text fw={600} mt={2}>{p.nombre}</Text>
                <Text fw={800} mt="auto">{precio(p.precio)}</Text>
              </Card>
            </m.div>
          ))}
        </SimpleGrid>
      )}
    </Stack>
  );
}

function Esqueleto({ alto, cantidad, cols }) {
  return (
    <SimpleGrid cols={cols} spacing="lg">
      {Array.from({ length: cantidad }, (_, i) => <Skeleton key={i} h={alto} radius="md" />)}
    </SimpleGrid>
  );
}

export function Promociones() {
  const promos = useCargar(() => api.obtenerPromocionesVigentes(), []);
  const carta = useCargar(() => api.obtenerProductosCandy(), []);

  return (
    <Stack gap="xl">
      <Encabezado />
      <Stack gap="md">
        <Title order={2}>Promos de entradas</Title>
        {promos.datos
          ? <Promos promos={promos.datos} />
          : <EsperaOError carga={promos} esqueleto={<Esqueleto alto={260} cantidad={2} cols={{ base: 1, sm: 2 }} />} />}
      </Stack>
      <Stack gap="md">
        <Title order={2}>Candy</Title>
        {carta.datos
          ? <Candy productos={carta.datos} />
          : <EsperaOError carga={carta} esqueleto={<Esqueleto alto={160} cantidad={3} cols={{ base: 1, sm: 3 }} />} />}
      </Stack>
    </Stack>
  );
}
