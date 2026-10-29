import { ActionIcon, Button, Text, Tooltip } from "@mantine/core";
import { IconMinus, IconPlus } from "@tabler/icons-react";
import clases from "./Candy.module.css";

// En cero, «Agregar»; con algo, el menos, la cantidad y el más, en el mismo lugar y del mismo ancho: la tarjeta no
// salta. En el tope el más sigue enfocable y dice por qué no suma.
export function Contador({ nombre, cantidad, maximo, alCambiar }) {
  if (!cantidad) {
    return (
      <Button size="compact-md" variant="light" className={clases.contador} leftSection={<IconPlus size={16} stroke={2.5} />}
        onClick={() => alCambiar(1)} aria-label={`Agregar ${nombre}`}>
        Agregar
      </Button>
    );
  }
  const tope = cantidad >= maximo;
  return (
    <div className={clases.contador} data-lleno role="group" aria-label={`Cantidad de ${nombre}`}>
      <ActionIcon variant="subtle" color="dark" radius="xl" onClick={() => alCambiar(-1)}
        aria-label={cantidad === 1 ? `Quitar ${nombre}` : `Quitar una unidad de ${nombre}`}>
        <IconMinus size={16} stroke={2.5} />
      </ActionIcon>
      <Text fw={800} ta="center" className="cine-cifras" aria-label={`${cantidad} ${nombre}`}>{cantidad}</Text>
      <Tooltip label={`Hasta ${maximo} por producto`} disabled={!tope} events={{ hover: true, focus: true, touch: true }}>
        <ActionIcon variant="subtle" color="dark" radius="xl" data-disabled={tope || undefined} aria-disabled={tope}
          onClick={() => !tope && alCambiar(1)}
          aria-label={tope ? `Sumar una unidad de ${nombre}: llegaste al tope de ${maximo}` : `Sumar una unidad de ${nombre}`}>
          <IconPlus size={16} stroke={2.5} />
        </ActionIcon>
      </Tooltip>
    </div>
  );
}
