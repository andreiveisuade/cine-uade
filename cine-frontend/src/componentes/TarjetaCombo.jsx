import { Badge, Card, Group, SimpleGrid, Stack, Text, Title } from "@mantine/core";
import { Foto } from "./Foto.jsx";
import { precio } from "@/api/formato.js";
import { MontoAnimado } from "./MontoAnimado.jsx";

// El ahorro y el precio mandan; lo que trae el combo va en una línea.
export function TarjetaCombo({ combo, destacada = false }) {
  return (
    <Card padding={0} withBorder h="100%">
      <SimpleGrid cols={{ base: 1, md: destacada && combo.imagenUrl ? 2 : 1 }} spacing={0} h="100%">
        {combo.imagenUrl && (
          <Foto src={combo.imagenUrl} alt={combo.nombre} />
        )}
        <Stack gap="sm" p={destacada ? "xl" : "lg"} justify="center">
          <Badge color="ambar" size="lg" variant="filled" tt="none" style={{ alignSelf: "flex-start" }}>
            Ahorrás <MontoAnimado valor={combo.ahorro} desde={0} />
          </Badge>
          <Title order={3} fz={destacada ? "h2" : "h3"}>{combo.nombre}</Title>
          <Group gap={4}>
            {combo.componentes.map((c) => (
              <Badge key={c.productoId} variant="default" tt="none" fw={500}>{c.cantidad} × {c.nombre}</Badge>
            ))}
          </Group>
          <Group gap="xs" align="baseline" mt="xs" className="cine-cifras">
            <Text className="cine-display" fw={850} fz={destacada ? 48 : 36} lh={1}>{precio(combo.precio)}</Text>
            <Text c="dimmed" td="line-through">{precio(combo.precioSuelto)}</Text>
          </Group>
        </Stack>
      </SimpleGrid>
    </Card>
  );
}
