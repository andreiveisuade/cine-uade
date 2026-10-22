import { useState } from "react";
import { Button, TextInput } from "@mantine/core";
import * as api from "@/api/api-http.js";
import { useAvisar } from "@/compartido/estado/Avisos.jsx";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";
import { enfocarPrimero, faltantes } from "@/compartido/formularios/faltantes.js";
import { useSesion } from "../SesionDelCliente.jsx";
import { SeccionDeCuenta } from "./SeccionDeCuenta.jsx";

export function DatosDeCuenta() {
  const { usuario, setUsuario } = useSesion();
  const avisar = useAvisar();
  const [nombre, setNombre] = useState(usuario.nombre);
  const [error, setError] = useState(null);
  const [faltan, setFaltan] = useState({});
  const [enviando, setEnviando] = useState(false);

  async function guardar(evento) {
    evento.preventDefault();
    const vacios = faltantes({ nombre: { valor: nombre, etiqueta: "tu nombre" } });
    setFaltan(vacios);
    enfocarPrimero(evento.currentTarget, vacios);
    if (Object.keys(vacios).length) return;
    setError(null);
    setEnviando(true);
    try {
      setUsuario(await api.modificarCuenta({ nombre: nombre.trim() }));
      avisar("Guardamos tu nombre");
    } catch (e) {
      setError(e.message);
    }
    setEnviando(false);
  }

  return (
    <SeccionDeCuenta titulo="Tus datos" alEnviar={guardar}>
      <TextInput label="Nombre" autoComplete="name" required value={nombre} name="nombre" error={faltan.nombre}
        onChange={(e) => setNombre(e.currentTarget.value)} />
      <TextInput label="Email" type="email" autoComplete="email" value={usuario.email ?? ""} readOnly
        description="El email no se puede cambiar" />
      <Button type="submit" loading={enviando}>Guardar</Button>
      {error && <ErrorCaja>{error}</ErrorCaja>}
    </SeccionDeCuenta>
  );
}
