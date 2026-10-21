import { useState } from "react";
import { Carousel } from "@mantine/carousel";
import "@mantine/carousel/styles.css";
import Autoplay from "embla-carousel-autoplay";
import { Button, Group, Stack, Text, Title } from "@mantine/core";
import { useReducedMotion } from "motion/react";
import { TarjetaCombo } from "@/componentes/TarjetaCombo.jsx";
import { TarjetaProducto } from "@/componentes/TarjetaProducto.jsx";
import { TarjetaPromo } from "@/componentes/TarjetaPromo.jsx";

// Lo que se puede sumar a la función, en el orden en que conviene: combos, sueltos con foto, promos de entradas.
function slides(productos, promos) {
  return [
    ...productos.filter((p) => p.esCombo).map((c) => ({ clave: `combo-${c.id}`, nodo: <TarjetaCombo combo={c} /> })),
    ...productos.filter((p) => !p.esCombo && p.imagenUrl).map((p) => ({ clave: `producto-${p.id}`, nodo: <TarjetaProducto producto={p} ratio={16 / 9} /> })),
    ...promos.map((p) => ({ clave: `promo-${p.nombre}`, nodo: <TarjetaPromo promo={p} conCondiciones={false} /> })),
  ];
}

// El candy no se reserva: se paga en el mostrador. Lo que cambia según el estado es qué tiene que hacer el cliente.
function Llamado({ reserva }) {
  if (reserva.estado === "PAGADA") {
    return <Text fw={650}>Pedilo en el candy diciendo tu reserva <Text span c="var(--mantine-color-ambar-text)" fw={800}>#{reserva.id}</Text></Text>;
  }
  return (
    <Group gap="sm">
      <Text fw={650}>Pagá online y sumá tu combo en el candy.</Text>
      {reserva.cobrable && (
        <Button size="compact-md" variant="light" onClick={() => document.getElementById("pagar")?.scrollIntoView({ behavior: "smooth", block: "center" })}>
          Pagar online
        </Button>
      )}
    </Group>
  );
}

export function VentaCruzada({ reserva, productos, promos }) {
  const quieto = useReducedMotion();
  // Una sola instancia por montaje: el carrusel la arranca y la frena él mismo.
  const [autoplay] = useState(() => Autoplay({ delay: 4500, stopOnInteraction: false, stopOnMouseEnter: true, stopOnFocusIn: true }));
  const items = slides(productos, promos);
  if (!items.length) return null;
  return (
    <Stack component="section" gap="md" aria-label="Para sumar a la función">
      <Group justify="space-between" align="flex-end" gap="sm">
        <Title order={2} fz="h3">¿Pochoclos para la función?</Title>
        <Llamado reserva={reserva} />
      </Group>
      <Carousel slideSize={{ base: "82%", sm: "50%", lg: "33.333%" }} slideGap="lg" withIndicators={items.length > 1}
        emblaOptions={{ loop: items.length > 2, align: "start" }} plugins={quieto ? [] : [autoplay]}
        withControls={items.length > 1} controlsOffset="xs" pb="xl"
        styles={{ indicator: { background: "var(--mantine-color-dimmed)" } }}>
        {items.map((it) => <Carousel.Slide key={it.clave} style={{ display: "flex" }}><div style={{ flex: 1 }}>{it.nodo}</div></Carousel.Slide>)}
      </Carousel>
    </Stack>
  );
}
