import { AspectRatio, Card, Text } from "@mantine/core";
import { etiqueta } from "@/api/etiquetas.js";
import { precio } from "@/compartido/formato.js";

export function TarjetaProducto({ producto, ratio = 1 }) {
  return (
    <Card padding="md" withBorder h="100%">
      {producto.imagenUrl && (
        <Card.Section mb="sm">
          <AspectRatio ratio={ratio}>
            <img src={producto.imagenUrl} alt={producto.nombre} loading="lazy" style={{ objectFit: "cover" }} />
          </AspectRatio>
        </Card.Section>
      )}
      <Text size="xs" c="dimmed" tt="uppercase" fw={700} style={{ letterSpacing: "0.04em" }}>{etiqueta(producto.tipo)}</Text>
      <Text fw={650} lh={1.25} mt={2}>{producto.nombre}</Text>
      <Text className="cine-display cine-cifras" fw={850} fz={26} mt={6} lh={1}>{precio(producto.precio)}</Text>
    </Card>
  );
}
