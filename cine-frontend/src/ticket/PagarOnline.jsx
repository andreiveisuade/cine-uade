import { useState } from "react";
import { Button, Group, NativeSelect, Paper, Stack, Text } from "@mantine/core";
import * as api from "@/api/api-http.js";
import { etiqueta } from "@/api/etiquetas.js";
import { precio } from "@/api/formato.js";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";
import { useVueltaDelPago } from "./useVueltaDelPago.js";

// Elegir el medio, abrir el checkout y recién ahí ir a la pasarela: el monto con la promo del medio lo calcula
// el backend, y se muestra antes de salir del sitio.
export function PagarOnline({ codigo, alVolver }) {
  const [medios, setMedios] = useState(null);
  const [medio, setMedio] = useState("");
  const [checkout, setCheckout] = useState(null);
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);
  const [pagando, setPagando] = useState(false);
  const avisarVuelta = useVueltaDelPago(pagando ? checkout.id : null, alVolver);

  // Si MP rechazó, el motivo viaja en el 302 del retorno, que el fetch no lee: se mira cómo quedó la reserva.
  async function yaPague() {
    setError(null);
    await avisarVuelta();
    const reserva = await api.obtenerReservaPorCodigo(codigo).catch(() => null);
    if (reserva?.cobrable) setError("Mercado Pago todavía no confirmó el pago");
  }

  async function empezar() {
    setError(null);
    // Mientras llegan los medios el botón queda cargando: un doble clic no pide dos veces.
    setEnviando(true);
    try {
      // Online solo los que pasan por la pasarela; el efectivo se paga en la boletería.
      const electronicos = (await api.obtenerMediosDePago()).filter((m) => m.requiereAutorizacion);
      setMedios(electronicos);
      setMedio(electronicos[0]?.nombre || "");
    } catch (e) {
      setError(e.message);
    }
    setEnviando(false);
  }

  async function abrir() {
    setError(null);
    setEnviando(true);
    try {
      setCheckout(await api.abrirCheckout(codigo, medio));
    } catch (e) {
      setError(e.message);
    }
    setEnviando(false);
  }

  // En la misma pestaña: otra pestaña algunos navegadores ni la muestran. MP vuelve solo al ticket (auto_return);
  // si se vuelve con Atrás y el navegador restaura la página, queda "Ya pagué".
  const irAPagar = (props) => (
    <Button onClick={() => { setPagando(true); window.location.assign(checkout.urlPago); }} {...props} />
  );

  if (!medios) {
    return (
      <Stack gap="xs">
        <Button loading={enviando} onClick={empezar}>Pagar online</Button>
        {error && <ErrorCaja>{error}</ErrorCaja>}
      </Stack>
    );
  }

  return (
    <Paper withBorder p="sm">
      <Stack gap="xs">
        {pagando ? (
          <>
            <Text size="sm">
              Estás pagando <strong>{precio(checkout.monto)}</strong> en Mercado Pago. Al terminar te trae de vuelta
              acá y el ticket se actualiza solo.
            </Text>
            <Group gap="xs">
              <Button onClick={yaPague}>Ya pagué</Button>
              {irAPagar({ variant: "default", children: "Abrir Mercado Pago" })}
              <Button variant="subtle" onClick={() => { setPagando(false); setCheckout(null); }}>Cambiar medio</Button>
            </Group>
          </>
        ) : checkout ? (
          <>
            <Text size="sm">
              Vas a pagar <strong>{precio(checkout.monto)}</strong> con {etiqueta(checkout.medio).toLowerCase()}.
            </Text>
            <Group gap="xs">
              {irAPagar({ children: "Ir a pagar" })}
              <Button variant="default" onClick={() => setCheckout(null)}>Cambiar medio</Button>
            </Group>
          </>
        ) : (
          <Group gap="xs" align="flex-end">
            <NativeSelect label="Medio de pago" value={medio} onChange={(e) => setMedio(e.currentTarget.value)}
              data={medios.map((m) => ({ value: m.nombre, label: etiqueta(m.nombre) }))} />
            <Button loading={enviando} disabled={!medio} onClick={abrir}>Continuar</Button>
          </Group>
        )}
        {error && <ErrorCaja>{error}</ErrorCaja>}
      </Stack>
    </Paper>
  );
}
