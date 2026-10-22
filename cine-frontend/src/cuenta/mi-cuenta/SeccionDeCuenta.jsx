import { Paper, Stack, Title } from "@mantine/core";

export function SeccionDeCuenta({ titulo, alEnviar, children }) {
  return (
    <Paper withBorder p="lg">
      <form onSubmit={alEnviar}>
        <Stack gap="md">
          <Title order={2} size="h4">{titulo}</Title>
          {children}
        </Stack>
      </form>
    </Paper>
  );
}
