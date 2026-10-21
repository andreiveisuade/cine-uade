import { useState } from "react";
import { Carousel } from "@mantine/carousel";
import "@mantine/carousel/styles.css";
import Autoplay from "embla-carousel-autoplay";
import { Badge, Box, Card, Chip, Collapse, Group, SimpleGrid, Skeleton, Stack, Text, Title, UnstyledButton } from "@mantine/core";
import { AnimatePresence, m, useMotionValue, useReducedMotion, useScroll, useSpring, useTransform } from "motion/react";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { precio } from "../api/formato.js";
import { EsperaOError } from "../componentes/Estado.jsx";
import { MontoAnimado } from "../componentes/MontoAnimado.jsx";
import { useCargar } from "../componentes/useCargar.js";

const DIAS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

const SALA = "radial-gradient(ellipse at 75% 0%, var(--mantine-color-indigo-9), var(--mantine-color-dark-8) 55%, var(--mantine-color-dark-9))";

// Entran con resorte y escalonadas; al filtrar, las que quedan se corren con `layout`.
const TARJETA = {
  oculta: { opacity: 0, y: 40, scale: 0.92 },
  visible: (orden) => ({
    opacity: 1, y: 0, scale: 1,
    transition: { type: "spring", stiffness: 260, damping: 20, delay: Math.min(orden, 8) * 0.08 },
  }),
  saliente: { opacity: 0, scale: 0.9, transition: { duration: 0.15 } },
};

