// Lo que se repite en las pantallas del panel: el título con su explicación, la barra de
// filtros, la fila de "no hay nada" y el patrón de apretar un botón, avisar y recargar.

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

/**
 * Los filtros los resuelve el backend; acá solo se juntan. `total` y `visibles` arman el
 * «mostrando 3 de 40», que es lo que avisa que hay un filtro puesto aunque no se lo mire.
 */
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

/** La aclaración gris al pie de un panel: el porqué de una regla que se ve rara. */
export function Nota({ children, ...resto }) {
  return <Text size="xs" c="dimmed" {...resto}>{children}</Text>;
}

/**
 * Corre una acción del encargado: si sale bien avisa y recarga, si el backend la rechaza
 * muestra su mensaje tal cual y la pantalla queda como estaba.
 */
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

/** Las opciones de un Select de Mantine a partir de una lista de enums. */
export const opcionesDe = (valores, texto) => valores.map((v) => ({ value: String(v), label: texto(v) }));
