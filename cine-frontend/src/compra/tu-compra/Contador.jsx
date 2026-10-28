import { ActionIcon, Button, Group, Text } from "@mantine/core";
import { IconMinus, IconPlus } from "@tabler/icons-react";

// Cero es un botón para sumar; con algo, el menos, la cantidad y el más.
export function Contador({ nombre, cantidad, maximo, alCambiar }) {
  if (!cantidad) {
    return (
      <Button variant="light" leftSection={<IconPlus size={16} />} onClick={() => alCambiar(1)} aria-label={`Sumar ${nombre}`}>
        Sumar
      </Button>
    );
  }
  return (
    <Group gap={6} wrap="nowrap">
      <ActionIcon variant="default" size="lg" onClick={() => alCambiar(-1)} aria-label={`Sacar un ${nombre}`}>
        <IconMinus size={16} />
      </ActionIcon>
      <Text fw={800} w={28} ta="center" className="cine-cifras" aria-label={`${cantidad} ${nombre}`}>{cantidad}</Text>
      <ActionIcon size="lg" onClick={() => alCambiar(1)} disabled={cantidad >= maximo} aria-label={`Sumar otro ${nombre}`}>
        <IconPlus size={16} />
      </ActionIcon>
    </Group>
  );
}
