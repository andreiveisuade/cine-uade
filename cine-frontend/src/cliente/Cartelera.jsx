import { useState } from "react";
import { Carousel } from "@mantine/carousel";
import "@mantine/carousel/styles.css";
import Autoplay from "embla-carousel-autoplay";
import { Box, Button, Card, Group, SimpleGrid, Skeleton, Stack, Text, Title } from "@mantine/core";
import { AnimatePresence, m, useReducedMotion } from "motion/react";
import { Link, useParams } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { duracion } from "../api/formato.js";
import { Chip, ChipClasificacion } from "../componentes/Chips.jsx";
import { EsperaOError, Vacio } from "../componentes/Estado.jsx";
import { Poster } from "../componentes/Poster.jsx";
import { useCargar } from "../componentes/useCargar.js";

// Portada de la home: una sala a oscuras con las destacadas pasando en la pantalla. Sin películas queda el texto solo.
function Hero({ peliculas }) {
  const destacadas = peliculas.slice(0, 8);
  const quieto = useReducedMotion();
  // Una sola instancia por montaje: el carrusel la arranca y la frena él mismo.
  const [autoplay] = useState(() => Autoplay({ delay: 3500, stopOnInteraction: false, stopOnMouseEnter: true }));
  return (
    <Box p={{ base: "lg", sm: "xl" }} c="white"
      style={{
        borderRadius: "var(--mantine-radius-md)",
        background: "radial-gradient(ellipse at 75% 0%, var(--mantine-color-indigo-9), var(--mantine-color-dark-8) 55%, var(--mantine-color-dark-9))",
      }}>
      <Group justify="space-between" align="center" gap="xl" wrap="wrap">
        <Stack gap="sm" maw={420}>
          <Title order={1} fz={{ base: 32, sm: 44 }} lh={1.1}>CINE UADE</Title>
          <Text size="lg" c="gray.4">Tu cine de barrio: salas 2D y 3D, candy y entradas online.</Text>
          <div>
            <Button size="md"
              onClick={() => document.getElementById("cartelera")?.scrollIntoView({ behavior: "smooth" })}>
              Ver cartelera
            </Button>
          </div>
        </Stack>
        {destacadas.length > 0 && (
          <Carousel w={{ base: "100%", md: 460 }} slideSize={{ base: "40%", sm: "33.333%" }} slideGap="md"
            emblaOptions={{ loop: destacadas.length > 3, align: "start" }} plugins={quieto ? [] : [autoplay]}
            controlSize={28} withControls={destacadas.length > 3}>
            {destacadas.map((p) => (
              <Carousel.Slide key={p.id}>
                <Box component={Link} to={`/pelicula/${p.id}`} display="block" c="white"
                  style={{ textDecoration: "none" }}>
                  {/* El brillo es la luz del proyector rebotando en el póster. */}
                  <Box style={{ borderRadius: "var(--mantine-radius-sm)", boxShadow: "0 8px 28px -6px var(--mantine-color-indigo-5)" }}>
                    <Poster pelicula={p} />
                  </Box>
                  <Text size="xs" fw={600} mt={6} lh={1.2} lineClamp={2}>{p.titulo}</Text>
                </Box>
              </Carousel.Slide>
            ))}
          </Carousel>
        )}
      </Group>
    </Box>
  );
}

// Entran escalonadas por posición; al filtrar, las que quedan se corren a su lugar nuevo con `layout`.
const TARJETA = {
  oculta: { opacity: 0, y: 12 },
  visible: (orden) => ({ opacity: 1, y: 0, transition: { duration: 0.25, delay: Math.min(orden, 8) * 0.04 } }),
  saliente: { opacity: 0, scale: 0.95, transition: { duration: 0.15 } },
};

function CarteleraEsqueleto() {
  return (
    <Stack gap="lg">
      <Skeleton h={28} w={180} />
      <SimpleGrid cols={{ base: 2, sm: 3, md: 4 }} spacing="md">
        {Array.from({ length: 8 }, (_, i) => <Skeleton key={i} style={{ aspectRatio: "2 / 3" }} />)}
      </SimpleGrid>
    </Stack>
  );
}

export function Cartelera({ conHero = false }) {
  const { genero } = useParams();
  const carga = useCargar(() => Promise.all([api.obtenerCartelera(genero), api.obtenerGeneros()]), [genero]);
  if (!carga.datos) return <EsperaOError carga={carga} esqueleto={<CarteleraEsqueleto />} />;
  const [peliculas, generos] = carga.datos;

  return (
    <Stack gap="lg">
      {conHero && <Hero peliculas={peliculas} />}
      <div id="cartelera">
        {conHero ? <Title order={2}>Cartelera</Title> : <Title order={1}>Cartelera</Title>}
        <Text c="dimmed" size="sm">
          {peliculas.length} película{peliculas.length === 1 ? "" : "s"}{" "}
          {genero ? `de ${etiqueta(genero).toLowerCase()}` : "en cartel"}
        </Text>
      </div>

      <Group gap={6}>
        <Button component={Link} to="/cartelera" size="compact-sm" radius="xl" variant={genero ? "default" : "filled"}>
          Todos
        </Button>
        {generos.map((g) => (
          <Button key={g} component={Link} to={`/cartelera/${g}`} size="compact-sm" radius="xl"
            variant={g === genero ? "filled" : "default"}>
            {etiqueta(g)}
          </Button>
        ))}
      </Group>

      {peliculas.length ? (
        <SimpleGrid cols={{ base: 2, sm: 3, md: 4 }} spacing="md">
          <AnimatePresence mode="popLayout" initial={false}>
            {peliculas.map((p, orden) => (
              <m.div key={p.id} layout custom={orden} variants={TARJETA} initial="oculta" animate="visible" exit="saliente"
                whileHover={{ y: -4 }} transition={{ layout: { duration: 0.25 }, y: { duration: 0.15 } }}>
                <Card component={Link} to={`/pelicula/${p.id}`} withBorder padding="sm" h="100%">
                  <Card.Section>
                    <Poster pelicula={p} />
                  </Card.Section>
                  <Text fw={600} mt="sm" lh={1.2}>{p.titulo}</Text>
                  <Group gap={6} mt={6}>
                    <ChipClasificacion valor={p.clasificacion} />
                    <Text size="xs" c="dimmed">{duracion(p.duracionMinutos)}</Text>
                  </Group>
                  <Group gap={4} mt={6}>
                    {p.generos.map((g) => <Chip key={g} valor={g} />)}
                  </Group>
                </Card>
              </m.div>
            ))}
          </AnimatePresence>
        </SimpleGrid>
      ) : (
        <Vacio>No hay películas de ese género en cartelera.</Vacio>
      )}
    </Stack>
  );
}
