import { useState } from "react";
import { Button, Code, Divider, Paper, PasswordInput, Stack, Text, TextInput, Title } from "@mantine/core";
import { Navigate } from "react-router";
import * as api from "../api/api-http.js";
import { ErrorCaja } from "../componentes/Estado.jsx";
import { useSesion } from "./sesion.jsx";

export function Login() {
  const { empleado, esAdministrador, abrir } = useSesion();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [entrando, setEntrando] = useState(false);

  // El acomodador entra directo a la puerta: es lo único que puede hacer.
  if (empleado) return <Navigate to={esAdministrador ? "/peliculas" : "/puerta"} replace />;

  async function entrar(evento) {
    evento.preventDefault();
    setEntrando(true);
    try {
      abrir(await api.login(email, password));
    } catch (e) {
      setError(e.message);
      setEntrando(false);
    }
  }

  return (
    <Paper withBorder p="lg" maw={380} mx="auto" mt="xl">
      <Title order={1} size="h3" mb="md">Ingresar</Title>
      <form onSubmit={entrar}>
        <Stack gap="sm">
          <TextInput label="Email" type="email" required autoComplete="username" value={email}
            onChange={(e) => setEmail(e.currentTarget.value)} />
          <PasswordInput label="Contraseña" required autoComplete="current-password" value={password}
            onChange={(e) => setPassword(e.currentTarget.value)} />
          <Button type="submit" loading={entrando}>Entrar</Button>
          {error && <ErrorCaja>{error}</ErrorCaja>}
        </Stack>
      </form>
      <Divider my="md" />
      <Text size="xs" c="dimmed">
        Datos de prueba: <Code>encargado@cine.uade.ar</Code> / <Code>cine2026</Code>
      </Text>
    </Paper>
  );
}
