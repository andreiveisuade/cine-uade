import { useState } from "react";
import { Button, Group, Modal, Stack, Text, Title } from "@mantine/core";
import { AnimatePresence, m } from "motion/react";
import { Link } from "react-router";
import { etiqueta } from "@/api/etiquetas.js";
import { duracion } from "@/api/formato.js";
import { Banda } from "@/compartido/layout/Banda.jsx";
import { ChipClasificacion } from "@/compartido/ui/Chips.jsx";
import { Trailer } from "@/cartelera/Trailer.jsx";
import { FondoPortada } from "./FondoPortada.jsx";
import { MiniaturasDestacadas } from "./MiniaturasDestacadas.jsx";
import { useRotacion } from "./useRotacion.js";

// Portada de la home: una de las películas en cartel a toda pantalla; las demás se eligen desde los pósters chicos.
export function Portada({ peliculas }) {
  const destacadas = peliculas.slice(0, 5);
  const [enModal, setEnModal] = useState(false);
  const rotacion = useRotacion(destacadas.length, enModal);
  const pelicula = destacadas[rotacion.elegida];
  if (!pelicula) return null;

  return (
    <Banda mt="calc(-1 * var(--cine-gutter))" fondo={<AnimatePresence initial={false}><FondoPortada pelicula={pelicula} /></AnimatePresence>}>
      <Group justify="space-between" align="flex-end" gap="xl" mih="clamp(26rem, 62vh, 40rem)" py="xl" wrap="wrap"
        {...rotacion.alrededor}>
        <AnimatePresence mode="wait" initial={false}>
          <m.div key={pelicula.id} initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.3, ease: "easeOut" }}>
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
          </m.div>
        </AnimatePresence>
        {destacadas.length > 1 && <MiniaturasDestacadas peliculas={destacadas} rotacion={rotacion} />}
      </Group>
      <Modal opened={enModal} onClose={() => setEnModal(false)} size="xl" centered padding={0}
        withCloseButton={false} radius="lg" overlayProps={{ backgroundOpacity: 0.85 }}>
        {enModal && pelicula.trailerYoutube && <Trailer id={pelicula.trailerYoutube} titulo={pelicula.titulo} />}
      </Modal>
    </Banda>
  );
}
