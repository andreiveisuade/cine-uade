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
  const yaPague = useVueltaDelPago(pagando ? checkout.id : null, alVolver);

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

  // Un link y no window.open: el navegador abre la pestaña sin bloqueo de pop-ups, y el ticket queda abierto
  // esperando el pago. Hay navegadores (el de cmux) en los que window.open abre igual pero devuelve null.
  const irAPagar = (props) => (
    <Button component="a" href={checkout.urlPago} target="_blank" rel="noopener" onClick={() => setPagando(true)}
      {...props} />
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
              Estás pagando <strong>{precio(checkout.monto)}</strong> en la pestaña de Mercado Pago. Cuando termines,
              volvé acá: el ticket se actualiza solo.
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
