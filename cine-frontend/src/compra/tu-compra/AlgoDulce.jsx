import { useState } from "react";
import { Badge, Chip, Group, Stack, Text, Title } from "@mantine/core";
import { etiqueta } from "@/api/etiquetas.js";
import { precio } from "@/compartido/formato.js";
import { Grilla } from "@/compartido/layout/Grilla.jsx";
import { Contador } from "./Contador.jsx";
import clases from "./Candy.module.css";

const PESTANIAS = { COMBO: "Combos", POCHOCLOS: "Pochoclos", BEBIDA: "Bebidas", GOLOSINA: "Golosinas" };

// El candy de la carta, combos primero: se paga con la entrada y se retira con el mismo QR (R30). Cada tarjeta lleva
// su contador junto al precio, como en la boletería de un cine.
export function AlgoDulce({ productos, candy }) {
  const [tipo, setTipo] = useState("TODO");
  if (!productos.length) return null;
  // Los combos adelante y el resto en el orden de la carta; solo los tipos que hay.
  const tipos = [...new Set(productos.map((p) => p.tipo))].sort((a, b) => (b === "COMBO") - (a === "COMBO"));
  const visibles = tipo === "TODO" ? productos : productos.filter((p) => p.tipo === tipo);
  const combos = visibles.filter((p) => p.esCombo);
  const sueltos = visibles.filter((p) => !p.esCombo);

  const contador = (p) => (
    <Contador nombre={p.nombre} cantidad={candy.cantidades[p.id] ?? 0} maximo={candy.maximo}
      alCambiar={(cuanto) => candy.cambiar(p.id, cuanto)} />
  );
  const elegido = (p) => (candy.cantidades[p.id] ? true : undefined);

  return (
    <Stack component="section" gap="md" aria-label="Candy para sumar">
      <div>
        <Title order={2} className="cine-display" fz="clamp(1.9rem, 1.4rem + 2vw, 2.8rem)">¡Agregale algo dulce!</Title>
        <Text c="dimmed" mt={4}>Lo pagás con la entrada y lo retirás en el candy con el mismo QR, sin hacer la fila.</Text>
      </div>
      {tipos.length > 1 && (
        <Chip.Group value={tipo} onChange={setTipo}>
          <Group gap="xs" role="radiogroup" aria-label="Qué mostrar">
            {["TODO", ...tipos].map((t) => (
              <Chip key={t} value={t} type="radio" variant="light">{t === "TODO" ? "Todo" : PESTANIAS[t] ?? etiqueta(t)}</Chip>
            ))}
          </Group>
        </Chip.Group>
      )}
      {combos.length > 0 && (
        <Grilla min={380} gap="var(--mantine-spacing-md)">
          {combos.map((c) => (
            <article key={c.id} className={clases.tarjeta} data-elegido={elegido(c)} aria-label={c.nombre}>
              <div className={clases.combo}>
                <div className={clases.foto}>{c.imagenUrl && <img src={c.imagenUrl} alt="" loading="lazy" />}</div>
                <Stack gap={6} p="md" justify="space-between">
                  <Stack gap={6}>
                    <Badge color="ambar" variant="filled" tt="none" style={{ alignSelf: "flex-start" }}>
                      Ahorrás {precio(c.ahorro)}
                    </Badge>
                    <Title order={3} fz="h3">{c.nombre}</Title>
                    <Text size="sm" c="dimmed">{c.componentes.map((i) => `${i.cantidad} × ${i.nombre}`).join(" + ")}</Text>
                  </Stack>
                  <Group justify="space-between" align="center" wrap="nowrap" mt="xs">
                    <Group gap={6} align="baseline" wrap="nowrap" className="cine-cifras">
                      <Text className="cine-display" fw={850} fz={30} lh={1}>{precio(c.precio)}</Text>
                      <Text size="sm" c="dimmed" td="line-through">{precio(c.precioSuelto)}</Text>
                    </Group>
                    {contador(c)}
                  </Group>
                </Stack>
              </div>
            </article>
          ))}
        </Grilla>
      )}
      {sueltos.length > 0 && (
        <Grilla min={300} gap="var(--mantine-spacing-sm)">
          {sueltos.map((p) => (
            <article key={p.id} className={clases.tarjeta} data-elegido={elegido(p)} aria-label={p.nombre}>
              <div className={clases.suelto}>
                <div className={clases.foto}>{p.imagenUrl && <img src={p.imagenUrl} alt="" loading="lazy" />}</div>
                <Stack gap={8}>
                  <Text fw={650} lh={1.25}>{p.nombre}</Text>
                  <Group justify="space-between" align="center" wrap="nowrap" gap="xs">
                    <Text className="cine-display cine-cifras" fw={850} fz={22} lh={1} style={{ whiteSpace: "nowrap" }}>{precio(p.precio)}</Text>
                    {contador(p)}
                  </Group>
                </Stack>
              </div>
            </article>
          ))}
        </Grilla>
      )}
    </Stack>
  );
}
