import { Badge, Button, Group, Paper, Stack, Text } from "@mantine/core";
import { Link } from "react-router";
import { cantidad, fechaHora, hora, precio } from "@/compartido/formato.js";
import { cerrada, estadoVisible, importeDeLasEntradas } from "@/compartido/estadoReserva.js";
import { FechaEnBloque } from "./FechaEnBloque.jsx";

// Una fila por reserva: la fecha como bloque a la izquierda, lo que es en el medio y lo que se puede hacer a la derecha.
export function FilaReserva({ reserva: r, cancelando, alCancelar }) {
  const estado = estadoVisible(r);
  return (
    <Paper withBorder p="md" style={{ opacity: cerrada(r) || r.ingresadaEn ? 0.65 : 1 }}>
      <Group justify="space-between" align="center" gap="md">
        <Group gap="md" wrap="nowrap" align="center">
          <FechaEnBloque iso={r.funcion.inicio} />
          <Stack gap={4}>
            <Group gap="sm">
              <Text fw={700} fz="lg" lh={1.2}>{r.pelicula.titulo}</Text>
              <Badge color={estado.color} variant={r.estado === "RESERVADA" ? "light" : "filled"}>{estado.texto}</Badge>
            </Group>
            <Text size="sm" c="dimmed" className="cine-cifras">
              {fechaHora(r.funcion.inicio)} · {r.sala.nombre} · {cantidad(r.cantidadEntradas, "entrada")} · {precio(importeDeLasEntradas(r))}
              {r.estado === "RESERVADA" && ` · pagala antes de las ${hora(r.venceEn)}`}
            </Text>
          </Stack>
        </Group>
        <Group gap="xs">
          <Button component={Link} to={`/ticket/${r.codigo}`} variant="default" size="sm" ff="monospace">{r.codigo}</Button>
          {r.cobrable && <Button component={Link} to={`/compra/${r.codigo}`} size="sm">Terminar compra</Button>}
          {r.cancelable && (
            <Button variant="subtle" color="red" size="sm" loading={cancelando} onClick={alCancelar}>Cancelar</Button>
          )}
        </Group>
      </Group>
    </Paper>
  );
}
