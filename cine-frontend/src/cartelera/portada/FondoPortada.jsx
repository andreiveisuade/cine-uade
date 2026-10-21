import { useState } from "react";
import { Box } from "@mantine/core";
import { m } from "motion/react";

// Con trailer, un cuadro del video en 16:9; sin él, el póster agrandado y difuminado.
export function FondoPortada({ pelicula }) {
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
