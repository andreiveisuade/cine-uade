import { Box } from "@mantine/core";

// 16:9 como mínimo; al costado de un texto más alto se estira y recorta en vez de dejar un hueco abajo.
export function Foto({ src, alt, ratio = "16 / 9" }) {
  return (
    <Box pos="relative" mih="100%" style={{ aspectRatio: ratio }}>
      <img src={src} alt={alt} loading="lazy"
        style={{ position: "absolute", inset: 0, width: "100%", height: "100%", objectFit: "cover" }} />
    </Box>
  );
}
