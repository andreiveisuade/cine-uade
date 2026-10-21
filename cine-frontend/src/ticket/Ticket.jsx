import { useState } from "react";
import { Alert, Stack } from "@mantine/core";
import { useLocation, useParams, useSearchParams } from "react-router";
import * as api from "@/api/api-http.js";
import { EsperaOError } from "@/compartido/estado/EsperaOError.jsx";
import { useCargar } from "@/compartido/hooks/useCargar.js";
import { useTitulo } from "@/compartido/hooks/useTitulo.js";
import { AccionesTicket } from "./AccionesTicket.jsx";
import { ComprobanteTexto } from "./ComprobanteTexto.jsx";
import { EnLaPuerta } from "./EnLaPuerta.jsx";
import { EntradaTicket } from "@/ticket/entrada/EntradaTicket.jsx";
import { vigente } from "./estadoReserva.js";
import { TicketEsqueleto } from "./TicketEsqueleto.jsx";
import { useEsperarPago } from "./useEsperarPago.js";
import { useFestejarPago } from "./useFestejarPago.js";
import { VentaCruzada } from "./VentaCruzada.jsx";

// La reserva como entrada, lo que se puede hacer con ella y lo que se puede sumar a la función.
export function Ticket() {
  const { codigo } = useParams();
  useTitulo("Tu ticket");
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
  const { reserva, pagoEnVivo } = useEsperarPago(codigo, carga.datos?.[0]);
  useFestejarPago(pagoEnVivo, codigo);

  if (!carga.datos) return <EsperaOError carga={carga} esqueleto={<TicketEsqueleto />} />;
  const [, productos, promos] = carga.datos;

  return (
    <Stack gap="var(--cine-seccion)">
      <Stack gap="lg">
        {aviso && <Alert color="yellow" title="El pago no se completó">{aviso}</Alert>}
        <EntradaTicket reserva={reserva} pagoEnVivo={pagoEnVivo} />
        <AccionesTicket reserva={reserva} recien={recien} alMostrarEnPuerta={() => setEnPuerta(true)} />
        <ComprobanteTexto reserva={reserva} />
      </Stack>

      {vigente(reserva) && <VentaCruzada reserva={reserva} productos={productos} promos={promos} />}
      {reserva.codigo && <EnLaPuerta reserva={reserva} abierto={enPuerta} alCerrar={() => setEnPuerta(false)} />}
    </Stack>
  );
}
