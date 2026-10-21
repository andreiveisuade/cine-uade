import { Burger, Drawer, Group, Stack } from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { Contenedor } from "@/compartido/layout/Contenedor.jsx";
import { BotonTema } from "./BotonTema.jsx";
import { Enlace } from "./Enlace.jsx";
import { EnlacesDeCuenta } from "./EnlacesDeCuenta.jsx";
import { Marca } from "./Marca.jsx";

function Secciones({ alElegir }) {
  return (
    <>
      <Enlace a="/cartelera" alElegir={alElegir}>Cartelera</Enlace>
      <Enlace a="/promociones" alElegir={alElegir}>Promos</Enlace>
      <Enlace a="/nosotros" alElegir={alElegir}>Nosotros</Enlace>
    </>
  );
}

// En celular los enlaces no entran en una fila: van a un panel lateral que se cierra al elegir.
export function Barra() {
  const [abierto, { toggle, close }] = useDisclosure(false);
  return (
    <Contenedor h="100%">
      <Group h="100%" justify="space-between" wrap="nowrap">
        <Group gap="xl" wrap="nowrap">
          <Marca />
          <Group gap="lg" wrap="nowrap" visibleFrom="sm"><Secciones /></Group>
        </Group>
        <Group gap="lg" wrap="nowrap">
          <Group gap="lg" wrap="nowrap" visibleFrom="sm"><EnlacesDeCuenta /></Group>
          <BotonTema />
          <Burger opened={abierto} onClick={toggle} hiddenFrom="sm" size="sm" aria-label="Abrir el menú" />
        </Group>
      </Group>
      <Drawer opened={abierto} onClose={close} position="right" size="xs" title={<Marca />} hiddenFrom="sm">
        <Stack gap="lg" pt="md" fz="lg">
          <Secciones alElegir={close} />
          <EnlacesDeCuenta alElegir={close} />
        </Stack>
      </Drawer>
    </Contenedor>
  );
}
