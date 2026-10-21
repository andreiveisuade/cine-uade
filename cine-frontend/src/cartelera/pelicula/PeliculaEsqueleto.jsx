import { Grid, Skeleton, Stack } from "@mantine/core";

export function PeliculaEsqueleto() {
  return (
    <Stack gap="xl">
      <Skeleton h={{ base: 220, sm: 480 }} radius="lg" />
      <Grid gap="xl">
        <Grid.Col span={{ base: 4, md: 3 }}><Skeleton radius="md" style={{ aspectRatio: "2 / 3" }} /></Grid.Col>
        <Grid.Col span={{ base: 8, md: 5 }}>
          <Stack gap="sm"><Skeleton h={44} w="70%" /><Skeleton h={14} w="40%" /><Skeleton h={90} mt="xs" /></Stack>
        </Grid.Col>
      </Grid>
    </Stack>
  );
}
