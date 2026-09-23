import { useRef, useState } from "react";
import { Alert, Button, Divider, Group, Stack, Text, TextInput } from "@mantine/core";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { fechaHora } from "../api/formato.js";
import { Encabezado } from "./comun.jsx";

function EntradaValida({ reserva }) {
  return (
    <Alert color="green" variant="outline" title={<Text fz="lg" fw={800}>ADELANTE</Text>} style={{ borderWidth: 2 }}>
      <Text fw={600}>{reserva.pelicula?.titulo || ""}</Text>
      <Text size="sm">{reserva.sala?.nombre || ""} · {fechaHora(reserva.funcion?.inicio)}</Text>
      <Stack gap={0} mt="sm">
        {reserva.entradas.map((e) => {
          const pideCarnet = e.tarifa && e.tarifa !== "GENERAL";
          return (
            <div key={e.codigo}>
              <Divider />
              <Group justify="space-between" py={4}>
                <Text ff="monospace" fw={600}>{e.codigo}</Text>
                <Text size="sm" fw={pideCarnet ? 700 : 400} c={pideCarnet ? "yellow" : "dimmed"}>
                  {etiqueta(e.tarifa || "GENERAL")}{pideCarnet ? " · pedir carnet" : ""}
                </Text>
              </Group>
            </div>
          );
        })}
      </Stack>
      <Text size="xs" c="dimmed" mt="sm">
        {reserva.entradas.length} persona{reserva.entradas.length === 1 ? "" : "s"} · ingreso registrado {fechaHora(reserva.ingresadaEn)}
      </Text>
    </Alert>
  );
}

/**
 * CU-18: lo que usa el acomodador. Se escanea o se tipea el código de la reserva y se
 * marca la entrada como usada.
 *
 * El foco vuelve al campo después de cada validación porque en la puerta se encadenan
 * una atrás de otra: obligar a hacer clic entre persona y persona sería insufrible.
 */
export function Puerta() {
  const [codigo, setCodigo] = useState("");
  const [resultado, setResultado] = useState(null);
  const campo = useRef(null);

  async function validar(evento) {
    evento.preventDefault();
    const limpio = codigo.trim().toUpperCase();
    if (!limpio) return;
    try {
      setResultado({ reserva: await api.validarEntrada(limpio) });
    } catch (e) {
      // Los tres motivos —código inexistente, sin pagar y ya usada— se muestran igual de
      // fuerte: en la puerta lo único que importa es que no pasa.
      setResultado({ error: e.message });
    }
    setCodigo("");
    campo.current?.focus();
  }

  return (
    <Stack maw={560}>
      <Encabezado titulo="Validar entrada">Escaneá el código del ticket o tipealo. Cada entrada sirve una sola vez.</Encabezado>
      <form onSubmit={validar}>
        <Group align="flex-end">
          <TextInput ref={campo} required autoFocus autoComplete="off" placeholder="A1B2C3D4" size="lg" w={240}
            styles={{ input: { fontFamily: "var(--mantine-font-family-monospace)", textTransform: "uppercase", letterSpacing: "0.15em" } }}
            value={codigo} onChange={(e) => setCodigo(e.currentTarget.value)} />
          <Button type="submit" size="lg">Validar</Button>
        </Group>
      </form>
      {resultado?.reserva && <EntradaValida reserva={resultado.reserva} />}
      {resultado?.error && (
        <Alert color="red" variant="outline" title={<Text fz="lg" fw={800}>NO PASA</Text>} style={{ borderWidth: 2 }}>
          {resultado.error}
        </Alert>
      )}
    </Stack>
  );
}
