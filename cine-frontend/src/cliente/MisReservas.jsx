import { useState } from "react";
import { Button, Group, Paper, Stack, Text, TextInput, Title } from "@mantine/core";
import { useNavigate } from "react-router";

// Por código y no por email: el email no prueba ser el dueño, el código del ticket sí.
export function MisReservas() {
  const navegar = useNavigate();
  const [codigo, setCodigo] = useState("");

  function buscar(evento) {
    evento.preventDefault();
    navegar(`/ticket/${encodeURIComponent(codigo.replace(/\s/g, "").toUpperCase())}`);
  }

  return (
    <Stack gap="md" maw={520}>
      <div>
        <Title order={1}>Mis reservas</Title>
        <Text size="sm" c="dimmed">
          Ingresá el código de acceso de tu ticket para verlo o cancelar la reserva.
        </Text>
      </div>
      <Paper withBorder p="md">
        <form onSubmit={buscar}>
          <Group gap="xs" align="flex-end">
            <TextInput label="Código de acceso" required placeholder="A1B2 C3D4" value={codigo} style={{ flex: 1 }}
              styles={{ input: { fontFamily: "var(--mantine-font-family-monospace)", textTransform: "uppercase", letterSpacing: "0.1em" } }}
              onChange={(e) => setCodigo(e.currentTarget.value)} />
            <Button type="submit">Buscar</Button>
          </Group>
        </form>
      </Paper>
    </Stack>
  );
}
