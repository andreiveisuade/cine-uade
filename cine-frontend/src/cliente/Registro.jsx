import { useState } from "react";
import { Anchor, Button, Paper, Stack, Text, TextInput, Title } from "@mantine/core";
import { useNavigate } from "react-router";
import * as api from "../api/api-http.js";
import { useAvisar } from "../componentes/Avisos.jsx";
import { ErrorCaja } from "../componentes/Estado.jsx";
import { clienteRecordado, olvidarCliente, recordarCliente } from "./compra.jsx";

export function Registro() {
  const navegar = useNavigate();
  const avisar = useAvisar();
  const [recordado, setRecordado] = useState(clienteRecordado);
  const [nombre, setNombre] = useState("");
  const [email, setEmail] = useState("");
  const [error, setError] = useState(null);

  async function registrar(evento) {
    evento.preventDefault();
    try {
      const cliente = await api.registrarCliente({ nombre, email });
      recordarCliente(cliente);
      avisar(`Listo, ${cliente.nombre}`);
      navegar("/");
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <Stack gap="md" maw={440} mx="auto">
      <div>
        <Title order={1}>Registrarme</Title>
        <Text size="sm" c="dimmed">No hace falta para comprar: es para no tener que cargar tus datos cada vez.</Text>
      </div>
      {recordado && (
        <Paper withBorder p="sm">
          <Text size="sm">
            Este navegador ya recuerda a <strong>{recordado.nombre}</strong> ({recordado.email}).{" "}
            <Anchor component="button" size="sm" onClick={() => { olvidarCliente(); setRecordado(null); }}>Olvidar</Anchor>
          </Text>
        </Paper>
      )}
      <Paper withBorder p="md">
        <form onSubmit={registrar}>
          <Stack gap="sm">
            <TextInput label="Nombre" required value={nombre} onChange={(e) => setNombre(e.currentTarget.value)} />
            <TextInput label="Email" type="email" required value={email} onChange={(e) => setEmail(e.currentTarget.value)} />
            <Button type="submit">Registrarme</Button>
            {error && <ErrorCaja>{error}</ErrorCaja>}
          </Stack>
        </form>
      </Paper>
    </Stack>
  );
}
