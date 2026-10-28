import { useRef, useState } from "react";
import { Button } from "@mantine/core";
import { IconDownload } from "@tabler/icons-react";
import { QRCodeSVG } from "qrcode.react";
import { useAvisar } from "@/compartido/estado/Avisos.jsx";
import { descargarEntradaPdf } from "./entradaPdf.js";

// La entrada en PDF, armada en el navegador con lo que la pantalla ya tiene. El QR oculto es el mismo del talón.
export function DescargarEntrada({ reserva }) {
  const qr = useRef(null);
  const [armando, setArmando] = useState(false);
  const avisar = useAvisar();

  async function descargar() {
    setArmando(true);
    try {
      await descargarEntradaPdf(reserva, qr.current);
    } catch {
      avisar("No pudimos armar el PDF. Probá de nuevo.", "error");
    } finally {
      setArmando(false);
    }
  }

  return (
    <>
      <Button variant="default" leftSection={<IconDownload size={16} />} loading={armando} onClick={descargar}>
        Descargar entrada
      </Button>
      <QRCodeSVG ref={qr} value={reserva.codigo} level="M" aria-hidden style={{ display: "none" }} />
    </>
  );
}
