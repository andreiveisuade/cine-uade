import { useState } from "react";
import { Anchor, Button, Paper, PasswordInput, Stack, Text, TextInput, Title } from "@mantine/core";
import { Link, useLocation, useNavigate } from "react-router";
import * as api from "@/api/api-http.js";
import { useAvisar } from "@/compartido/estado/Avisos.jsx";
import { useTitulo } from "@/compartido/hooks/useTitulo.js";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";
import { faltantes } from "./compra.jsx";
import { useSesion } from "./sesion.jsx";

export function Registro() {
  const navegar = useNavigate();
  const avisar = useAvisar();
  useTitulo("Registrarme");
  const { setUsuario } = useSesion();
  const [nombre, setNombre] = useState("");
  const { state } = useLocation();
  const [email, setEmail] = useState(state?.email || "");
  const [clave, setClave] = useState("");
  const [error, setError] = useState(null);
  const [faltan, setFaltan] = useState({});
  const [enviando, setEnviando] = useState(false);

  async function registrar(evento) {
    evento.preventDefault();
    const vacios = faltantes({
      nombre: { valor: nombre, etiqueta: "tu nombre" },
      email: { valor: email, etiqueta: "tu email" },
      clave: { valor: clave, etiqueta: "una contraseña" },
    });
    setFaltan(vacios);
    if (Object.keys(vacios).length) return;
    setError(null);
    setEnviando(true);
    try {
      await api.registrarCliente({ nombre: nombre.trim(), email: email.trim(), password: clave });
      // Recién creada la cuenta, entrar con ella: pedirle la clave de nuevo no agrega nada.
      const usuario = await api.ingresar(email.trim(), clave);
      setUsuario(usuario);
      avisar(`Listo, ${usuario.nombre}`);
      navegar("/mis-reservas");
    } catch (e) {
      setError(e.message);
      setEnviando(false);
    }
  }

  return (
    <Stack gap="lg" maw={420} mx="auto" pt={{ base: "md", sm: "xl" }}>
      <div>
        <Title order={1}>Registrarme</Title>
        <Text size="sm" c="dimmed" mt={6}>
          No hace falta para reservar: es para pagar online y ver tus reservas. Si ya reservaste, usá ese mismo email
          y las vas a encontrar en tu cuenta.
        </Text>
      </div>
      <Paper withBorder p="lg">
        <form onSubmit={registrar}>
          <Stack gap="md">
            <TextInput label="Nombre" required value={nombre} error={faltan.nombre}
              onChange={(e) => setNombre(e.currentTarget.value)} />
            <TextInput label="Email" type="email" required value={email} error={faltan.email}
              onChange={(e) => setEmail(e.currentTarget.value)} />
            <PasswordInput label="Contraseña" required value={clave} error={faltan.clave}
              onChange={(e) => setClave(e.currentTarget.value)} />
            <Button type="submit" size="md" mt="xs" loading={enviando}>Registrarme</Button>
            {error && <ErrorCaja>{error}</ErrorCaja>}
            <Text size="sm" c="dimmed" ta="center">
              ¿Ya tenés cuenta? <Anchor component={Link} to="/ingresar" state={{ email }} size="sm">Ingresá</Anchor>
            </Text>
          </Stack>
        </form>
      </Paper>
    </Stack>
  );
}
