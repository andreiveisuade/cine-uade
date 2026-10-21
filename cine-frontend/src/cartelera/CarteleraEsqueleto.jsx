import { Skeleton, Stack } from "@mantine/core";
import { Grilla } from "@/compartido/layout/Grilla.jsx";

export function CarteleraEsqueleto() {
  return (
    <Stack gap="lg">
      <Skeleton h={36} w={220} />
      <Grilla min={220}>
        {Array.from({ length: 8 }, (_, i) => <Skeleton key={i} radius="md" style={{ aspectRatio: "2 / 3" }} />)}
      </Grilla>
    </Stack>
  );
}
