import { Box } from "@mantine/core";
import { m } from "motion/react";
import { Banda } from "@/compartido/layout/Banda.jsx";
import { Volver } from "@/compartido/layout/Volver.jsx";
import { Trailer } from "@/cartelera/Trailer.jsx";
import { useVolarPoster } from "@/cartelera/useVolarPoster.js";

// La sala a oscuras con el trailer en pantalla. Sin trailer, el póster difuminado ocupa su lugar.
export function PantallaTrailer({ pelicula }) {
  const volar = useVolarPoster();
  const fondo = pelicula.posterUrl && (
    <img src={pelicula.posterUrl} alt="" data-testid="fondo-poster"
      style={{ position: "absolute", inset: 0, width: "100%", height: "100%", objectFit: "cover", zIndex: -1,
               filter: "blur(28px) brightness(0.45)", transform: "scale(1.2)" }} />
  );
  return (
    <Banda mt="calc(-1 * var(--cine-gutter))" fondo={fondo}>
      <Box py={{ base: "md", sm: "xl" }} mih={pelicula.trailerYoutube ? undefined : { base: 160, sm: 280 }}>
        <Volver a="/" onClick={volar(pelicula.id, "/")}>Cartelera</Volver>
        {pelicula.trailerYoutube && (
          <m.div initial={{ opacity: 0, scale: 0.98 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.5, ease: "easeOut" }}
            style={{ maxWidth: 1100, margin: "var(--mantine-spacing-md) auto 0" }}>
            <Trailer id={pelicula.trailerYoutube} titulo={pelicula.titulo} radio="lg" />
          </m.div>
        )}
      </Box>
    </Banda>
  );
}
