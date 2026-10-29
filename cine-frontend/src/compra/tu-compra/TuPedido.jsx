import { ActionIcon, Badge, Button, Divider, Group, Loader, ScrollArea, Stack, Text, Title } from "@mantine/core";
import { IconTrash } from "@tabler/icons-react";
import { AnimatePresence, m } from "motion/react";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";
import { precio } from "@/compartido/formato.js";
import { estadoDelCandy } from "@/ticket/estadoReserva.js";
import { Entrada } from "@/ticket/entrada/Entrada.jsx";
import { Contador } from "./Contador.jsx";
import clases from "./Candy.module.css";

const entradas = (n) => `${n} entrada${n === 1 ? "" : "s"}`;

// El pedido tiene la forma de la entrada que se está comprando: arriba lo que lleva, en el talón cómo se paga.
export function TuPedido({ talon, ...props }) {
  return (
    <Entrada component="section" aria-label="Tu pedido" talon={talon}>
      <Group justify="space-between" mb="sm">
        <Title order={2} fz="h3">Tu pedido</Title>
        {props.candy.actualizando && <Loader size="xs" aria-label="Guardando el pedido" />}
      </Group>
      <RenglonesDelPedido {...props} />
    </Entrada>
  );
}

// Lo que se paga ahora, en renglones: las entradas fijas y el candy con su cantidad, su subtotal y cómo sacarlo.
// El candy ya cobrado va aparte y no se toca.
export function RenglonesDelPedido({ reserva, sumar, pedido, candy }) {
  const cobradas = (reserva.candy ?? []).filter((c) => c.estado === "COBRADA");
  return (
    <Stack gap="sm">
      {!sumar && (
        <Group justify="space-between" align="flex-start" wrap="nowrap" className="cine-cifras">
          <div>
            <Text fw={650}>{entradas(reserva.entradas.length)}</Text>
            <Text size="sm" c="dimmed">{reserva.entradas.map((e) => e.codigo).join(", ")}</Text>
          </div>
          <Text fw={650}>{precio(reserva.total)}</Text>
        </Group>
      )}

      {pedido.items.length > 0 ? (
        <>
          <Group justify="space-between">
            <Text size="sm" fw={650} c="dimmed">Candy</Text>
            <Button variant="subtle" color="gray" c="dimmed" size="compact-sm" onClick={candy.vaciar}>Vaciar el candy</Button>
          </Group>
          <ScrollArea.Autosize mah={300}>
            <Stack gap={4}>
              <AnimatePresence initial={false}>
                {pedido.items.map((i) => (
                  <m.div key={i.id} className={clases.renglon} layout="position"
                    initial={{ opacity: 0, backgroundColor: "rgba(251, 196, 51, 0.22)" }}
                    animate={{ opacity: 1, backgroundColor: "rgba(251, 196, 51, 0)", transition: { duration: 0.6, ease: [0.2, 0, 0, 1] } }}
                    exit={{ opacity: 0, transition: { duration: 0.15 } }}>
                    <Text fw={600} lh={1.25}>{i.nombre}</Text>
                    <Text size="sm" c="dimmed" className="cine-cifras">{precio(i.precio)} c/u</Text>
                    <Group gap={4} wrap="nowrap">
                      <Contador nombre={i.nombre} cantidad={i.cantidad} maximo={candy.maximo}
                        alCambiar={(cuanto) => candy.cambiar(i.id, cuanto)} />
                      <ActionIcon variant="subtle" color="gray" size="lg" c="dimmed" onClick={() => candy.quitar(i.id)}
                        aria-label={`Sacar ${i.nombre} del pedido`}>
                        <IconTrash size={18} stroke={1.75} />
                      </ActionIcon>
                    </Group>
                    <Text fw={650} className="cine-cifras">{precio(i.precio * i.cantidad)}</Text>
                  </m.div>
                ))}
              </AnimatePresence>
            </Stack>
          </ScrollArea.Autosize>
          {pedido.ahorro > 0 && (
            <Text size="sm" fw={600} className={`cine-cifras ${clases.ahorro}`}>Con los combos ahorrás {precio(pedido.ahorro)}</Text>
          )}
        </>
      ) : (
        <Text size="sm" c="dimmed">
          {sumar ? "Todavía no sumaste nada." : "Sin candy por ahora: sumalo desde la carta y lo retirás con el QR de tu entrada."}
        </Text>
      )}

      {candy.error && <ErrorCaja>{candy.error}</ErrorCaja>}

      {cobradas.length > 0 && (
        <>
          <Divider />
          <Stack gap={6} role="group" aria-label="Ya en tu entrada">
            <Text size="sm" fw={650} c="dimmed">Ya en tu entrada</Text>
            {cobradas.flatMap((c) => c.items.map((i) => {
              const estado = estadoDelCandy(c);
              return (
                <Group key={`${c.id}-${i.productoId}`} justify="space-between" wrap="nowrap">
                  <Text size="sm">{i.cantidad} × {i.nombre}</Text>
                  <Badge color={estado.color} variant="light" tt="none">{estado.texto}</Badge>
                </Group>
              );
            }))}
          </Stack>
        </>
      )}
    </Stack>
  );
}
