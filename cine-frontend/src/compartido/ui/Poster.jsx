import { useState } from "react";
import { AspectRatio, Center, Image, Text } from "@mantine/core";

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
