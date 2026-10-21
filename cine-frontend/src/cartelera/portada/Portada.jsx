import { useState } from "react";
import { Button, Group, Modal, Stack, Text, Title } from "@mantine/core";
import { AnimatePresence } from "motion/react";
import { Link } from "react-router";
import { etiqueta } from "@/api/etiquetas.js";
import { duracion } from "@/api/formato.js";
import { Banda } from "@/compartido/layout/Banda.jsx";
import { ChipClasificacion } from "@/compartido/ui/Chips.jsx";
import { Trailer } from "@/cartelera/Trailer.jsx";
import { FondoPortada } from "./FondoPortada.jsx";
import { MiniaturasDestacadas } from "./MiniaturasDestacadas.jsx";

// Portada de la home: una de las películas en cartel a toda pantalla; las demás se eligen desde los pósters chicos.
export function Portada({ peliculas }) {
  const destacadas = peliculas.slice(0, 5);
  const [elegida, setElegida] = useState(0);
  const [enModal, setEnModal] = useState(false);
  const pelicula = destacadas[elegida];
  if (!pelicula) return null;

  return (
    <Banda mt="calc(-1 * var(--cine-gutter))" fondo={<AnimatePresence initial={false}><FondoPortada pelicula={pelicula} /></AnimatePresence>}>
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
        {destacadas.length > 1 && <MiniaturasDestacadas peliculas={destacadas} elegida={elegida} alElegir={setElegida} />}
      </Group>
      <Modal opened={enModal} onClose={() => setEnModal(false)} size="xl" centered padding={0}
        withCloseButton={false} radius="lg" overlayProps={{ backgroundOpacity: 0.85 }}>
        {enModal && pelicula.trailerYoutube && <Trailer id={pelicula.trailerYoutube} titulo={pelicula.titulo} />}
      </Modal>
    </Banda>
  );
}
