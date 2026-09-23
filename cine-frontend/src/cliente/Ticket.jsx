import { Alert, Button, Code, Grid, Group, Paper, Stack, Text } from "@mantine/core";
import { Link, useParams } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { fechaHora, precio, precioExacto } from "../api/formato.js";
import { useAvisar } from "../componentes/Avisos.jsx";
import { EsperaOError } from "../componentes/Estado.jsx";
import { useCargar } from "../componentes/useCargar.js";

const LINEA = "=".repeat(44);

const renglon = (etiquetaTexto, valor) => ` ${etiquetaTexto.padEnd(13)}: ${valor}`;

const centrar = (texto) => " ".repeat(Math.max(Math.floor((LINEA.length - texto.length) / 2), 0)) + texto;

/** Mismo contenido y formato que tickets/ticket-<id>.txt del backend. */
function armarTicket(reserva) {
  return [
    LINEA,
    centrar("CINE UADE"),
    centrar("TICKET #" + reserva.id),
    LINEA,
    renglon("Pelicula", reserva.pelicula.titulo),
    renglon("Sala", `${reserva.sala.nombre} (${reserva.sala.tipo})`),
    renglon("Funcion", fechaHora(reserva.funcion.inicio)),
    renglon("Formato", `${reserva.funcion.proyeccion} ${reserva.funcion.idioma}`),
    renglon("Cliente", reserva.cliente.nombre),
    LINEA,
    ...reserva.entradas.map((e) => renglon(
      "Butaca " + e.codigo,
      precioExacto(e.precio) + (e.tarifa && e.tarifa !== "GENERAL" ? "  " + e.tarifa : ""))),
    LINEA,
    renglon("Entradas", String(reserva.entradas.length)),
    renglon("Total", precioExacto(reserva.total)),
    renglon("Estado", reserva.estado),
    LINEA,
    centrar("CODIGO DE ACCESO"),
    centrar(reserva.codigo || ""),
    LINEA,
    centrar("Presentar en boleteria"),
    LINEA,
  ].join("\n");
}

/**
 * El código de acceso, grande y separado en dos grupos de cuatro para poder leerlo de
 * un renglón. No es un QR dibujado: generarlo de verdad pide una librería, y el código
 * en claro cumple la misma función —el acomodador lo escanea o lo tipea— sin sumar una
 * dependencia al proyecto.
 */
function TarjetaCodigo({ reserva }) {
  if (!reserva.codigo) return null;
  const usada = reserva.ingresadaEn;
  return (
    <Paper withBorder p="lg" ta="center" style={{ borderWidth: 2, opacity: usada ? 0.6 : 1 }}>
      <Text size="xs" tt="uppercase" c="dimmed" style={{ letterSpacing: "0.15em" }}>Código de acceso</Text>
      <Text ff="monospace" fz={32} fw={700} style={{ letterSpacing: "0.2em", textDecoration: usada ? "line-through" : "none" }}>
        {reserva.codigo.slice(0, 4)} {reserva.codigo.slice(4)}
      </Text>
      <Text size="xs" c="dimmed">
        {usada ? `Ya se usó el ${fechaHora(usada)}` : "Mostralo en la puerta. Sirve una sola vez."}
      </Text>
    </Paper>
  );
}

/**
 * La carta del candy, solo para mirar. No se compra online a propósito: la venta de candy
 * nace cobrada en el mostrador, y un pago web pediría un circuito de reserva que el candy
 * no tiene. Con el número de reserva, en el mostrador la venta se asocia a esta función.
 */
function CartaCandy({ productos, reservaId }) {
  if (!productos.length) return null;
  return (
    <Paper withBorder p="md">
      <Text fw={600}>¿Pochoclos para la función?</Text>
      <Text size="sm" c="dimmed" mb="sm">
        Comprá en el mostrador del candy antes de entrar. Si decís tu número de reserva (#{reservaId}), la compra
        queda a tu nombre.
      </Text>
      <Stack gap={6}>
        {productos.map((p) => (
          <Group key={p.id} justify="space-between" align="baseline" wrap="nowrap">
            <div>
              <Text size="sm">{p.nombre}</Text>
              {p.componentes?.length > 0 && (
                <Text size="xs" c="dimmed">{p.componentes.map((c) => `${c.cantidad}× ${c.nombre}`).join(" + ")}</Text>
              )}
            </div>
            <Text size="sm" fw={500} style={{ whiteSpace: "nowrap" }}>{precio(p.precio)}</Text>
          </Group>
        ))}
      </Stack>
    </Paper>
  );
}

export function Ticket() {
  const { id } = useParams();
  const avisar = useAvisar();
  // Si la carta no carga, el ticket se muestra igual: es lo único imprescindible acá.
  const carga = useCargar(() => Promise.all([api.obtenerReserva(id), api.obtenerProductosCandy().catch(() => [])]), [id]);
  if (!carga.datos) return <EsperaOError carga={carga} />;
  const [reserva, productos] = carga.datos;
  const conAcreditacion = reserva.entradas.filter((e) => e.tarifa && e.tarifa !== "GENERAL");

  async function copiar() {
    await navigator.clipboard.writeText(armarTicket(reserva));
    avisar("Comprobante copiado");
  }

  return (
    <Stack gap="md">
      <Alert color="green">Reserva confirmada. Presentá este comprobante en boletería.</Alert>
      <Grid gap="md" align="flex-start">
        <Grid.Col span={{ base: 12, md: 7 }}>
          <Stack gap="md">
            <TarjetaCodigo reserva={reserva} />
            {conAcreditacion.length > 0 && (
              <Alert color="yellow" title="Traé el carnet">
                En la puerta se acredita la tarifa de{" "}
                {conAcreditacion.map((e) => `${e.codigo} (${etiqueta(e.tarifa).toLowerCase()})`).join(", ")}.
              </Alert>
            )}
            <Code block fz="xs">{armarTicket(reserva)}</Code>
            <Group>
              <Button component={Link} to="/">Volver a la cartelera</Button>
              <Button variant="default" onClick={copiar}>Copiar</Button>
            </Group>
          </Stack>
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 5 }}>
          <CartaCandy productos={productos} reservaId={reserva.id} />
        </Grid.Col>
      </Grid>
    </Stack>
  );
}
