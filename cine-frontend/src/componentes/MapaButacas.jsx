import { useEffect, useRef, useState } from "react";
import { Box, Group, Paper, Stack, Text, UnstyledButton } from "@mantine/core";
import { m } from "motion/react";

const SIMBOLO = { VIP: "*", PAREJA: "&", ACCESIBLE: "+", ESTANDAR: "" };

// El ámbar queda para lo elegido: ningún tipo de butaca lo usa.
const COLOR_TIPO = { VIP: "grape", PAREJA: "pink", ACCESIBLE: "blue", ESTANDAR: "gray" };

export function estiloTipo(tipo) {
  const color = COLOR_TIPO[tipo];
  return {
    border: tipo === "ESTANDAR" ? "1px solid var(--cine-borde)" : `1.5px solid var(--mantine-color-${color}-5)`,
    background: tipo === "ESTANDAR" ? "var(--cine-superficie-2)" : `var(--mantine-color-${color}-light)`,
    color: "var(--mantine-color-text)",
  };
}

export const ESTILO = {
  elegida: { background: "var(--mantine-color-ambar-4)", color: "#15171d", fontWeight: 700, border: "1px solid transparent",
             boxShadow: "0 0 0 3px color-mix(in srgb, var(--mantine-color-ambar-4) 30%, transparent)" },
  ocupada: { background: "var(--mantine-color-dark-3)", color: "var(--mantine-color-dark-0)", border: "1px solid transparent", cursor: "not-allowed", opacity: 0.55 },
  fueraDeServicio: {
    background: "var(--mantine-color-default-hover)", color: "var(--mantine-color-dimmed)",
    textDecoration: "line-through", border: "1px dashed var(--mantine-color-default-border)", cursor: "not-allowed",
  },
};

// Las filas entran de a una, de la pantalla hacia el fondo, como se llena una sala.
const SALA = { oculta: {}, visible: { transition: { staggerChildren: 0.03 } } };
const FILA = { oculta: { opacity: 0, y: 6 }, visible: { opacity: 1, y: 0, transition: { duration: 0.2 } } };
const POP = { type: "spring", stiffness: 500, damping: 15 };

// pintar: (asiento) => { estilo, deshabilitado, titulo }
export function MapaButacas({ sala, asientos, pintar, alElegir }) {
  const filas = [];
  for (let fila = 1; fila <= sala.filas; fila++) filas.push(asientos.filter((a) => a.fila === fila));

  // 32 px es el mínimo táctil: una sala ancha no entra en un celular, así que el scroll arranca en el centro.
  const marco = useRef(null);
  const [desborda, setDesborda] = useState(false);
  useEffect(() => {
    const el = marco.current;
    if (!el) return;
    el.scrollLeft = (el.scrollWidth - el.clientWidth) / 2;
    setDesborda(el.scrollWidth > el.clientWidth + 1);
  }, [sala.filas, asientos.length]);

  return (
    <Paper withBorder p={{ base: "md", sm: "xl" }}>
      {desborda && <Text size="xs" c="dimmed" ta="center" mb="xs">Deslizá para ver toda la sala</Text>}
      <Box ref={marco} style={{ overflowX: "auto", ...(desborda && {
        maskImage: "linear-gradient(90deg, transparent, #000 24px, #000 calc(100% - 24px), transparent)" }) }}>
      {/* La pantalla es una curva de luz: de ahí sale el ámbar del sitio. */}
      <Box mx="auto" mb="xl" miw={280} maw={520} aria-hidden>
        <Box h={14} style={{ borderTop: "3px solid var(--mantine-color-ambar-4)", borderRadius: "50% / 100% 100% 0 0",
                             boxShadow: "0 -10px 28px -8px var(--mantine-color-ambar-4)" }} />
        <Text ta="center" fz={10} fw={700} c="dimmed" style={{ letterSpacing: "0.35em" }}>PANTALLA</Text>
      </Box>
      <Stack component={m.div} variants={SALA} initial="oculta" animate="visible" gap={6} style={{ minWidth: "max-content" }} mx="auto" w="fit-content">
        {filas.map((deLaFila, i) => (
          <Group component={m.div} variants={FILA} key={i} gap={6} justify="center" wrap="nowrap">
            <Text w={16} ta="right" fz="xs" fw={600} c="dimmed">{deLaFila[0]?.codigo.charAt(0)}</Text>
            {deLaFila.map((a) => {
              const { estilo, deshabilitado, titulo } = pintar(a);
              return (
                <UnstyledButton component={m.button} key={a.codigo} title={titulo} disabled={deshabilitado}
                  whileTap={deshabilitado ? undefined : { scale: 0.8 }} transition={POP}
                  onClick={() => alElegir?.(a)}
                  style={{ height: 32, width: a.tipo === "PAREJA" ? 56 : 32, borderRadius: "6px 6px 3px 3px", fontSize: 11,
                           fontWeight: 550, fontVariantNumeric: "tabular-nums", textAlign: "center", flexShrink: 0, ...estilo }}>
                  {a.numero}{SIMBOLO[a.tipo]}
                </UnstyledButton>
              );
            })}
          </Group>
        ))}
      </Stack>
      </Box>
    </Paper>
  );
}

export function Referencia({ items }) {
  return (
    <Group gap="md" mt="md" justify="center">
      {items.map(([estilo, texto]) => (
        <Group key={texto} gap={6} wrap="nowrap">
          <Box w={12} h={12} style={{ borderRadius: 3, ...estilo }} />
          <Text fz="xs" c="dimmed">{texto}</Text>
        </Group>
      ))}
    </Group>
  );
}
