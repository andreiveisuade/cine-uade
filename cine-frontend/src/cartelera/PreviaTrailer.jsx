import { Box } from "@mantine/core";
import { urlTrailer } from "./urlTrailer.js";

export function PreviaTrailer({ id }) {
  return (
    <Box pos="absolute" inset={0} style={{ pointerEvents: "none", background: "black", overflow: "hidden" }}>
      {/* Un 16:9 a la altura del póster 2:3 mide 8/3 del ancho: centrado, recorta los costados. */}
      <iframe src={urlTrailer(id, { controles: false })} title="Previa del trailer" tabIndex={-1} aria-hidden
        allow="autoplay; encrypted-media" style={{ border: 0, height: "100%", width: "267%", marginLeft: "-83.5%" }} />
    </Box>
  );
}
