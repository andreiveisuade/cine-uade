import { useState } from "react";
import { Anchor, Button, PasswordInput, Text, TextInput } from "@mantine/core";
import { Link, useLocation, useNavigate } from "react-router";
import * as api from "@/api/api-http.js";
import { useAvisar } from "@/compartido/estado/Avisos.jsx";
import { useTitulo } from "@/compartido/hooks/useTitulo.js";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";
import { enfocarPrimero, faltantes } from "@/compartido/formularios/faltantes.js";
import { FormularioCentrado } from "./FormularioCentrado.jsx";
import { useSesion } from "./SesionDelCliente.jsx";

const BAJADA = "No hace falta para reservar: es para pagar online y ver tus reservas. Si ya reservaste, usá ese mismo "
  + "email y las vas a encontrar en tu cuenta.";

export function Registro() {
  const navegar = useNavigate();
  const avisar = useAvisar();
  useTitulo("Registrarme");
  const { setUsuario } = useSesion();
  const [nombre, setNombre] = useState("");
  const { state } = useLocation();
  const [email, setEmail] = useState(state?.email || "");
  const [clave, setClave] = useState("");
  const [codigo, setCodigo] = useState("");
  const [pideCodigo, setPideCodigo] = useState(false);
  const [error, setError] = useState(null);
  const [faltan, setFaltan] = useState({});
  const [enviando, setEnviando] = useState(false);

  async function registrar(evento) {
    evento.preventDefault();
    const vacios = faltantes({
      nombre: { valor: nombre, etiqueta: "tu nombre" },
      email: { valor: email, etiqueta: "tu email" },
      clave: { valor: clave, etiqueta: "una contraseña" },
      ...(pideCodigo && { codigo: { valor: codigo, etiqueta: "el código" } }),
    });
    setFaltan(vacios);
    enfocarPrimero(evento.currentTarget, vacios);
    if (Object.keys(vacios).length) return;
    setError(null);
    setEnviando(true);
    try {
      await api.registrarCliente({ nombre: nombre.trim(), email: email.trim(), password: clave,
        codigoReserva: pideCodigo ? codigo.trim() : undefined });
      // Recién creada la cuenta, entrar con ella: pedirle la clave de nuevo no agrega nada.
      const usuario = await api.ingresar(email.trim(), clave);
      setUsuario(usuario);
      avisar(`Listo, ${usuario.nombre}`);
      navegar("/mis-reservas");
    } catch (e) {
      // 403: el email ya compró sin cuenta y el backend pide el código de una entrada para probar que es suyo.
      if (e.status === 403) setPideCodigo(true);
      setError(e.message);
      setEnviando(false);
    }
  }

  return (
    <FormularioCentrado titulo="Registrarme" alEnviar={registrar} bajada={BAJADA}>
      <TextInput label="Nombre" required value={nombre} name="nombre" error={faltan.nombre}
        onChange={(e) => setNombre(e.currentTarget.value)} />
      <TextInput label="Email" type="email" required value={email} name="email" error={faltan.email}
        onChange={(e) => setEmail(e.currentTarget.value)} />
      <PasswordInput label="Contraseña" required value={clave} name="clave" error={faltan.clave}
        onChange={(e) => setClave(e.currentTarget.value)} />
      {pideCodigo && (
        <TextInput label="Código de una de tus entradas" required value={codigo} name="codigo" error={faltan.codigo}
          description="Está en el ticket que recibiste al comprar"
          onChange={(e) => setCodigo(e.currentTarget.value)} />
      )}
      <Button type="submit" size="md" mt="xs" loading={enviando}>Registrarme</Button>
      {error && <ErrorCaja>{error}</ErrorCaja>}
      <Text size="sm" c="dimmed" ta="center">
        ¿Ya tenés cuenta? <Anchor component={Link} to="/ingresar" state={{ email }} size="sm">Ingresá</Anchor>
      </Text>
    </FormularioCentrado>
  );
}
