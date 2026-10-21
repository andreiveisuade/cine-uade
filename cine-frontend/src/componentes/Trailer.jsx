import { useRef, useState } from "react";
import { AspectRatio, Box, Button, Image, UnstyledButton } from "@mantine/core";
import { useReducedMotion } from "@mantine/hooks";

// El navegador solo deja arrancar solo un video muteado; el sonido se prende después con la IFrame API por postMessage.
export function urlTrailer(id, { autoplay = true, controles = true } = {}) {
  const params = new URLSearchParams({
    autoplay: autoplay ? "1" : "0", mute: "1", playsinline: "1", rel: "0", modestbranding: "1",
    loop: "1", playlist: id, enablejsapi: "1", controls: controles ? "1" : "0",
  });
  return `https://www.youtube-nocookie.com/embed/${id}?${params}`;
}

export function Trailer({ id, titulo, radio = "md" }) {
  const quieto = useReducedMotion(undefined, { getInitialValueInEffect: false });
  const [elegido, setElegido] = useState(false);
  const [conSonido, setConSonido] = useState(false);
  const video = useRef(null);
  const corriendo = !quieto || elegido;

  function activarSonido() {
    for (const func of ["unMute", "playVideo"]) {
      video.current?.contentWindow?.postMessage(JSON.stringify({ event: "command", func, args: [] }), "*");
    }
    setConSonido(true);
  }

  return (
    <Box pos="relative"
      style={{ borderRadius: `var(--mantine-radius-${radio})`, overflow: "hidden", boxShadow: "0 24px 60px -24px rgba(0,0,0,0.7)", background: "black" }}>
      <AspectRatio ratio={16 / 9}>
        {corriendo ? (
          <iframe ref={video} src={urlTrailer(id)} title={`Trailer de ${titulo}`} style={{ border: 0 }}
            allow="autoplay; encrypted-media; picture-in-picture" allowFullScreen />
        ) : (
          <UnstyledButton pos="relative" onClick={() => setElegido(true)} aria-label={`Reproducir el trailer de ${titulo}`}>
            <Image src={`https://i.ytimg.com/vi/${id}/hqdefault.jpg`} alt="" fit="cover" h="100%" />
            <Box pos="absolute" top="50%" left="50%" fz={56} c="white"
              style={{ transform: "translate(-50%, -50%)", textShadow: "0 2px 16px black" }}>▶</Box>
          </UnstyledButton>
        )}
      </AspectRatio>
      {corriendo && !conSonido && (
        <Button pos="absolute" bottom={16} left={16} size="md" radius="xl" color="yellow" c="dark.9" onClick={activarSonido}>
          Activar sonido
        </Button>
      )}
    </Box>
  );
}
