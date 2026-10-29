import { useRef } from "react";
import { Box, Button, Drawer, Group, Stack, Text } from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { IconChevronUp } from "@tabler/icons-react";
import { precio } from "@/compartido/formato.js";
import { MontoAnimado } from "@/compartido/ui/MontoAnimado.jsx";
import { RenglonesDelPedido } from "./TuPedido.jsx";

// En el celular el pedido queda abajo de la carta: esta barra lo tiene siempre a mano, con cuánto lleva y el total,
// y abre el detalle para cambiarlo. Se esconde cuando el pedido de verdad está a la vista.
export function BarraDelPedido({ visible, total, alIrAPagar, ...pedido }) {
  const [abierto, { open, close }] = useDisclosure(false);
  // El drawer bloquea el scroll mientras está abierto: se baja al pago recién cuando terminó de cerrarse.
  const aPagar = useRef(false);
  const candy = pedido.pedido.items.reduce((n, i) => n + i.cantidad, 0);
  const lleva = [
    !pedido.sumar && `${pedido.reserva.entradas.length} entrada${pedido.reserva.entradas.length === 1 ? "" : "s"}`,
    candy > 0 && `${candy} de candy`,
  ].filter(Boolean).join(" + ") || "Nada todavía";

  function irAPagar() {
    aPagar.current = true;
    close();
  }

  function cerrado() {
    if (aPagar.current) alIrAPagar();
    aPagar.current = false;
  }

  return (
    <>
      <Box hiddenFrom="md" h={88} aria-hidden />
      <Box hiddenFrom="md" pos="fixed" left={0} right={0} bottom={0} px="md" py="sm"
        style={{
          zIndex: 150, background: "var(--cine-superficie)", borderTop: "1px solid var(--cine-borde)",
          boxShadow: "0 -12px 28px -16px rgb(0 0 0 / 0.5)", paddingBottom: "max(var(--mantine-spacing-sm), env(safe-area-inset-bottom))",
          transform: visible ? "none" : "translateY(110%)", transition: "transform 200ms cubic-bezier(0.2, 0, 0, 1)",
        }}
        inert={!visible}>
        <Group justify="space-between" wrap="nowrap" gap="sm">
          <Stack gap={0}>
            <Text size="sm" c="dimmed">{lleva}</Text>
            <Text className="cine-display cine-cifras" fz={28} fw={850} lh={1.1} aria-hidden>
              <MontoAnimado valor={total} />
            </Text>
          </Stack>
          <Button onClick={open} rightSection={<IconChevronUp size={18} />}>Ver pedido</Button>
        </Group>
      </Box>
      <Drawer opened={abierto} onClose={close} onExitTransitionEnd={cerrado} position="bottom" title="Tu pedido" hiddenFrom="md" radius="lg"
        styles={{
          content: { height: "auto", maxHeight: "85dvh" },
          header: { background: "var(--cine-superficie)" },
          title: { fontWeight: 800, fontSize: "var(--mantine-h3-font-size)" },
        }}>
        <RenglonesDelPedido {...pedido} />
        <Group justify="space-between" align="baseline" mt="md" className="cine-cifras">
          <Text fw={650}>Total</Text>
          <Text className="cine-display" fz={32} fw={850} lh={1}>{precio(total)}</Text>
        </Group>
        <Button fullWidth size="md" mt="md" onClick={irAPagar}>Ir a pagar</Button>
      </Drawer>
    </>
  );
}
