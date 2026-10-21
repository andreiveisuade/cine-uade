import { Group, UnstyledButton } from "@mantine/core";
import { Poster } from "@/compartido/ui/Poster.jsx";

export function MiniaturasDestacadas({ peliculas, elegida, alElegir }) {
  return (
    <Group gap="sm" wrap="nowrap" aria-label="Películas destacadas">
      {peliculas.map((p, i) => (
        <UnstyledButton key={p.id} w={{ base: 56, sm: 76 }} onClick={() => alElegir(i)} aria-pressed={i === elegida}
          aria-label={`Destacar ${p.titulo}`}
          style={{ borderRadius: "var(--mantine-radius-sm)", overflow: "hidden", transition: "opacity 200ms, outline-color 200ms",
                   outline: `2px solid ${i === elegida ? "var(--mantine-color-ambar-4)" : "transparent"}`, outlineOffset: 3,
                   opacity: i === elegida ? 1 : 0.55 }}>
          <Poster pelicula={p} />
        </UnstyledButton>
      ))}
    </Group>
  );
}
