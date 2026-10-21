import { useRef, useState } from "react";
import { Carousel } from "@mantine/carousel";
import "@mantine/carousel/styles.css";
import Autoplay from "embla-carousel-autoplay";
import { Box, Button, Card, Group, Modal, SimpleGrid, Skeleton, Stack, Text, Title } from "@mantine/core";
import { useMediaQuery } from "@mantine/hooks";
import { AnimatePresence, m, useReducedMotion } from "motion/react";
import { Link, useParams } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { duracion } from "../api/formato.js";
import { Chip, ChipClasificacion } from "../componentes/Chips.jsx";
import { EsperaOError, Vacio } from "../componentes/Estado.jsx";
import { Poster } from "../componentes/Poster.jsx";
import { Trailer, urlTrailer } from "../componentes/Trailer.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { useTitulo } from "../componentes/useTitulo.js";

// Portada de la home: una sala a oscuras con las destacadas pasando en la pantalla. Sin películas queda el texto solo.
function Hero({ peliculas }) {
  const destacadas = peliculas.slice(0, 8);
  const quieto = useReducedMotion();
  // Una sola instancia por montaje: el carrusel la arranca y la frena él mismo.
  const [autoplay] = useState(() => Autoplay({ delay: 3500, stopOnInteraction: false, stopOnMouseEnter: true }));
  const [enModal, setEnModal] = useState(null);
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
          <Carousel w="100%" maw={400} slideSize="33.333%" slideGap="md"
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
                {p.trailerYoutube && (
                  <Button size="compact-xs" variant="white" color="dark" radius="xl" mt={6} onClick={() => setEnModal(p)}>
                    ▶ Ver trailer
                  </Button>
                )}
              </Carousel.Slide>
            ))}
          </Carousel>
        )}
      </Group>
      <Modal opened={enModal !== null} onClose={() => setEnModal(null)} size="xl" centered padding={0}
        withCloseButton={false} radius="md" overlayProps={{ backgroundOpacity: 0.8, blur: 4 }}>
        {enModal && <Trailer id={enModal.trailerYoutube} titulo={enModal.titulo} />}
      </Modal>
    </Box>
  );
}

// Con mouse, quedarse sobre una tarjeta muestra el trailer muteado arriba del póster; en touch no hay hover.
const ESPERA_PREVIA = 600;

function usePrevia() {
  const conMouse = useMediaQuery("(hover: hover) and (pointer: fine)");
  const [actual, setActual] = useState(null);
  const espera = useRef(null);
  function entrar(pelicula) {
    if (!conMouse || !pelicula.trailerYoutube) return;
    clearTimeout(espera.current);
    espera.current = setTimeout(() => setActual(pelicula.id), ESPERA_PREVIA);
  }
  function salir() {
    clearTimeout(espera.current);
    setActual(null);
  }
  return { actual, entrar, salir };
}

function Previa({ id }) {
  return (
    <Box pos="absolute" inset={0} style={{ pointerEvents: "none", background: "black", overflow: "hidden" }}>
      {/* Un 16:9 a la altura del póster 2:3 mide 8/3 del ancho: centrado, recorta los costados. */}
      <iframe src={urlTrailer(id, { controles: false })} title="Previa del trailer" tabIndex={-1} aria-hidden
        allow="autoplay; encrypted-media" style={{ border: 0, height: "100%", width: "267%", marginLeft: "-83.5%" }} />
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
  useTitulo(conHero ? undefined : "Cartelera");
  const previa = usePrevia();
  const carga = useCargar(() => Promise.all([api.obtenerCartelera(genero), api.obtenerGeneros()]), [genero]);
  // Mientras llega otro género se queda la lista anterior: las tarjetas se reordenan en vez de pasar por el esqueleto.
  const datos = carga.datos ?? carga.previos;
  if (!datos) return <EsperaOError carga={carga} esqueleto={<CarteleraEsqueleto />} />;
  const [peliculas, generos] = datos;

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
                whileHover={{ y: -4 }} transition={{ layout: { duration: 0.25 }, y: { duration: 0.15 } }}
                onMouseEnter={() => previa.entrar(p)} onMouseLeave={previa.salir}>
                <Card component={Link} to={`/pelicula/${p.id}`} withBorder padding="sm" h="100%">
                  <Card.Section pos="relative">
                    <Poster pelicula={p} />
                    {previa.actual === p.id && <Previa id={p.trailerYoutube} />}
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
