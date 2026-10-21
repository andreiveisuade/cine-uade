import { useState } from "react";
import { Box, Button, Group, Modal, Stack, Text, Title, UnstyledButton } from "@mantine/core";
import { AnimatePresence, m } from "motion/react";
import { Link } from "react-router";
import { etiqueta } from "@/api/etiquetas.js";
import { duracion } from "@/api/formato.js";
import { Banda } from "@/compartido/layout/Banda.jsx";
import { ChipClasificacion } from "@/compartido/ui/Chips.jsx";
import { Poster } from "@/compartido/ui/Poster.jsx";
import { Trailer } from "@/componentes/Trailer.jsx";

// Con trailer, un cuadro del video en 16:9; sin él, el póster agrandado y difuminado.
function Fondo({ pelicula }) {
  const [sinMaxima, setSinMaxima] = useState(false);
  const cuadro = pelicula.trailerYoutube
    && `https://i.ytimg.com/vi/${pelicula.trailerYoutube}/${sinMaxima ? "hqdefault" : "maxresdefault"}.jpg`;
  return (
    <m.div key={pelicula.id} aria-hidden initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
      transition={{ duration: 0.6, ease: "easeOut" }} style={{ position: "absolute", inset: 0, zIndex: -1 }}>
      {(cuadro || pelicula.posterUrl) && (
        <img src={cuadro || pelicula.posterUrl} alt="" onError={() => setSinMaxima(true)}
          style={{ width: "100%", height: "100%", objectFit: "cover", objectPosition: "center 30%",
                   filter: cuadro ? "brightness(0.8)" : "blur(24px) brightness(0.6)", transform: cuadro ? undefined : "scale(1.15)" }} />
      )}
      {/* Oscurece del lado del texto y hacia abajo: el título se lee sobre cualquier cuadro. */}
      <Box pos="absolute" inset={0} style={{ background: "linear-gradient(90deg, rgb(11 13 17 / 0.95) 0%, rgb(11 13 17 / 0.7) 45%, rgb(11 13 17 / 0.15) 100%), linear-gradient(0deg, rgb(11 13 17) 0%, transparent 45%)" }} />
    </m.div>
  );
}

// Portada de la home: una de las películas en cartel a toda pantalla; las demás se eligen desde los pósters chicos.
export function Portada({ peliculas }) {
  const destacadas = peliculas.slice(0, 5);
  const [elegida, setElegida] = useState(0);
  const [enModal, setEnModal] = useState(false);
  const pelicula = destacadas[elegida];
  if (!pelicula) return null;

  return (
    <Banda mt="calc(-1 * var(--cine-gutter))" fondo={<AnimatePresence initial={false}><Fondo pelicula={pelicula} /></AnimatePresence>}>
      <Group justify="space-between" align="flex-end" gap="xl" mih="clamp(26rem, 62vh, 40rem)" py="xl" wrap="wrap">
        <Stack gap="md" maw="38rem">
          <Title order={1} fz="clamp(2.75rem, 1.8rem + 4.2vw, 5.5rem)" lh={0.92}>{pelicula.titulo}</Title>
          <Group gap="sm">
            <ChipClasificacion valor={pelicula.clasificacion} />
            <Text size="sm" c="var(--cine-banda-tenue)">
              {[duracion(pelicula.duracionMinutos), ...pelicula.generos.map(etiqueta)].join(" · ")}
            </Text>
          </Group>
          {pelicula.sinopsis && <Text c="var(--cine-banda-tenue)" lineClamp={3} maw="34rem">{pelicula.sinopsis}</Text>}
          <Group gap="sm" mt="xs">
            <Button component={Link} to={`/pelicula/${pelicula.id}`} size="md">Comprar entradas</Button>
            {pelicula.trailerYoutube && (
              <Button size="md" variant="white" color="dark" onClick={() => setEnModal(true)}>Ver trailer</Button>
            )}
          </Group>
        </Stack>
        {destacadas.length > 1 && (
          <Group gap="sm" wrap="nowrap" aria-label="Películas destacadas">
            {destacadas.map((p, i) => (
              <UnstyledButton key={p.id} w={{ base: 56, sm: 76 }} onClick={() => setElegida(i)} aria-pressed={i === elegida}
                aria-label={`Destacar ${p.titulo}`}
                style={{ borderRadius: "var(--mantine-radius-sm)", overflow: "hidden", transition: "opacity 200ms, outline-color 200ms",
                         outline: `2px solid ${i === elegida ? "var(--mantine-color-ambar-4)" : "transparent"}`, outlineOffset: 3,
                         opacity: i === elegida ? 1 : 0.55 }}>
                <Poster pelicula={p} />
              </UnstyledButton>
            ))}
          </Group>
        )}
      </Group>
      <Modal opened={enModal} onClose={() => setEnModal(false)} size="xl" centered padding={0}
        withCloseButton={false} radius="lg" overlayProps={{ backgroundOpacity: 0.85 }}>
        {enModal && pelicula.trailerYoutube && <Trailer id={pelicula.trailerYoutube} titulo={pelicula.titulo} />}
      </Modal>
    </Banda>
  );
}
