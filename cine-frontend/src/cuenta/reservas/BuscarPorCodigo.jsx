import { useState } from "react";
import { Anchor, Button, Group, Paper, Stack, Text, TextInput } from "@mantine/core";
import { Link, useNavigate } from "react-router";
import { faltantes } from "@/compartido/formularios/faltantes.js";
import { Encabezado } from "@/compartido/layout/Encabezado.jsx";

// Sin sesión el ticket se busca por código: el email no prueba ser el dueño, el código sí.
export function BuscarPorCodigo() {
  const navegar = useNavigate();
  const [codigo, setCodigo] = useState("");
  const [faltan, setFaltan] = useState({});

  function buscar(evento) {
    evento.preventDefault();
    const vacios = faltantes({ codigo: { valor: codigo, etiqueta: "el código de tu ticket" } });
    setFaltan(vacios);
    if (vacios.codigo) return;
    navegar(`/ticket/${encodeURIComponent(codigo.replace(/\s/g, "").toUpperCase())}`);
  }

  return (
    <Stack gap={0} maw={560}>
      <Encabezado titulo="Mis reservas">
        <Text c="dimmed">
          Ingresá el código de acceso de tu ticket para verlo. Para pagarla online o cancelarla,{" "}
          <Anchor component={Link} to="/ingresar">ingresá</Anchor> con tu cuenta.
        </Text>
      </Encabezado>
      <Paper withBorder p="lg">
        <form onSubmit={buscar}>
          <Group gap="xs" align="flex-end">
            <TextInput label="Código de acceso" required placeholder="A1B2 C3D4" value={codigo} style={{ flex: 1 }}
              error={faltan.codigo}
              styles={{ input: { fontFamily: "var(--mantine-font-family-monospace)", textTransform: "uppercase", letterSpacing: "0.1em" } }}
              onChange={(e) => setCodigo(e.currentTarget.value)} />
            <Button type="submit" size="sm">Buscar</Button>
          </Group>
        </form>
      </Paper>
    </Stack>
  );
}
