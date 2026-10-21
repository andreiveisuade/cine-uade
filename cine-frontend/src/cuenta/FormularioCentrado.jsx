import { Paper, Stack, Text, Title } from "@mantine/core";

// Ingresar y registrarse se ven igual: título, para qué sirve y el formulario en una caja angosta.
export function FormularioCentrado({ titulo, bajada, alEnviar, children }) {
  return (
    <Stack gap="lg" maw={420} mx="auto" pt={{ base: "md", sm: "xl" }}>
      <div>
        <Title order={1}>{titulo}</Title>
        <Text size="sm" c="dimmed" mt={6}>{bajada}</Text>
      </div>
      <Paper withBorder p="lg">
        <form onSubmit={alEnviar}>
          <Stack gap="md">{children}</Stack>
        </form>
      </Paper>
    </Stack>
  );
}
