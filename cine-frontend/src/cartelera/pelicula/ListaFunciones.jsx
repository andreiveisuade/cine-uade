import { Stack, Text } from "@mantine/core";
import { dia, porDia } from "@/compartido/formato.js";
import { Vacio } from "@/compartido/estado/Vacio.jsx";
import { Horario } from "./Horario.jsx";

export function ListaFunciones({ funciones }) {
  if (!funciones.length) return <Vacio>No hay funciones programadas.</Vacio>;
  return (
    <Stack gap="lg">
      {porDia(funciones).map(([clave, delDia]) => (
        <Stack key={clave} gap="xs">
          <Text size="sm" fw={700} tt="uppercase" c="dimmed" style={{ letterSpacing: "0.04em" }}>{dia(delDia[0].inicio)}</Text>
          {delDia.map((f) => <Horario key={f.id} funcion={f} />)}
        </Stack>
      ))}
    </Stack>
  );
}
