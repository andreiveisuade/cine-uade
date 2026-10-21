import { Stack } from "@mantine/core";
import { Grilla } from "./Grilla.jsx";

// El primero a ancho completo y el resto en grilla: con uno solo no queda media pantalla vacía, con muchos no se apilan.
export function Destacados({ items, clave, render, min = 340 }) {
  const [primero, ...resto] = items;
  if (!primero) return null;
  return (
    <Stack gap="lg">
      {render(primero, true)}
      {resto.length > 0 && <Grilla min={min}>{resto.map((item) => <div key={clave(item)}>{render(item, false)}</div>)}</Grilla>}
    </Stack>
  );
}
