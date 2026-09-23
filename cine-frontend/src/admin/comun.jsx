import { useCallback } from "react";
import { Button, Group, Stack, Table, Text, Title } from "@mantine/core";
import { useAvisar } from "../componentes/Avisos.jsx";

export function Encabezado({ titulo, children }) {
  return (
    <Stack gap={4} mb="lg">
      <Title order={1}>{titulo}</Title>
      {children && <Text size="sm" c="dimmed" maw={760}>{children}</Text>}
    </Stack>
  );
}

// `total` y `visibles` arman el «mostrando 3 de 40», que avisa que hay un filtro puesto.
export function BarraFiltros({ children, alLimpiar, visibles, total }) {
  return (
    <Group align="flex-end" gap="sm" mb="md">
      {children}
      <Button variant="default" onClick={alLimpiar}>Limpiar</Button>
      {visibles !== undefined && visibles !== total && (
        <Text size="sm" c="dimmed">mostrando {visibles} de {total}</Text>
      )}
    </Group>
  );
}

export function FilaVacia({ columnas, children }) {
  return (
    <Table.Tr>
      <Table.Td colSpan={columnas} ta="center" py="xl" c="dimmed">{children}</Table.Td>
    </Table.Tr>
  );
}

export function Nota({ children, ...resto }) {
  return <Text size="xs" c="dimmed" {...resto}>{children}</Text>;
}

export function useAccion(recargar) {
  const avisar = useAvisar();
  return useCallback(async (accion, mensaje) => {
    try {
      const resultado = await accion();
      if (mensaje) avisar(typeof mensaje === "function" ? mensaje(resultado) : mensaje);
      recargar?.();
      return resultado;
    } catch (e) {
      avisar(e.message, "error");
      return undefined;
    }
  }, [avisar, recargar]);
}

export const opcionesDe = (valores, texto) => valores.map((v) => ({ value: String(v), label: texto(v) }));
