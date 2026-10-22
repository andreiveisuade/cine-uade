import { Alert, Paper, Text, Title } from "@mantine/core";
import { etiqueta } from "@/api/etiquetas.js";
import { precio } from "@/compartido/formato.js";
import { DatosFuncion } from "@/compra/DatosFuncion.jsx";
import { TablaButacas } from "./TablaButacas.jsx";

// Qué se está por reservar: la función, cada butaca con su tarifa y el total.
export function DetalleCompra({ funcion, tarifas, butacas, elegidas, total }) {
  const aAcreditar = elegidas.filter((a) => tarifas.find((t) => t.nombre === butacas[a.codigo])?.requiereAcreditacion);
  return (
    <Paper withBorder p="lg">
      <Title order={2} fz="h3">{funcion.pelicula.titulo}</Title>
      <DatosFuncion funcion={funcion} c="dimmed" mb="md" />
      <TablaButacas elegidas={elegidas} butacas={butacas} total={total} />
      <Text size="xs" c="dimmed" mt="sm">
        Precio base {precio(funcion.precio)} × sala {etiqueta(funcion.sala.tipo)} × tipo de butaca × tarifa.
      </Text>
      <Text size="xs" c="dimmed" mt={4}>
        Si hay promociones vigentes, el descuento se aplica al pagar: depende del medio de pago, así que el
        total definitivo aparece recién ahí.
      </Text>
      {aAcreditar.length > 0 && (
        <Alert color="ambar" variant="light" mt="md" title="Acordate del carnet">
          En la puerta te van a pedir que acredites la tarifa de{" "}
          {aAcreditar.map((a) => `${a.codigo} (${etiqueta(butacas[a.codigo]).toLowerCase()})`).join(", ")}.
        </Alert>
      )}
    </Paper>
  );
}