// La fecha viaja sin hora: armarla con Date la correría un día por la zona horaria.
function fecha(iso, conAnio = true) {
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

const corre = (promo, dia) => promo.diasSemana.length === 0 || promo.diasSemana.includes(dia);

// Solo presentación: si el descuento entra en una compra lo decide el backend al cobrar.
function aplicaHoy(promo) {
  const hoy = hoyIso();
  return promo.vigenciaDesde <= hoy && hoy <= promo.vigenciaHasta && corre(promo, hoyEnLaSemana());
}

function festejar(evento) {
  const { left, top, width, height } = evento.currentTarget.getBoundingClientRect();
  // Se baja recién cuando alguien toca: casi nadie la usa.
  import("canvas-confetti").then(({ default: lanzar }) => lanzar({
    particleCount: 80, spread: 70, startVelocity: 35, disableForReducedMotion: true,
    origin: { x: (left + width / 2) / window.innerWidth, y: (top + height / 2) / window.innerHeight },
  }));
}

function Encabezado() {
  const quieto = useReducedMotion();
  const { scrollY } = useScroll();
  const fondo = useTransform(scrollY, [0, 600], [0, quieto ? 0 : 120]);
  return (
    <Box pos="relative" p={{ base: "lg", sm: "xl" }} c="white" style={{ borderRadius: "var(--mantine-radius-md)", overflow: "hidden" }}>
      <m.div aria-hidden style={{ position: "absolute", inset: "-30% 0 0 0", background: SALA, y: fondo }} />
      <Stack gap={4} pos="relative">
        <Title order={1} fz={{ base: 40, sm: 60 }} lh={1}>Promos</Title>
        <Text c="gray.4">No se acumulan: va la que más te conviene.</Text>
      </Stack>
    </Box>
  );
}

// El beneficio es el protagonista: gradiente que corre por las letras.
function Beneficio({ texto }) {
  const quieto = useReducedMotion();
  return (
    <m.span
      style={{
        display: "inline-block", whiteSpace: "nowrap", fontWeight: 900, lineHeight: 0.95, letterSpacing: "-0.04em",
        backgroundImage: "linear-gradient(100deg, var(--mantine-color-yellow-4) 20%, #fff 40%, var(--mantine-color-orange-4) 60%, var(--mantine-color-yellow-4) 80%)",
        backgroundSize: "250% 100%", WebkitBackgroundClip: "text", backgroundClip: "text", color: "transparent",
      }}
      animate={quieto ? undefined : { backgroundPosition: ["100% 0%", "-150% 0%"] }}
      transition={{ duration: 4, repeat: Infinity, ease: "linear" }}>
      <Text span inherit fz={{ base: 46, xs: 64, sm: 88 }}>{texto}</Text>
    </m.span>
  );
}

function Barrido() {
  const quieto = useReducedMotion();
  if (quieto) return null;
  return (
    <m.div aria-hidden
      style={{
        position: "absolute", inset: 0, pointerEvents: "none",
        background: "linear-gradient(105deg, transparent 40%, rgba(255,255,255,0.12) 50%, transparent 60%)",
      }}
      initial={{ x: "-100%" }} animate={{ x: "100%" }}
      transition={{ duration: 1.4, repeat: Infinity, repeatDelay: 3.5, ease: "easeInOut" }} />
  );
}

function DiasDeLaSemana({ promo }) {
  const hoy = hoyEnLaSemana();
  const quieto = useReducedMotion();
  return (
    <Group gap={4} wrap="nowrap" aria-label="Días">
      {DIAS.map((dia) => {
        const prendido = corre(promo, dia);
        const esHoy = dia === hoy && prendido;
        return (
          <m.div key={dia} animate={esHoy && !quieto ? { scale: [1, 1.18, 1] } : undefined}
            transition={{ duration: 1.1, repeat: Infinity, ease: "easeInOut" }}>
            <Badge size="md" radius="sm" tt="none" w={{ base: 30, xs: 38 }} px={0}
              variant={prendido ? "filled" : "outline"} color={esHoy ? "yellow" : prendido ? "indigo" : "gray"}
              c={esHoy ? "dark.9" : undefined} style={{ opacity: prendido ? 1 : 0.4 }}
              aria-label={`${etiqueta(dia)}${prendido ? "" : " (no aplica)"}`}>
              {etiqueta(dia).slice(0, 2)}
            </Badge>
          </m.div>
        );
      })}
    </Group>
  );
}

function Condiciones({ promo }) {
  const [abiertas, setAbiertas] = useState(false);
  return (
    <Box>
      <UnstyledButton onClick={() => setAbiertas((a) => !a)} c="gray.4" fz="xs" fw={600} aria-expanded={abiertas}>
        {abiertas ? "Ocultar condiciones ▲" : "Ver condiciones ▼"}
      </UnstyledButton>
      <Collapse expanded={abiertas}>
        <Stack gap={2} pt={6}>
          <Text size="xs" c="gray.3">Del {fecha(promo.vigenciaDesde)} al {fecha(promo.vigenciaHasta)}</Text>
          {promo.horaDesde && <Text size="xs" c="gray.3">Funciones de {promo.horaDesde.slice(0, 5)} a {promo.horaHasta.slice(0, 5)}</Text>}
          <Text size="xs" c="gray.3">
            {promo.mediosPago.length === 0 ? "Cualquier medio de pago" : `Pagando con ${promo.mediosPago.map(etiqueta).join(", ").toLowerCase()}`}
          </Text>
          {promo.tarifasExcluidas.length > 0 && (
            <Text size="xs" c="gray.3">No suma con las tarifas {promo.tarifasExcluidas.map(etiqueta).join(", ").toLowerCase()}</Text>
          )}
        </Stack>
      </Collapse>
    </Box>
  );
}

function TarjetaPromo({ promo, orden }) {
  const quieto = useReducedMotion();
  const hoy = aplicaHoy(promo);
  const empiezaDespues = promo.vigenciaDesde > hoyIso();
  // Tilt leve siguiendo al puntero, con resorte para que no tiemble.
  const px = useMotionValue(0);
  const py = useMotionValue(0);
  const rotateX = useSpring(useTransform(py, [-0.5, 0.5], [6, -6]), { stiffness: 200, damping: 18 });
  const rotateY = useSpring(useTransform(px, [-0.5, 0.5], [-8, 8]), { stiffness: 200, damping: 18 });

  function seguir(e) {
    if (quieto) return;
    const r = e.currentTarget.getBoundingClientRect();
    px.set((e.clientX - r.left) / r.width - 0.5);
    py.set((e.clientY - r.top) / r.height - 0.5);
  }

  return (
    <m.div layout variants={TARJETA} custom={orden} initial="oculta" animate="visible" exit="saliente"
      style={{ perspective: 900 }}>
      {/* Flota con un período distinto por tarjeta: así no respiran todas a la vez. */}
      <m.div animate={quieto ? undefined : { y: [0, -8, 0] }}
        transition={{ duration: 4 + (orden % 3) * 0.7, repeat: Infinity, ease: "easeInOut" }}>
        <m.div onPointerMove={seguir} onPointerLeave={() => { px.set(0); py.set(0); }}
          whileHover={{ scale: 1.02 }} style={{ rotateX, rotateY, transformStyle: "preserve-3d" }}>
          <Card radius="lg" padding="lg" pos="relative"
            style={{
              background: SALA, color: "white", overflow: "hidden",
              boxShadow: hoy ? "0 16px 48px -14px var(--mantine-color-yellow-5)" : "0 12px 36px -18px var(--mantine-color-indigo-5)",
            }}>
            <Barrido />
            <Stack gap="sm" pos="relative">
              <Group justify="space-between" mih={30}>
                <Text size="sm" fw={700} c="gray.3" tt="uppercase">{promo.nombre}</Text>
                {hoy && (
                  <m.div animate={quieto ? undefined : { scale: [1, 1.08, 1] }}
                    transition={{ duration: 1.2, repeat: Infinity }}>
                    <UnstyledButton onClick={festejar}>
                      <Badge color="yellow" c="dark.9" size="lg" variant="filled" style={{ cursor: "pointer" }}>¡Hoy aplica!</Badge>
                    </UnstyledButton>
                  </m.div>
                )}
                {empiezaDespues && <Badge color="gray" variant="light">Desde {fecha(promo.vigenciaDesde, false)}</Badge>}
              </Group>
              <Beneficio texto={promo.beneficio} />
              <DiasDeLaSemana promo={promo} />
              <Group gap={6}>
                {promo.horaDesde && (
                  <Badge variant="light" color="grape" tt="none">{promo.horaDesde.slice(0, 5)}–{promo.horaHasta.slice(0, 5)}</Badge>
                )}
                {promo.mediosPago.map((medio) => (
                  <Badge key={medio} variant="light" color="cyan" tt="none">{etiqueta(medio)}</Badge>
                ))}
                <Badge variant="light" color="gray" tt="none">Hasta {fecha(promo.vigenciaHasta, false)}</Badge>
              </Group>
              <Condiciones promo={promo} />
            </Stack>
          </Card>
        </m.div>
      </m.div>
    </m.div>
  );
}

function Promos({ promos }) {
  const [dia, setDia] = useState("TODOS");
  const visibles = dia === "TODOS" ? promos : promos.filter((p) => corre(p, dia));

  if (promos.length === 0) {
    return (
      <Card radius="lg" padding="xl" ta="center" style={{ background: SALA, color: "white" }}>
        <Title order={3}>Por ahora no hay promos</Title>
        <Text c="gray.4" mt={4}>Volvé pronto. Mientras, mirá los combos.</Text>
      </Card>
    );
  }
  return (
    <Stack gap="md">
      <Chip.Group value={dia} onChange={setDia}>
        <Group gap={6} aria-label="Filtrar por día">
          <Chip value="TODOS" size="sm">Todos</Chip>
          {DIAS.map((d) => <Chip key={d} value={d} size="sm">{etiqueta(d).slice(0, 3)}</Chip>)}
        </Group>
      </Chip.Group>
      <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="xl">
        <AnimatePresence mode="popLayout">
          {visibles.map((promo, i) => <TarjetaPromo key={promo.nombre} promo={promo} orden={i} />)}
        </AnimatePresence>
      </SimpleGrid>
      {visibles.length === 0 && <Text c="dimmed" ta="center">Ese día no hay promos.</Text>}
    </Stack>
  );
}

function TarjetaCombo({ combo }) {
  return (
    <m.div whileHover={{ y: -6, scale: 1.02 }} style={{ height: "100%" }}>
      <Card radius="lg" padding="lg" withBorder h="100%">
        <Stack gap="xs" h="100%">
          <Badge color="green" size="xl" variant="filled" tt="none" style={{ alignSelf: "flex-start" }}>
            Ahorrás <MontoAnimado valor={combo.ahorro} desde={0} />
          </Badge>
          <Title order={3}>{combo.nombre}</Title>
          <Group gap={4}>
            {combo.componentes.map((c) => (
              <Badge key={c.productoId} variant="light" color="gray" tt="none">{c.cantidad} × {c.nombre}</Badge>
            ))}
          </Group>
          <Group gap="xs" align="baseline" mt="auto">
            <Text fw={900} fz={32}>{precio(combo.precio)}</Text>
            <Text c="dimmed" td="line-through" size="sm">{precio(combo.precioSuelto)}</Text>
          </Group>
        </Stack>
      </Card>
    </m.div>
  );
}

function Candy({ productos }) {
  const quieto = useReducedMotion();
  // Una sola instancia por montaje: el carrusel la arranca y la frena él mismo.
  const [autoplay] = useState(() => Autoplay({ delay: 2800, stopOnInteraction: false, stopOnMouseEnter: true }));
  const combos = productos.filter((p) => p.esCombo);
  const sueltos = productos.filter((p) => !p.esCombo);
  if (productos.length === 0) return <Text c="dimmed">La carta del candy está vacía por ahora.</Text>;
  return (
    <Stack gap="lg">
      {combos.length > 0 && (
        <Carousel slideSize={{ base: "85%", sm: "50%", md: "33.333%" }} slideGap="lg" withIndicators={combos.length > 1}
          emblaOptions={{ loop: combos.length > 2, align: "start" }} plugins={quieto ? [] : [autoplay]}
          withControls={combos.length > 1}>
          {combos.map((c) => <Carousel.Slide key={c.id} py="sm"><TarjetaCombo combo={c} /></Carousel.Slide>)}
        </Carousel>
      )}
      {sueltos.length > 0 && (
        <SimpleGrid cols={{ base: 2, sm: 3, md: 5 }} spacing="sm">
          {sueltos.map((p, i) => (
            <m.div key={p.id} variants={TARJETA} custom={i} initial="oculta" animate="visible" whileHover={{ y: -4 }}>
              <Card radius="md" padding="sm" withBorder h="100%">
                <Text size="xs" c="dimmed" tt="uppercase" fw={700}>{etiqueta(p.tipo)}</Text>
                <Text fw={600} size="sm" lineClamp={1}>{p.nombre}</Text>
                <Text fw={900} fz="lg">{precio(p.precio)}</Text>
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
      {Array.from({ length: cantidad }, (_, i) => <Skeleton key={i} h={alto} radius="lg" />)}
    </SimpleGrid>
  );
}

export function Promociones() {
  const promos = useCargar(() => api.obtenerPromocionesVigentes(), []);
  const carta = useCargar(() => api.obtenerProductosCandy(), []);

  return (
    <Stack gap="xl">
      <Encabezado />
      {promos.datos
        ? <Promos promos={promos.datos} />
        : <EsperaOError carga={promos} esqueleto={<Esqueleto alto={300} cantidad={2} cols={{ base: 1, sm: 2 }} />} />}
      <Stack gap="md">
        <Title order={2} fz={{ base: 28, sm: 36 }}>Candy</Title>
        {carta.datos
          ? <Candy productos={carta.datos} />
          : <EsperaOError carga={carta} esqueleto={<Esqueleto alto={180} cantidad={3} cols={{ base: 1, sm: 3 }} />} />}
      </Stack>
    </Stack>
  );
}
