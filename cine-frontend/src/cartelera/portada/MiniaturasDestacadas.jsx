import { Box, Group, UnstyledButton } from "@mantine/core";
import { Poster } from "@/compartido/ui/Poster.jsx";
import { ROTAR_CADA_MS } from "./useRotacion.js";

// La barra de la elegida marca el tiempo hasta la próxima y, al llenarse, avanza: pausarla pausa la rotación.
export function MiniaturasDestacadas({ peliculas, rotacion: { elegida, elegir, rota, corre, avanzar } }) {
  return (
    <Group gap="sm" wrap="nowrap" aria-label="Películas destacadas">
      {peliculas.map((p, i) => (
        <UnstyledButton key={p.id} w={{ base: 56, sm: 76 }} onClick={() => elegir(i)} pos="relative" aria-pressed={i === elegida}
          aria-label={`Destacar ${p.titulo}`}
          style={{ borderRadius: "var(--mantine-radius-sm)", overflow: "hidden", transition: "opacity 200ms, outline-color 200ms",
                   outline: `2px solid ${i === elegida ? "var(--mantine-color-ambar-4)" : "transparent"}`, outlineOffset: 3,
                   opacity: i === elegida ? 1 : 0.55 }}>
          <Poster pelicula={p} />
          {rota && i === elegida && (
            <Box pos="absolute" bottom={0} left={0} right={0} h={3} bg="var(--mantine-color-ambar-4)" onAnimationEnd={avanzar}
              style={{ transformOrigin: "left", animation: `cine-avance ${ROTAR_CADA_MS}ms linear`,
                       animationPlayState: corre ? "running" : "paused" }} />
          )}
        </UnstyledButton>
      ))}
    </Group>
  );
}
