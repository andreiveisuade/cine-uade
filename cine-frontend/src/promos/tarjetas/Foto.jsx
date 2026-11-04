import { Box } from "@mantine/core";

// 16:9 como mínimo; al costado de un texto más alto se estira y recorta en vez de dejar un hueco abajo.
// Con ancho explícito: sin él, aspect-ratio saca el ancho del alto estirado y la foto tapa la otra columna.
export function Foto({ src, alt, ratio = "16 / 9" }) {
  return (
    <Box pos="relative" w="100%" mih="100%" style={{ aspectRatio: ratio }}>
      <img src={src} alt={alt} loading="lazy"
        style={{ position: "absolute", inset: 0, width: "100%", height: "100%", objectFit: "cover" }} />
    </Box>
  );
}
