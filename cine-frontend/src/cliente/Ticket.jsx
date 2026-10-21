import { Alert, Anchor, Box, Button, Code, Group, Modal, Skeleton, Stack, Text } from "@mantine/core";
import { IconCopy, IconFileText, IconQrcode } from "@tabler/icons-react";
import { useReducedMotion } from "motion/react";
import { QRCodeSVG } from "qrcode.react";
import { useEffect, useRef, useState } from "react";
import { Link, useLocation, useParams, useSearchParams } from "react-router";
import * as api from "@/api/api-http.js";
import { etiqueta } from "@/api/etiquetas.js";
import { fechaHora, precioExacto } from "@/api/formato.js";
import { useAvisar } from "@/componentes/Avisos.jsx";
import { EsperaOError } from "@/componentes/Estado.jsx";
import { useCargar } from "@/componentes/useCargar.js";
import { useTitulo } from "@/componentes/useTitulo.js";
import { EntradaTicket, vigente } from "./EntradaTicket.jsx";
import { PagarOnline } from "./PagarOnline.jsx";
import { useSesion } from "./sesion.jsx";
import { VentaCruzada } from "./VentaCruzada.jsx";

// El pago se concilia solo en el backend: mientras espera, se vuelve a pedir para que aparezca "Pagada" sin recargar.
const CADA_MS = 10_000;

const LINEA = "=".repeat(44);

const renglon = (etiquetaTexto, valor) => ` ${etiquetaTexto.padEnd(13)}: ${valor}`;

const centrar = (texto) => " ".repeat(Math.max(Math.floor((LINEA.length - texto.length) / 2), 0)) + texto;

/** Mismo contenido que tickets/ticket-<id>.txt del backend, con las constantes traducidas a su etiqueta. */
function armarTicket(reserva) {
  return [
    LINEA,
    centrar("CINE UADE"),
    centrar("TICKET #" + reserva.id),
    LINEA,
    renglon("Película", reserva.pelicula.titulo),
    renglon("Sala", `${reserva.sala.nombre} (${etiqueta(reserva.sala.tipo)})`),
    renglon("Función", fechaHora(reserva.funcion.inicio)),
    renglon("Formato", `${etiqueta(reserva.funcion.proyeccion)} ${etiqueta(reserva.funcion.idioma).toLowerCase()}`),
    renglon("Cliente", reserva.cliente.nombre),
    LINEA,
    ...reserva.entradas.map((e) => renglon(
      "Butaca " + e.codigo,
      precioExacto(e.precio) + (e.tarifa && e.tarifa !== "GENERAL" ? "  " + etiqueta(e.tarifa) : ""))),
    LINEA,
    renglon("Entradas", String(reserva.entradas.length)),
    renglon("Total", precioExacto(reserva.total)),
    renglon("Estado", etiqueta(reserva.estado)),
    LINEA,
    centrar("CÓDIGO DE ACCESO"),
    centrar(reserva.codigo || ""),
    LINEA,
    centrar("Presentar en boletería"),
    LINEA,
  ].join("\n");
}

// Para la puerta: el código y el QR a pantalla llena, con brillo máximo de contraste.
function EnLaPuerta({ reserva, abierto, cerrar }) {
  return (
    <Modal opened={abierto} onClose={cerrar} centered title="Mostralo en la puerta" radius="lg">
      <Stack align="center" gap="md" pb="md">
        <Box p="md" bg="white" style={{ borderRadius: "var(--mantine-radius-md)", lineHeight: 0 }}>
          <QRCodeSVG value={reserva.codigo} size={240} level="M" />
        </Box>
        <Text ff="monospace" fz={40} fw={700} style={{ letterSpacing: "0.18em" }}>
          {reserva.codigo.slice(0, 4)} {reserva.codigo.slice(4)}
        </Text>
      </Stack>
    </Modal>
  );
}

