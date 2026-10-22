import { useState } from "react";
import { Anchor, Button, PasswordInput, Text, TextInput } from "@mantine/core";
import { Link, useLocation, useNavigate } from "react-router";
import * as api from "@/api/api-http.js";
import { useAvisar } from "@/compartido/estado/Avisos.jsx";
import { useTitulo } from "@/compartido/hooks/useTitulo.js";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";
import { faltantes } from "@/compartido/formularios/faltantes.js";
import { FormularioCentrado } from "./FormularioCentrado.jsx";
import { useSesion } from "./SesionDelCliente.jsx";

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
      navegar(usuario.debeCambiarClave ? "/mi-cuenta" : "/mis-reservas");
    } catch (e) {
      setError(e.message);
      setEnviando(false);
    }
  }

  return (
    <FormularioCentrado titulo="Ingresar" alEnviar={entrar}
      bajada="Con tu cuenta ves tus reservas, las pagás online o las cancelás.">
      <TextInput label="Email" type="email" autoComplete="email" required value={email} error={faltan.email}
        onChange={(e) => setEmail(e.currentTarget.value)} />
      <PasswordInput label="Contraseña" autoComplete="current-password" required value={clave} error={faltan.clave}
        onChange={(e) => setClave(e.currentTarget.value)} />
      <Button type="submit" size="md" mt="xs" loading={enviando}>Ingresar</Button>
      {error && <ErrorCaja>{error}</ErrorCaja>}
      <Anchor component={Link} to="/recuperar-clave" state={{ email }} size="sm" ta="center">¿Olvidaste tu clave?</Anchor>
      <Text size="sm" c="dimmed" ta="center">
        ¿No tenés cuenta? <Anchor component={Link} to="/registro" state={{ email }} size="sm">Registrate</Anchor>
      </Text>
    </FormularioCentrado>
  );
}
