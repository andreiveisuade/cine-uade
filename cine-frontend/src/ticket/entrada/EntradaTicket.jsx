import { Box, Group, SimpleGrid, Stack, Text } from "@mantine/core";
import { etiqueta } from "@/api/etiquetas.js";
import { dia, hora, precio } from "@/api/formato.js";
import { vigente } from "@/ticket/estadoReserva.js";
import { ButacaEntrada } from "./ButacaEntrada.jsx";
import { CabeceraEntrada } from "./CabeceraEntrada.jsx";
import { DatoEntrada } from "./DatoEntrada.jsx";
import { Entrada } from "./Entrada.jsx";
import { TalonEntrada } from "./TalonEntrada.jsx";

// El ticket es una entrada de verdad: cuerpo con la función y las butacas, talón con el código para la puerta.
export function EntradaTicket({ reserva, pagoEnVivo }) {
  const atenuada = !vigente(reserva);
  const sala = reserva.sala || reserva.funcion.sala;
  return (
    <Entrada talon={reserva.codigo && <TalonEntrada reserva={reserva} pagoEnVivo={pagoEnVivo} />}
      style={atenuada ? { opacity: 0.6, filter: "grayscale(1)" } : undefined}>
      <Stack gap="lg">
        <CabeceraEntrada reserva={reserva} />

        <SimpleGrid cols={{ base: 2, sm: 4 }} spacing="md">
          <DatoEntrada titulo="Día" grande>{dia(reserva.funcion.inicio)}</DatoEntrada>
          <DatoEntrada titulo="Hora" grande>{hora(reserva.funcion.inicio)}</DatoEntrada>
          <DatoEntrada titulo="Sala">{sala.nombre} · {etiqueta(sala.tipo)}</DatoEntrada>
          <DatoEntrada titulo="Formato">{etiqueta(reserva.funcion.proyeccion)} · {etiqueta(reserva.funcion.idioma)}</DatoEntrada>
        </SimpleGrid>

        <Box>
          {reserva.entradas.map((e) => <ButacaEntrada key={e.codigo} entrada={e} />)}
          <Group justify="space-between" align="baseline" pt="sm">
            <Text fw={650}>Total · {reserva.entradas.length} entrada{reserva.entradas.length === 1 ? "" : "s"}</Text>
            <Text className="cine-display cine-cifras" fz={36} fw={850} lh={1}>{precio(reserva.total)}</Text>
          </Group>
        </Box>
      </Stack>
    </Entrada>
  );
}
