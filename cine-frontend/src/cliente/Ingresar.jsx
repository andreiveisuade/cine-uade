import { useState } from "react";
import { Anchor, Button, Paper, PasswordInput, Stack, Text, TextInput, Title } from "@mantine/core";
import { Link, useLocation, useNavigate } from "react-router";
import * as api from "../api/api-http.js";
import { useAvisar } from "../componentes/Avisos.jsx";
import { useTitulo } from "../componentes/useTitulo.js";
import { ErrorCaja } from "../componentes/Estado.jsx";
import { faltantes } from "./compra.jsx";
import { useSesion } from "./sesion.jsx";

export function Ingresar() {
  const navegar = useNavigate();
  const avisar = useAvisar();
  useTitulo("Ingresar");
  const { setUsuario } = useSesion();
  // El ticket manda el email con que se reservó, para entrar con ese.
  const { state } = useLocation();
  const [email, setEmail] = useState(state?.email || "");
  const [clave, setClave] = useState("");
  const [error, setError] = useState(null);
  const [faltan, setFaltan] = useState({});
  const [enviando, setEnviando] = useState(false);

  async function entrar(evento) {
    evento.preventDefault();
    const vacios = faltantes({
      email: { valor: email, etiqueta: "tu email" },
      clave: { valor: clave, etiqueta: "tu contraseña" },
    });
    setFaltan(vacios);
    if (Object.keys(vacios).length) return;
    setError(null);
    setEnviando(true);
    try {
      const usuario = await api.ingresar(email.trim(), clave);
      // Al personal el backend no le abre sesión con cookie: su lugar es la app de escritorio.
      if (usuario.rol !== "CLIENTE") {
        setError("Esa cuenta es del personal del cine: entrá desde la aplicación de escritorio");
        setEnviando(false);
        return;
      }
      setUsuario(usuario);
      avisar(`Hola, ${usuario.nombre}`);
      navegar("/mis-reservas");
    } catch (e) {
      setError(e.message);
      setEnviando(false);
    }
  }

  return (
    <Stack gap="lg" maw={420} mx="auto" pt={{ base: "md", sm: "xl" }}>
      <div>
        <Title order={1}>Ingresar</Title>
        <Text size="sm" c="dimmed" mt={6}>Con tu cuenta ves tus reservas, las pagás online o las cancelás.</Text>
      </div>
      <Paper withBorder p="lg">
        <form onSubmit={entrar}>
          <Stack gap="md">
            <TextInput label="Email" type="email" required value={email} error={faltan.email}
              onChange={(e) => setEmail(e.currentTarget.value)} />
            <PasswordInput label="Contraseña" required value={clave} error={faltan.clave}
              onChange={(e) => setClave(e.currentTarget.value)} />
            <Button type="submit" size="md" mt="xs" loading={enviando}>Ingresar</Button>
            {error && <ErrorCaja>{error}</ErrorCaja>}
            <Text size="sm" c="dimmed" ta="center">
              ¿No tenés cuenta? <Anchor component={Link} to="/registro" state={{ email }} size="sm">Registrate</Anchor>
            </Text>
          </Stack>
        </form>
      </Paper>
    </Stack>
  );
}