export function Ticket() {
  const { codigo } = useParams();
  const avisar = useAvisar();
  useTitulo("Tu ticket");
  const { usuario } = useSesion();
  // El resultado del pago: la vuelta de Mercado Pago redirige acá con ?aviso= si no se pudo cobrar.
  const aviso = useSearchParams()[0].get("aviso");
  const recien = useLocation().state?.reserva;
  // Recién comprada viene en la navegación; si no (recarga, Mis reservas) se pide por código,
  // que es la única llave del cliente: el id se adivina. La carta y las promos son opcionales.
  const carga = useCargar(() => Promise.all([
    recien?.codigo === codigo ? recien : api.obtenerReservaPorCodigo(codigo),
    api.obtenerProductosCandy().catch(() => []),
    api.obtenerPromocionesVigentes().catch(() => []),
  ]), [codigo]);
  const [enPuerta, setEnPuerta] = useState(false);
  const [actualizada, setActualizada] = useState(null);
  const inicial = carga.datos?.[0];
  const esperandoPago = inicial?.estado === "RESERVADA" && inicial.cobrable
    && !(actualizada?.codigo === codigo && actualizada.estado !== "RESERVADA");

  useEffect(() => {
    if (!esperandoPago) return;
    const intervalo = setInterval(() => {
      api.obtenerReservaPorCodigo(codigo).then(setActualizada, () => {});
    }, CADA_MS);
    return () => clearInterval(intervalo);
  }, [esperandoPago, codigo]);

  const reserva = actualizada?.codigo === codigo ? actualizada : inicial;
  // Lo refrescado solo existe si se estaba esperando el pago: si dice PAGADA, el cambio pasó con la pantalla abierta.
  const pagoEnVivo = actualizada?.codigo === codigo && actualizada.estado === "PAGADA";
  const quieto = useReducedMotion();
  const celebrada = useRef(null);

  useEffect(() => {
    if (!pagoEnVivo || quieto || celebrada.current === codigo) return;
    // Se baja recién cuando hace falta: casi ningún ticket la usa.
    let activo = true;
    let confetti;
    import("canvas-confetti").then(({ default: lanzar }) => {
      if (!activo) return;
      // Se marca al lanzar y no al entrar: StrictMode desmonta y vuelve a correr el efecto, y el primero se descarta.
      celebrada.current = codigo;
      confetti = lanzar;
      lanzar({ particleCount: 120, spread: 75, origin: { y: 0.3 }, disableForReducedMotion: true });
    });
    return () => {
      activo = false;
      confetti?.reset();
    };
  }, [pagoEnVivo, quieto, codigo]);

  if (!carga.datos) {
    return <EsperaOError carga={carga} esqueleto={<Stack gap="md"><Skeleton h={420} radius="lg" /><Skeleton h={48} w={320} /></Stack>} />;
  }
  const [, productos, promos] = carga.datos;

  async function copiar(texto, aviso) {
    // Sin permiso o fuera de HTTPS el navegador rechaza la escritura.
    try {
      await navigator.clipboard.writeText(texto);
      avisar(aviso);
    } catch {
      avisar("No pudimos copiar. Seleccionalo y copialo a mano.", "error");
    }
  }

  return (
    <Stack gap="var(--cine-seccion)">
      <Stack gap="lg">
        {aviso && <Alert color="yellow" title="El pago no se completó">{aviso}</Alert>}
        <EntradaTicket reserva={reserva} pagoEnVivo={pagoEnVivo} />

        <Group gap="sm" justify="space-between" align="flex-start">
          <Box id="pagar">
            {/* Sin cancelar acá: el ticket es público y se muestra en la puerta; cancelar es del dueño, en Mis reservas. */}
            {reserva.estado === "PAGADA" && !reserva.ingresadaEn && (
              <Button size="md" leftSection={<IconQrcode size={20} />} onClick={() => setEnPuerta(true)}>Mostrar en la puerta</Button>
            )}
            {usuario && reserva.cobrable && <PagarOnline codigo={reserva.codigo} />}
            {usuario === null && recien && reserva.cobrable && (
              <Text size="sm" c="dimmed" maw="52ch">
                Para pagarla online,{" "}
                <Anchor component={Link} to="/registro" state={{ email: reserva.cliente.email }} size="sm">registrate</Anchor>{" "}
                o <Anchor component={Link} to="/ingresar" state={{ email: reserva.cliente.email }} size="sm">ingresá</Anchor>{" "}
                con {reserva.cliente.email}. Si no, se paga en la boletería.
              </Text>
            )}
          </Box>
          <Group gap="xs">
            <Button variant="default" leftSection={<IconCopy size={16} />} onClick={() => copiar(reserva.codigo, "Código copiado")}>Copiar código</Button>
            <Button variant="default" leftSection={<IconFileText size={16} />} onClick={() => copiar(armarTicket(reserva), "Comprobante copiado")}>
              Copiar comprobante
            </Button>
            <Button variant="subtle" color="gray" c="var(--mantine-color-text)" component={Link} to="/">Volver a la cartelera</Button>
          </Group>
        </Group>

        <Box component="details" c="dimmed" fz="sm">
          <Box component="summary" style={{ cursor: "pointer" }}>Ver el comprobante en texto</Box>
          <Code block fz="xs" mt="sm">{armarTicket(reserva)}</Code>
        </Box>
      </Stack>

      {vigente(reserva) && <VentaCruzada reserva={reserva} productos={productos} promos={promos} />}
      {reserva.codigo && <EnLaPuerta reserva={reserva} abierto={enPuerta} cerrar={() => setEnPuerta(false)} />}
    </Stack>
  );
}
