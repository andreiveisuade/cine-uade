import { useState } from "react";
import { useParams, useSearchParams } from "react-router";
import { Alert, Button, Group, Paper, Stack, Text } from "@mantine/core";
import * as api from "@/api/api-http.js";
import { etiqueta } from "@/api/etiquetas.js";
import { precioExacto } from "@/compartido/formato.js";
import { Encabezado } from "@/compartido/layout/Encabezado.jsx";
import { useTitulo } from "@/compartido/hooks/useTitulo.js";

// Adonde lleva el link de pago sin MP_ACCESS_TOKEN. El monto llega en la URL solo para mostrarlo: lo que se cobra
// lo decide el backend, y con Mercado Pago de verdad ignora lo que se elija acá.
export function PasarelaEmulada() {
  useTitulo("Pago de prueba");
  const { id } = useParams();
  const [parametros] = useSearchParams();
  const [volviendo, setVolviendo] = useState(false);
  const monto = Number(parametros.get("monto"));
  const medio = parametros.get("medio");

  function volver(status) {
    setVolviendo(true);
    api.volverDeLaPasarelaEmulada(id, status);
  }

  return (
    <div>
      <Encabezado titulo="Pago de prueba" bajada="Pasarela emulada: reemplaza a Mercado Pago cuando no está configurado" />
      <Paper withBorder p="lg" maw="32rem">
        <Stack gap="md">
          <Alert color="yellow" variant="light">No se cobra nada: elegí cómo termina el pago para probar el flujo</Alert>
          <div>
            <Text c="dimmed" size="sm">Monto a pagar{medio && ` con ${etiqueta(medio).toLowerCase()}`}</Text>
            <Text fz="h2" fw={700}>{precioExacto(monto)}</Text>
            <Text c="dimmed" size="xs">Checkout {id}</Text>
          </div>
          <Group gap="xs">
            <Button color="green" disabled={volviendo} onClick={() => volver("approved")}>Aprobar</Button>
            <Button color="red" variant="light" disabled={volviendo} onClick={() => volver("rejected")}>
              Rechazar
            </Button>
          </Group>
        </Stack>
      </Paper>
    </div>
  );
}
