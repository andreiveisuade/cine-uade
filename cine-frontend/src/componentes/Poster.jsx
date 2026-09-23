import { useState } from "react";
import { AspectRatio, Center, Image, Text } from "@mantine/core";

/**
 * posterUrl puede venir vacío o con una URL que no carga: entonces queda la inicial del
 * título, y la tarjeta conserva su forma en vez de mostrar un ícono de imagen rota.
 * Siempre 2:3, la proporción de un afiche, para que las tarjetas queden parejas.
 */
export function Poster({ pelicula, w = "100%" }) {
  const [rota, setRota] = useState(false);
  return (
    <AspectRatio ratio={2 / 3} w={w} style={{ flexShrink: 0, borderRadius: "var(--mantine-radius-sm)", overflow: "hidden" }}
      bg="var(--mantine-color-default-hover)">
      {pelicula.posterUrl && !rota ? (
        <Image src={pelicula.posterUrl} alt="" loading="lazy" fit="cover" onError={() => setRota(true)} />
      ) : (
        <Center>
          <Text fz={32} fw={700} c="dimmed">{pelicula.titulo.charAt(0).toUpperCase()}</Text>
        </Center>
      )}
    </AspectRatio>
  );
}
