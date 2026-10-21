import { Grid, Skeleton, Stack } from "@mantine/core";

export function FuncionEsqueleto() {
  return (
    <Stack gap="md" mt="xl">
      <Skeleton h={36} w="50%" />
      <Skeleton h={14} w="35%" />
      <Grid gap="md">
        <Grid.Col span={{ base: 12, lg: 9 }}><Skeleton h={320} /></Grid.Col>
        <Grid.Col span={{ base: 12, lg: 3 }}><Skeleton h={200} /></Grid.Col>
      </Grid>
    </Stack>
  );
}
