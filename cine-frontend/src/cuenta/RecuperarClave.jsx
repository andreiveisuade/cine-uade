import { useState } from "react";
import { Anchor, Button, PasswordInput, Text, TextInput } from "@mantine/core";
import { Link, useLocation, useNavigate } from "react-router";
import * as api from "@/api/api-http.js";
import { useAvisar } from "@/compartido/estado/Avisos.jsx";
import { useTitulo } from "@/compartido/hooks/useTitulo.js";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";
import { enfocarPrimero, faltantes } from "@/compartido/formularios/faltantes.js";
import { FormularioCentrado } from "./FormularioCentrado.jsx";

const BAJADA = "El código de una de tus entradas prueba que la cuenta es tuya.";

export function RecuperarClave() {
  const navegar = useNavigate();
  const avisar = useAvisar();
  useTitulo("Recuperar clave");
  const { state } = useLocation();
  const [email, setEmail] = useState(state?.email || "");
  const [codigo, setCodigo] = useState("");
  const [nueva, setNueva] = useState("");
  const [error, setError] = useState(null);
  const [faltan, setFaltan] = useState({});
  const [enviando, setEnviando] = useState(false);

  async function recuperar(evento) {
    evento.preventDefault();
    const vacios = faltantes({
      email: { valor: email, etiqueta: "tu email" },
      codigo: { valor: codigo, etiqueta: "el código" },
      nueva: { valor: nueva, etiqueta: "la contraseña nueva" },
    });
    setFaltan(vacios);
    enfocarPrimero(evento.currentTarget, vacios);
    if (Object.keys(vacios).length) return;
    setError(null);
    setEnviando(true);
    try {
      await api.recuperarClave({ email: email.trim(), codigoReserva: codigo.trim(), nueva });
      avisar("Listo: entrá con tu contraseña nueva");
      navegar("/ingresar", { state: { email: email.trim() } });
    } catch (e) {
      setError(e.message);
      setEnviando(false);
    }
  }

  return (
    <FormularioCentrado titulo="Recuperar clave" alEnviar={recuperar} bajada={BAJADA}>
      <TextInput label="Email" type="email" autoComplete="email" required autoFocus value={email}
        name="email" error={faltan.email} onChange={(e) => setEmail(e.currentTarget.value)} />
      <TextInput label="Código de una de tus entradas" required value={codigo} name="codigo" error={faltan.codigo}
        description="Está en el ticket que recibiste al comprar" onChange={(e) => setCodigo(e.currentTarget.value)} />
      <PasswordInput label="Contraseña nueva" autoComplete="new-password" required value={nueva}
        name="nueva" error={faltan.nueva} onChange={(e) => setNueva(e.currentTarget.value)} />
      <Button type="submit" size="md" mt="xs" loading={enviando}>Cambiar contraseña</Button>
      {error && <ErrorCaja>{error}</ErrorCaja>}
      <Text size="sm" c="dimmed" ta="center">
        ¿Te acordaste? <Anchor component={Link} to="/ingresar" state={{ email }} size="sm">Ingresá</Anchor>
      </Text>
    </FormularioCentrado>
  );
}
